package com.fitbudget.app.domain

import com.fitbudget.app.domain.model.MealType
import org.junit.Assert.assertEquals
import org.junit.Test

class StreakCalculatorTest {

    private val today = 20_000L

    private fun day(
        epochDay: Long,
        diet: Boolean = false,
        workout: Boolean = false,
        water: Boolean = false,
        budget: Boolean = false
    ) = DaySummary(
        epochDay = epochDay,
        plannedMealTypes = if (diet) MealType.entries.toSet() else emptySet(),
        completedMealTypes = if (diet) MealType.entries.toSet() else emptySet(),
        waterMl = if (water) 2500 else 0,
        waterTargetMl = 2500,
        workoutsCompleted = if (workout) 1 else 0,
        spent = if (budget) 80.0 else 0.0,
        budget = 100.0
    )

    @Test
    fun `an empty history has no streaks`() {
        val streaks = StreakCalculator.compute(emptyList(), today)
        assertEquals(0, streaks.diet)
        assertEquals(0, streaks.workout)
        assertEquals(0, streaks.water)
        assertEquals(0, streaks.budget)
    }

    @Test
    fun `consecutive completed days build the streak`() {
        val summaries = (0..6).map { offset -> day(today - offset, diet = true) }
        assertEquals(7, StreakCalculator.compute(summaries, today).diet)
    }

    @Test
    fun `a gap breaks the streak`() {
        val summaries = listOf(
            day(today, diet = true),
            day(today - 1, diet = true),
            day(today - 2, diet = false),
            day(today - 3, diet = true)
        )
        assertEquals(2, StreakCalculator.compute(summaries, today).diet)
    }

    @Test
    fun `today still in progress keeps yesterday's streak`() {
        val summaries = listOf(
            day(today, diet = false),
            day(today - 1, diet = true),
            day(today - 2, diet = true)
        )
        assertEquals(2, StreakCalculator.compute(summaries, today).diet)
    }

    @Test
    fun `an unfinished today does not inflate the streak`() {
        val summaries = listOf(
            day(today, diet = false),
            day(today - 1, diet = false),
            day(today - 2, diet = true)
        )
        assertEquals(0, StreakCalculator.compute(summaries, today).diet)
    }

    @Test
    fun `a missing day record ends the streak`() {
        val summaries = listOf(
            day(today, diet = true),
            // today - 1 is absent entirely
            day(today - 2, diet = true)
        )
        assertEquals(1, StreakCalculator.compute(summaries, today).diet)
    }

    @Test
    fun `each tracker has its own independent streak`() {
        val summaries = listOf(
            day(today, diet = true, water = true, workout = false, budget = true),
            day(today - 1, diet = true, water = false, workout = true, budget = true),
            day(today - 2, diet = false, water = true, workout = true, budget = true)
        )
        val streaks = StreakCalculator.compute(summaries, today)
        // diet: today + yesterday, broken two days ago
        assertEquals(2, streaks.diet)
        // water: only today (yesterday missed the target)
        assertEquals(1, streaks.water)
        // workout: not done today yet, so the run ending yesterday is still alive
        assertEquals(2, streaks.workout)
        assertEquals(3, streaks.budget)
    }

    @Test
    fun `a day without tracked spending cannot extend the budget streak`() {
        val summaries = listOf(
            day(today, budget = true),
            DaySummary(epochDay = today - 1, spent = 0.0, budget = 100.0),
            day(today - 2, budget = true)
        )
        assertEquals(1, StreakCalculator.compute(summaries, today).budget)
    }

    @Test
    fun `overspending breaks the budget streak`() {
        val summaries = listOf(
            day(today, budget = true),
            DaySummary(epochDay = today - 1, spent = 150.0, budget = 100.0),
            day(today - 2, budget = true)
        )
        assertEquals(1, StreakCalculator.compute(summaries, today).budget)
    }

    @Test
    fun `longest streak finds the best run in the whole history`() {
        val summaries = listOf(
            day(today - 9, diet = true),
            day(today - 8, diet = true),
            day(today - 7, diet = true),
            day(today - 6, diet = true),
            day(today - 5, diet = false),
            day(today - 4, diet = true),
            day(today - 3, diet = true)
        )
        assertEquals(4, StreakCalculator.longestStreak(summaries) { it.dietGoalMet })
    }

    @Test
    fun `longest streak is zero when nothing qualifies`() {
        val summaries = (0..4).map { day(today - it) }
        assertEquals(0, StreakCalculator.longestStreak(summaries) { it.dietGoalMet })
    }

    @Test
    fun `overall streak needs at least three quarters of the checklist`() {
        val strong = DaySummary(
            epochDay = today,
            plannedMealTypes = MealType.entries.toSet(),
            completedMealTypes = MealType.entries.toSet(),
            waterMl = 2500,
            waterTargetMl = 2500,
            steps = 9000,
            stepGoal = 8000,
            workoutsCompleted = 0,
            weightKg = null
        )
        assertEquals(1, StreakCalculator.compute(listOf(strong), today).overall)
    }
}
