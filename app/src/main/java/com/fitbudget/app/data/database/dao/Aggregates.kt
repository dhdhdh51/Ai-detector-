package com.fitbudget.app.data.database.dao

import com.fitbudget.app.domain.model.MealType

/** Per-day totals for the meal plan. */
data class DayMealAggregate(
    val epochDay: Long,
    val itemCount: Int,
    val completedCount: Int,
    val plannedCalories: Double,
    val plannedProtein: Double,
    val plannedCost: Double,
    val consumedCalories: Double,
    val consumedProtein: Double,
    val consumedCost: Double
)

/** Per-day, per-meal completion used to build the checklist and diet streak. */
data class MealTypeAggregate(
    val epochDay: Long,
    val mealType: MealType?,
    val itemCount: Int,
    val completedCount: Int
) {
    val fullyCompleted: Boolean get() = itemCount > 0 && completedCount >= itemCount
}

data class DayIntAggregate(val epochDay: Long, val value: Int)

data class DayDoubleAggregate(val epochDay: Long, val value: Double)
