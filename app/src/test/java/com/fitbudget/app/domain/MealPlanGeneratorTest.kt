package com.fitbudget.app.domain

import com.fitbudget.app.data.database.entity.FoodEntity
import com.fitbudget.app.data.seed.FoodSeed
import com.fitbudget.app.domain.model.DietPreference
import com.fitbudget.app.domain.model.FoodCategory
import com.fitbudget.app.domain.model.MealType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MealPlanGeneratorTest {

    private val foods: List<FoodEntity> = FoodSeed.foods(0L)
        .mapIndexed { index, food -> food.copy(id = index + 1L) }

    private fun request(
        epochDay: Long = 20_000L,
        calories: Double = 2000.0,
        budget: Double = 100.0,
        preference: DietPreference = DietPreference.EGGETARIAN,
        excluded: Set<String> = emptySet()
    ) = MealPlanGenerator.Request(
        epochDay = epochDay,
        calorieTarget = calories,
        dailyBudget = budget,
        preference = preference,
        excludedKeys = excluded
    )

    @Test
    fun `a plan covers all four standard meals`() {
        val plan = MealPlanGenerator.generate(request(), foods)
        val mealTypes = plan.mapNotNull { it.mealType }.toSet()
        assertEquals(MealType.entries.toSet(), mealTypes)
    }

    @Test
    fun `a plan fits inside the daily budget`() {
        val plan = MealPlanGenerator.generate(request(budget = 100.0), foods)
        val total = plan.sumOf { it.totalCost }
        assertTrue("plan cost $total should not exceed ₹100", total <= 100.0)
    }

    @Test
    fun `a very small budget still produces something edible`() {
        val plan = MealPlanGenerator.generate(request(budget = 40.0), foods)
        assertTrue(plan.isNotEmpty())
        assertTrue(plan.sumOf { it.totalCost } <= 40.0)
    }

    @Test
    fun `a larger budget is allowed to buy more food`() {
        val small = MealPlanGenerator.generate(request(budget = 60.0), foods).sumOf { it.totalCost }
        val large = MealPlanGenerator.generate(request(budget = 200.0), foods).sumOf { it.totalCost }
        assertTrue("$large should be at least $small", large >= small)
    }

    @Test
    fun `vegetarian plans never contain egg or meat`() {
        val plan = MealPlanGenerator.generate(
            request(preference = DietPreference.VEGETARIAN),
            foods
        )
        val used = plan.mapNotNull { entry -> foods.firstOrNull { it.id == entry.foodId } }
        assertTrue(used.isNotEmpty())
        assertTrue(used.all { it.category == FoodCategory.VEG })
    }

    @Test
    fun `eggetarian plans never contain meat`() {
        val plan = MealPlanGenerator.generate(
            request(preference = DietPreference.EGGETARIAN),
            foods
        )
        val used = plan.mapNotNull { entry -> foods.firstOrNull { it.id == entry.foodId } }
        assertTrue(used.none { it.category == FoodCategory.NON_VEG })
    }

    @Test
    fun `excluded foods are never suggested`() {
        val plan = MealPlanGenerator.generate(
            request(excluded = setOf("boiled egg", "egg omelette", "egg white")),
            foods
        )
        assertTrue(plan.none { it.foodName.contains("Egg", ignoreCase = true) })
    }

    @Test
    fun `the same day always generates the same plan`() {
        val first = MealPlanGenerator.generate(request(epochDay = 20_100L), foods)
        val second = MealPlanGenerator.generate(request(epochDay = 20_100L), foods)
        assertEquals(
            first.map { it.foodName to it.quantity },
            second.map { it.foodName to it.quantity }
        )
    }

    @Test
    fun `different days vary the menu`() {
        val monday = MealPlanGenerator.generate(request(epochDay = 20_100L), foods)
            .map { it.foodName }
        val tuesday = MealPlanGenerator.generate(request(epochDay = 20_101L), foods)
            .map { it.foodName }
        assertFalse("plans for consecutive days should not be identical", monday == tuesday)
    }

    @Test
    fun `an empty food database yields an empty plan instead of crashing`() {
        assertTrue(MealPlanGenerator.generate(request(), emptyList()).isEmpty())
    }

    @Test
    fun `excluding everything yields an empty plan`() {
        val allKeys = foods.map { it.nameKey }.toSet()
        assertTrue(MealPlanGenerator.generate(request(excluded = allKeys), foods).isEmpty())
    }

    @Test
    fun `entries carry real nutrition and cost values`() {
        val plan = MealPlanGenerator.generate(request(), foods)
        assertTrue(plan.all { it.caloriesPerServing > 0.0 })
        assertTrue(plan.all { it.quantity > 0.0 })
        assertTrue(plan.all { it.costPerServing >= 0.0 })
        assertTrue(plan.all { it.servingLabel.isNotBlank() })
        assertTrue(plan.none { it.completed })
    }

    @Test
    fun `meal times default to the standard schedule`() {
        val plan = MealPlanGenerator.generate(request(), foods)
        val breakfast = plan.first { it.mealType == MealType.BREAKFAST }
        assertEquals(8 * 60, breakfast.timeMinutes)
        val dinner = plan.first { it.mealType == MealType.DINNER }
        assertEquals(20 * 60, dinner.timeMinutes)
    }

    @Test
    fun `plans land in a sensible calorie band for the target`() {
        val plan = MealPlanGenerator.generate(request(calories = 2000.0, budget = 150.0), foods)
        val total = plan.sumOf { it.totalCalories }
        assertTrue("total calories $total should be a realistic day", total in 900.0..2600.0)
    }

    @Test
    fun `every meal has at least one item when the budget allows`() {
        val plan = MealPlanGenerator.generate(request(budget = 120.0), foods)
        MealType.entries.forEach { type ->
            assertTrue(
                "meal $type should have at least one item",
                plan.any { it.mealType == type }
            )
        }
    }
}
