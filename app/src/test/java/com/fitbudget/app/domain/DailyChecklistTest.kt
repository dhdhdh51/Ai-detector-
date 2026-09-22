package com.fitbudget.app.domain

import com.fitbudget.app.domain.model.MealType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyChecklistTest {

    private fun fullDay() = DaySummary(
        epochDay = 100,
        plannedMealTypes = MealType.entries.toSet(),
        completedMealTypes = MealType.entries.toSet(),
        mealItemsPlanned = 12,
        mealItemsCompleted = 12,
        spent = 95.0,
        budget = 100.0,
        waterMl = 2500,
        waterTargetMl = 2500,
        steps = 9000,
        stepGoal = 8000,
        workoutsCompleted = 1,
        weightKg = 84.2
    )

    @Test
    fun `checklist always has the eight specified rows`() {
        val items = DailyChecklist.build(DaySummary(epochDay = 1))
        assertEquals(8, items.size)
        assertEquals(
            listOf(
                DailyChecklist.KEY_BREAKFAST,
                DailyChecklist.KEY_LUNCH,
                DailyChecklist.KEY_SNACK,
                DailyChecklist.KEY_DINNER,
                DailyChecklist.KEY_WATER,
                DailyChecklist.KEY_WORKOUT,
                DailyChecklist.KEY_STEPS,
                DailyChecklist.KEY_WEIGHT
            ),
            items.map { it.key }
        )
    }

    @Test
    fun `an empty day is zero percent complete`() {
        val summary = DaySummary(epochDay = 1, waterTargetMl = 2500, stepGoal = 8000)
        assertEquals(0, DailyChecklist.completionPercent(summary))
        assertTrue(DailyChecklist.build(summary).none { it.done })
    }

    @Test
    fun `a fully completed day is one hundred percent`() {
        assertEquals(100, DailyChecklist.completionPercent(fullDay()))
        assertTrue(DailyChecklist.build(fullDay()).all { it.done })
    }

    @Test
    fun `six of eight rows completed reports seventy five percent`() {
        // meals (4) + water + workout done; steps and weight missing -> 6/8 = 75%
        val summary = fullDay().copy(
            steps = 2000,
            weightKg = null
        )
        assertEquals(75, DailyChecklist.completionPercent(summary))
    }

    @Test
    fun `a meal counts as done only when every item is ticked`() {
        val summary = DaySummary(
            epochDay = 1,
            plannedMealTypes = setOf(MealType.BREAKFAST, MealType.LUNCH),
            completedMealTypes = setOf(MealType.BREAKFAST),
            mealItemsPlanned = 6,
            mealItemsCompleted = 3
        )
        val items = DailyChecklist.build(summary)
        assertTrue(items.first { it.key == DailyChecklist.KEY_BREAKFAST }.done)
        assertFalse(items.first { it.key == DailyChecklist.KEY_LUNCH }.done)
        assertFalse(summary.dietGoalMet)
    }

    @Test
    fun `diet goal needs every planned meal`() {
        val planned = setOf(MealType.BREAKFAST, MealType.LUNCH, MealType.DINNER)
        assertTrue(
            DaySummary(
                epochDay = 1,
                plannedMealTypes = planned,
                completedMealTypes = planned
            ).dietGoalMet
        )
        assertFalse(
            DaySummary(
                epochDay = 1,
                plannedMealTypes = planned,
                completedMealTypes = setOf(MealType.BREAKFAST)
            ).dietGoalMet
        )
    }

    @Test
    fun `a day with no plan is not a diet success`() {
        assertFalse(DaySummary(epochDay = 1).dietGoalMet)
    }

    @Test
    fun `rows without a planned meal say so`() {
        val summary = DaySummary(epochDay = 1, plannedMealTypes = setOf(MealType.BREAKFAST))
        val dinner = DailyChecklist.build(summary).first { it.key == DailyChecklist.KEY_DINNER }
        assertEquals("Nothing planned", dinner.detail)
    }

    @Test
    fun `goal flags require a configured goal`() {
        val noGoals = DaySummary(epochDay = 1, waterMl = 500, steps = 100)
        assertFalse(noGoals.waterGoalMet)
        assertFalse(noGoals.stepGoalMet)
    }

    @Test
    fun `hasAnyActivity detects a completely untouched day`() {
        assertFalse(DaySummary(epochDay = 1, waterTargetMl = 2500, stepGoal = 8000).hasAnyActivity)
        assertTrue(DaySummary(epochDay = 1, waterMl = 250).hasAnyActivity)
        assertTrue(fullDay().hasAnyActivity)
    }

    @Test
    fun `completion fraction of an empty list is zero`() {
        assertEquals(0.0, DailyChecklist.completionFraction(emptyList()), 0.001)
    }
}
