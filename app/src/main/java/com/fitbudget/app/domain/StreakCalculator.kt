package com.fitbudget.app.domain

data class Streaks(
    val diet: Int = 0,
    val workout: Int = 0,
    val water: Int = 0,
    val budget: Int = 0,
    val overall: Int = 0
)

/**
 * Streaks are computed from logged data only. A day increments a streak exclusively when the
 * requirement for that day was genuinely met.
 *
 * The current day is allowed to be "not yet done" without breaking the streak (the day is
 * still in progress); it simply does not add to the count until the goal is actually hit.
 */
object StreakCalculator {

    fun compute(summaries: List<DaySummary>, todayEpochDay: Long): Streaks = Streaks(
        diet = streakOf(summaries, todayEpochDay) { it.dietGoalMet },
        workout = streakOf(summaries, todayEpochDay) { it.workoutGoalMet },
        water = streakOf(summaries, todayEpochDay) { it.waterGoalMet },
        budget = streakOf(summaries, todayEpochDay) { it.budgetGoalMet },
        overall = streakOf(summaries, todayEpochDay) { DailyChecklist.completionPercent(it) >= 75 }
    )

    /**
     * Length of the run of consecutive qualifying days ending today (or ending yesterday when
     * today has not qualified yet).
     */
    fun streakOf(
        summaries: List<DaySummary>,
        todayEpochDay: Long,
        predicate: (DaySummary) -> Boolean
    ): Int {
        if (summaries.isEmpty()) return 0
        val byDay = summaries.associateBy { it.epochDay }
        var day = todayEpochDay
        // Today still in progress: start counting from yesterday instead of resetting to 0.
        if (byDay[day]?.let(predicate) != true) day -= 1
        var count = 0
        while (true) {
            val summary = byDay[day] ?: break
            if (!predicate(summary)) break
            count += 1
            day -= 1
        }
        return count
    }

    /** Longest qualifying run anywhere in the supplied history. */
    fun longestStreak(
        summaries: List<DaySummary>,
        predicate: (DaySummary) -> Boolean
    ): Int {
        if (summaries.isEmpty()) return 0
        val sorted = summaries.sortedBy { it.epochDay }
        var best = 0
        var running = 0
        var previousDay: Long? = null
        for (summary in sorted) {
            val qualifies = predicate(summary)
            running = when {
                !qualifies -> 0
                previousDay != null && summary.epochDay == previousDay + 1 -> running + 1
                else -> 1
            }
            if (running > best) best = running
            previousDay = summary.epochDay
        }
        return best
    }
}
