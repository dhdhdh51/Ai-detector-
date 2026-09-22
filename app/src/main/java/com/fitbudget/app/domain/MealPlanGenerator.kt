package com.fitbudget.app.domain

import com.fitbudget.app.data.database.entity.FoodEntity
import com.fitbudget.app.data.database.entity.MealEntryEntity
import com.fitbudget.app.domain.model.DietPreference
import com.fitbudget.app.domain.model.FoodRole
import com.fitbudget.app.domain.model.MealType

/**
 * Builds one day's meal plan from the offline food database.
 *
 * Priorities, in order:
 *  1. respect the user's diet preference and food exclusions,
 *  2. stay inside the daily rupee budget,
 *  3. get as close as possible to the estimated calorie target,
 *  4. prefer the cheapest protein per rupee.
 *
 * The generator is a pure function and deterministic for a given day, so re-opening the app
 * never reshuffles today's plan, while different days still get some variety.
 */
object MealPlanGenerator {

    data class Request(
        val epochDay: Long,
        val calorieTarget: Double,
        val dailyBudget: Double,
        val preference: DietPreference,
        val excludedKeys: Set<String> = emptySet(),
        val mealTimes: Map<MealType, Int> = emptyMap()
    )

    /** Share of the daily budget each meal may use. */
    private val budgetShare = mapOf(
        MealType.BREAKFAST to 0.26,
        MealType.LUNCH to 0.32,
        MealType.SNACK to 0.12,
        MealType.DINNER to 0.30
    )

    /** Roles each meal is built from, in plate order. */
    private val mealComposition: Map<MealType, List<FoodRole>> = mapOf(
        MealType.BREAKFAST to listOf(FoodRole.STAPLE, FoodRole.PROTEIN, FoodRole.DAIRY),
        MealType.LUNCH to listOf(FoodRole.STAPLE, FoodRole.PROTEIN, FoodRole.VEGETABLE),
        MealType.SNACK to listOf(FoodRole.SNACK, FoodRole.FRUIT),
        MealType.DINNER to listOf(FoodRole.STAPLE, FoodRole.PROTEIN, FoodRole.VEGETABLE)
    )

    /** Items in these roles can be dropped first when the day does not fit the budget. */
    private val optionalRoles = setOf(FoodRole.FRUIT, FoodRole.DAIRY, FoodRole.BEVERAGE, FoodRole.SNACK)

    private const val MAX_STAPLE_SERVINGS = 5.0

    fun generate(request: Request, foods: List<FoodEntity>): List<MealEntryEntity> {
        val available = foods.filter { food ->
            food.category.allowedFor(request.preference) &&
                food.nameKey !in request.excludedKeys
        }
        if (available.isEmpty()) return emptyList()

        val byRole = available.groupBy { it.role }
        val draft = mutableListOf<DraftItem>()

        for (mealType in MealType.entries) {
            val mealCalories = request.calorieTarget * mealType.calorieShare
            val mealBudget = request.dailyBudget * (budgetShare[mealType] ?: 0.25)
            draft += buildMeal(
                mealType = mealType,
                roles = mealComposition[mealType] ?: listOf(FoodRole.STAPLE, FoodRole.PROTEIN),
                byRole = byRole,
                fallback = available,
                calorieTarget = mealCalories,
                budget = mealBudget,
                seed = request.epochDay + mealType.ordinal * 7L
            )
        }

        trimToBudget(draft, request.dailyBudget)

        return draft.mapIndexed { index, item ->
            val time = request.mealTimes[item.mealType]
                ?: (item.mealType.defaultHour * 60 + item.mealType.defaultMinute)
            MealEntryEntity(
                epochDay = request.epochDay,
                mealType = item.mealType,
                mealLabel = item.mealType.label,
                timeMinutes = time,
                foodId = item.food.id,
                foodName = item.food.name,
                servingLabel = item.food.servingLabel,
                quantity = item.quantity,
                caloriesPerServing = item.food.calories,
                proteinPerServing = item.food.proteinG,
                costPerServing = item.food.costRupees,
                completed = false,
                sortOrder = index
            )
        }
    }

