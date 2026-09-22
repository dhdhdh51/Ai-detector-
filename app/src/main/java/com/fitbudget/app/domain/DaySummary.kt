package com.fitbudget.app.domain

import com.fitbudget.app.domain.model.MealType

/**
 * Everything the app knows about one calendar day, assembled from real logged data only.
 * Checklists, streaks and reports are all derived from this - nothing is invented.
 */
data class DaySummary(
    val epochDay: Long,
    val plannedMealTypes: Set<MealType> = emptySet(),
    val completedMealTypes: Set<MealType> = emptySet(),
    val mealItemsPlanned: Int = 0,
    val mealItemsCompleted: Int = 0,
    val caloriesPlanned: Double = 0.0,
    val caloriesConsumed: Double = 0.0,
    val proteinConsumed: Double = 0.0,
    val plannedCost: Double = 0.0,
    val spent: Double = 0.0,
    val budget: Double = 0.0,
    val waterMl: Int = 0,
    val waterTargetMl: Int = 0,
    val steps: Int = 0,
    val stepGoal: Int = 0,
    val workoutsCompleted: Int = 0,
    val weightKg: Double? = null
) {
    val weightLogged: Boolean get() = weightKg != null

    /** Diet is "done" when every meal planned for the day is fully ticked off. */
    val dietGoalMet: Boolean
        get() = plannedMealTypes.isNotEmpty() && completedMealTypes.containsAll(plannedMealTypes)

    val waterGoalMet: Boolean get() = waterTargetMl > 0 && waterMl >= waterTargetMl

    val stepGoalMet: Boolean get() = stepGoal > 0 && steps >= stepGoal

    val workoutGoalMet: Boolean get() = workoutsCompleted > 0

    /**
     * A budget day only counts when the user actually tracked spending, otherwise an empty
     * day would silently extend the budget streak.
     */
    val budgetGoalMet: Boolean get() = budget > 0.0 && spent > 0.0 && spent <= budget

    val remainingBudget: Double get() = budget - spent

    val isOverBudget: Boolean get() = budget > 0.0 && spent > budget

    val hasAnyActivity: Boolean
        get() = mealItemsCompleted > 0 || waterMl > 0 || steps > 0 ||
            workoutsCompleted > 0 || weightLogged || spent > 0.0
}

/** One row of the daily checklist. */
data class ChecklistItem(
    val key: String,
    val label: String,
    val done: Boolean,
    val detail: String? = null,
    val route: String? = null
)

object DailyChecklist {

    const val KEY_BREAKFAST = "breakfast"
    const val KEY_LUNCH = "lunch"
    const val KEY_SNACK = "snack"
    const val KEY_DINNER = "dinner"
    const val KEY_WATER = "water"
    const val KEY_WORKOUT = "workout"
    const val KEY_STEPS = "steps"
    const val KEY_WEIGHT = "weight"

    fun build(summary: DaySummary): List<ChecklistItem> {
        fun mealItem(key: String, type: MealType): ChecklistItem {
            val planned = type in summary.plannedMealTypes
            return ChecklistItem(
                key = key,
                label = type.label,
                done = type in summary.completedMealTypes,
                detail = if (planned) null else "Nothing planned",
                route = "diet"
            )
        }

        return listOf(
            mealItem(KEY_BREAKFAST, MealType.BREAKFAST),
            mealItem(KEY_LUNCH, MealType.LUNCH),
            mealItem(KEY_SNACK, MealType.SNACK),
            mealItem(KEY_DINNER, MealType.DINNER),
            ChecklistItem(
                key = KEY_WATER,
                label = "Water goal",
                done = summary.waterGoalMet,
                detail = "${summary.waterMl} / ${summary.waterTargetMl} ml",
                route = "water"
            ),
            ChecklistItem(
                key = KEY_WORKOUT,
                label = "Workout",
                done = summary.workoutGoalMet,
                detail = if (summary.workoutsCompleted > 0) "${summary.workoutsCompleted} finished" else null,
                route = "workout"
            ),
            ChecklistItem(
                key = KEY_STEPS,
                label = "Steps",
                done = summary.stepGoalMet,
                detail = "${summary.steps} / ${summary.stepGoal}",
                route = "steps"
            ),
            ChecklistItem(
                key = KEY_WEIGHT,
                label = "Weight logged",
                done = summary.weightLogged,
                detail = summary.weightKg?.let { "%.1f kg".format(it) },
                route = "weight"
            )
        )
    }

    /** Completion as a 0..1 fraction of the eight checklist rows. */
    fun completionFraction(items: List<ChecklistItem>): Double {
        if (items.isEmpty()) return 0.0
        return items.count { it.done }.toDouble() / items.size
    }

    fun completionPercent(summary: DaySummary): Int =
        (completionFraction(build(summary)) * 100).toInt()
}
