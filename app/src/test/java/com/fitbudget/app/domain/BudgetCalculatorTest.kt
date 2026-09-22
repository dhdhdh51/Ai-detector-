package com.fitbudget.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BudgetCalculatorTest {

    @Test
    fun `remaining budget is budget minus spending`() {
        val snapshot = BudgetCalculator.snapshot(budget = 100.0, spent = 42.0)
        assertEquals(58.0, snapshot.remaining, 0.001)
        assertFalse(snapshot.isOverBudget)
        assertEquals(0.0, snapshot.overBy, 0.001)
    }

    @Test
    fun `over budget is reported with the overshoot`() {
        val snapshot = BudgetCalculator.snapshot(budget = 100.0, spent = 118.0)
        assertTrue(snapshot.isOverBudget)
        assertEquals(18.0, snapshot.overBy, 0.001)
        assertEquals(-18.0, snapshot.remaining, 0.001)
        assertEquals(1.0, snapshot.usedFraction, 0.001)
    }

    @Test
    fun `used fraction is clamped and safe for a zero budget`() {
        assertEquals(0.42, BudgetCalculator.snapshot(100.0, 42.0).usedFraction, 0.001)
        assertEquals(0.0, BudgetCalculator.snapshot(0.0, 42.0).usedFraction, 0.001)
    }

    @Test
    fun `negative inputs are clamped instead of corrupting the maths`() {
        val snapshot = BudgetCalculator.snapshot(budget = -50.0, spent = -10.0)
        assertEquals(0.0, snapshot.budget, 0.001)
        assertEquals(0.0, snapshot.spent, 0.001)
    }

    @Test
    fun `projected spend includes the untouched part of the plan`() {
        val snapshot = BudgetCalculator.snapshot(budget = 100.0, spent = 60.0, pendingPlannedCost = 50.0)
        assertEquals(110.0, snapshot.projectedSpend, 0.001)
        assertTrue(snapshot.projectedOverBudget)
        assertFalse(snapshot.isOverBudget)
    }

    @Test
    fun `line cost multiplies per serving cost by quantity`() {
        assertEquals(21.0, BudgetCalculator.lineCost(7.0, 3.0), 0.001)
        assertEquals(10.5, BudgetCalculator.lineCost(7.0, 1.5), 0.001)
        assertEquals(0.0, BudgetCalculator.lineCost(7.0, -2.0), 0.001)
    }

    @Test
    fun `monthly summary counts only days with tracked spending`() {
        val summaries = listOf(
            DaySummary(epochDay = 1, spent = 90.0, budget = 100.0),
            DaySummary(epochDay = 2, spent = 120.0, budget = 100.0),
            DaySummary(epochDay = 3, spent = 0.0, budget = 100.0),
            DaySummary(epochDay = 4, spent = 100.0, budget = 100.0)
        )
        val monthly = BudgetCalculator.monthly(summaries)

        assertEquals(3, monthly.daysTracked)
        assertEquals(300.0, monthly.totalBudget, 0.001)
        assertEquals(310.0, monthly.totalSpent, 0.001)
        assertEquals(2, monthly.daysUnderBudget)
        assertEquals(1, monthly.daysOverBudget)
        assertEquals(103.33, monthly.averageDailySpend, 0.01)
        assertEquals(67, monthly.adherencePercent)
    }

    @Test
    fun `monthly summary of an empty month is all zeroes`() {
        val monthly = BudgetCalculator.monthly(emptyList())
        assertEquals(0, monthly.daysTracked)
        assertEquals(0.0, monthly.averageDailySpend, 0.001)
        assertEquals(0, monthly.adherencePercent)
    }

    @Test
    fun `a day with no spending never counts as a budget success`() {
        val emptyDay = DaySummary(epochDay = 1, spent = 0.0, budget = 100.0)
        assertFalse(emptyDay.budgetGoalMet)

        val trackedDay = DaySummary(epochDay = 2, spent = 80.0, budget = 100.0)
        assertTrue(trackedDay.budgetGoalMet)

        val overspentDay = DaySummary(epochDay = 3, spent = 180.0, budget = 100.0)
        assertFalse(overspentDay.budgetGoalMet)
        assertTrue(overspentDay.isOverBudget)
    }
}
