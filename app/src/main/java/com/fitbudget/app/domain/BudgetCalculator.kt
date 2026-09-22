package com.fitbudget.app.domain

import kotlin.math.roundToInt

data class BudgetSnapshot(
    val budget: Double,
    val spent: Double,
    /** Cost of items that are planned for today but not ticked off yet. */
    val pendingPlannedCost: Double = 0.0
) {
    val remaining: Double get() = budget - spent
    val isOverBudget: Boolean get() = spent > budget
    val overBy: Double get() = (spent - budget).coerceAtLeast(0.0)
    val usedFraction: Double
        get() = if (budget <= 0.0) 0.0 else (spent / budget).coerceIn(0.0, 1.0)
    val projectedSpend: Double get() = spent + pendingPlannedCost
    val projectedOverBudget: Boolean get() = budget > 0.0 && projectedSpend > budget
}

data class MonthlyBudgetSummary(
    val daysTracked: Int,
    val totalBudget: Double,
    val totalSpent: Double,
    val daysUnderBudget: Int,
    val daysOverBudget: Int
) {
    val averageDailySpend: Double
        get() = if (daysTracked <= 0) 0.0 else totalSpent / daysTracked
    val remaining: Double get() = totalBudget - totalSpent
    /** Share of tracked days that finished at or under budget, 0..1. */
    val adherenceFraction: Double
        get() = if (daysTracked <= 0) 0.0 else daysUnderBudget.toDouble() / daysTracked
    val adherencePercent: Int get() = (adherenceFraction * 100).roundToInt()
}

object BudgetCalculator {

    fun snapshot(budget: Double, spent: Double, pendingPlannedCost: Double = 0.0) =
        BudgetSnapshot(
            budget = budget.coerceAtLeast(0.0),
            spent = spent.coerceAtLeast(0.0),
            pendingPlannedCost = pendingPlannedCost.coerceAtLeast(0.0)
        )

    /** Aggregates only days that actually have tracked spending. */
    fun monthly(summaries: List<DaySummary>): MonthlyBudgetSummary {
        val tracked = summaries.filter { it.spent > 0.0 && it.budget > 0.0 }
        return MonthlyBudgetSummary(
            daysTracked = tracked.size,
            totalBudget = tracked.sumOf { it.budget },
            totalSpent = tracked.sumOf { it.spent },
            daysUnderBudget = tracked.count { it.spent <= it.budget },
            daysOverBudget = tracked.count { it.spent > it.budget }
        )
    }

    /** Cost of one line item = per-serving cost × servings. */
    fun lineCost(costPerServing: Double, quantity: Double): Double =
        (costPerServing.coerceAtLeast(0.0) * quantity.coerceAtLeast(0.0))

    fun round2(value: Double): Double = (value * 100).roundToInt() / 100.0
}