    private data class DraftItem(
        val mealType: MealType,
        val food: FoodEntity,
        var quantity: Double
    ) {
        val calories: Double get() = food.calories * quantity
        val cost: Double get() = food.costRupees * quantity
    }

    private fun buildMeal(
        mealType: MealType,
        roles: List<FoodRole>,
        byRole: Map<FoodRole, List<FoodEntity>>,
        fallback: List<FoodEntity>,
        calorieTarget: Double,
        budget: Double,
        seed: Long
    ): List<DraftItem> {
        val items = mutableListOf<DraftItem>()
        val used = mutableSetOf<Long>()

        roles.forEachIndexed { slot, role ->
            val candidates = (byRole[role] ?: emptyList())
                .filter { it.id !in used }
                .ifEmpty { fallback.filter { it.id !in used } }
            if (candidates.isEmpty()) return@forEachIndexed

            val ordered = when (role) {
                FoodRole.PROTEIN, FoodRole.DAIRY -> candidates.sortedByDescending { it.proteinPerRupee }
                else -> candidates.sortedBy { it.costRupees }
            }
            // Rotate through the cheapest/highest-value half so days differ but stay affordable.
            val pool = ordered.take(maxOf(3, ordered.size / 2))
            val picked = pool[((seed + slot * 13L).mod(pool.size.toLong())).toInt()]
            used += picked.id

            val startQuantity = if (role == FoodRole.STAPLE && mealType != MealType.BREAKFAST) 2.0 else 1.0
            items += DraftItem(mealType, picked, startQuantity)
        }

        if (items.isEmpty()) return items

        balance(items, calorieTarget, budget)
        return items
    }

    /** Nudges the staple (or largest) item so the meal lands near its calorie and cost slot. */
    private fun balance(items: MutableList<DraftItem>, calorieTarget: Double, budget: Double) {
        val flexible = items.firstOrNull { it.food.role == FoodRole.STAPLE } ?: items.maxByOrNull { it.calories }
        if (flexible == null || flexible.food.calories <= 0.0) return

        var guard = 0
        while (guard++ < 20) {
            val calories = items.sumOf { it.calories }
            val cost = items.sumOf { it.cost }
            when {
                calories < calorieTarget * 0.9 &&
                    flexible.quantity + 0.5 <= MAX_STAPLE_SERVINGS &&
                    cost + flexible.food.costRupees * 0.5 <= budget * 1.1 ->
                    flexible.quantity += 0.5

                calories > calorieTarget * 1.15 && flexible.quantity - 0.5 >= 0.5 ->
                    flexible.quantity -= 0.5

                cost > budget * 1.15 && flexible.quantity - 0.5 >= 0.5 ->
                    flexible.quantity -= 0.5

                else -> return
            }
        }
    }

    /**
     * Last pass over the whole day: drop optional extras, then shave staple servings, until the
     * plan fits the daily budget. Never removes the last item of a meal.
     */
    private fun trimToBudget(draft: MutableList<DraftItem>, dailyBudget: Double) {
        if (dailyBudget <= 0.0) return

        fun total() = draft.sumOf { it.cost }

        var guard = 0
        while (total() > dailyBudget && guard++ < 40) {
            val removable = draft
                .filter { it.food.role in optionalRoles }
                .filter { candidate -> draft.count { it.mealType == candidate.mealType } > 1 }
                .maxByOrNull { it.cost }
            if (removable != null) {
                draft.remove(removable)
                continue
            }
            val shrinkable = draft
                .filter { it.quantity > 0.5 }
                .maxByOrNull { it.food.costRupees }
                ?: break
            shrinkable.quantity -= 0.5
        }
    }
}
