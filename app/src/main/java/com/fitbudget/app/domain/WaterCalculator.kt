package com.fitbudget.app.domain

import kotlin.math.ceil
import kotlin.math.roundToInt

data class WaterSnapshot(
    val consumedMl: Int,
    val targetMl: Int
) {
    val remainingMl: Int get() = (targetMl - consumedMl).coerceAtLeast(0)
    val isGoalMet: Boolean get() = targetMl > 0 && consumedMl >= targetMl
    val fraction: Double
        get() = if (targetMl <= 0) 0.0 else (consumedMl.toDouble() / targetMl).coerceIn(0.0, 1.0)
    val percent: Int get() = (fraction * 100).roundToInt()
    /** How many 250 ml glasses are still needed. */
    val glassesRemaining: Int get() = ceil(remainingMl / 250.0).toInt()
}

object WaterCalculator {

    val QUICK_AMOUNTS_ML = listOf(250, 500, 750, 1000)

    const val DEFAULT_TARGET_ML = 2500

    fun snapshot(consumedMl: Int, targetMl: Int) = WaterSnapshot(
        consumedMl = consumedMl.coerceAtLeast(0),
        targetMl = targetMl.coerceAtLeast(0)
    )

    fun total(amountsMl: List<Int>): Int = amountsMl.filter { it > 0 }.sum()

    /** Average daily intake across the supplied days (days with no log count as 0). */
    fun averageMl(dailyTotals: List<Int>): Int =
        if (dailyTotals.isEmpty()) 0 else dailyTotals.sum() / dailyTotals.size

    /** Share of days where the target was met, 0..1. */
    fun adherenceFraction(dailyTotals: List<Int>, targetMl: Int): Double {
        if (dailyTotals.isEmpty() || targetMl <= 0) return 0.0
        return dailyTotals.count { it >= targetMl }.toDouble() / dailyTotals.size
    }
}
