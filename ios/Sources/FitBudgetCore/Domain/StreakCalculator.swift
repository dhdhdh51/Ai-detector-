import Foundation

public struct Streaks: Equatable, Sendable {
    public let diet: Int
    public let workout: Int
    public let water: Int
    public let budget: Int
    public let overall: Int

    public init(diet: Int = 0, workout: Int = 0, water: Int = 0, budget: Int = 0, overall: Int = 0) {
        self.diet = diet
        self.workout = workout
        self.water = water
        self.budget = budget
        self.overall = overall
    }
}

/// Streaks are computed from logged data only. A day increments a streak exclusively when the
/// requirement for that day was genuinely met.
///
/// The current day is allowed to be "not yet done" without breaking the streak (it is still in
/// progress); it simply does not add to the count until the goal is actually hit.
public enum StreakCalculator {

    public static func compute(_ summaries: [DaySummary], today: DayKey) -> Streaks {
        Streaks(
            diet: streak(summaries, today: today) { $0.dietGoalMet },
            workout: streak(summaries, today: today) { $0.workoutGoalMet },
            water: streak(summaries, today: today) { $0.waterGoalMet },
            budget: streak(summaries, today: today) { $0.budgetGoalMet },
            overall: streak(summaries, today: today) { DailyChecklist.completionPercent($0) >= 75 }
        )
    }

    /// Length of the run of consecutive qualifying days ending today (or ending yesterday when
    /// today has not qualified yet).
    public static func streak(
        _ summaries: [DaySummary],
        today: DayKey,
        predicate: (DaySummary) -> Bool
    ) -> Int {
        guard !summaries.isEmpty else { return 0 }
        var byDay: [Int: DaySummary] = [:]
        for summary in summaries { byDay[summary.day.value] = summary }

        var cursor = today.value
        // Today still in progress: start counting from yesterday instead of resetting to 0.
        if let todaySummary = byDay[cursor], predicate(todaySummary) {
            // keep cursor
        } else {
            cursor -= 1
        }

        var count = 0
        while let summary = byDay[cursor], predicate(summary) {
            count += 1
            cursor -= 1
        }
        return count
    }

    /// Longest qualifying run anywhere in the supplied history.
    public static func longestStreak(
        _ summaries: [DaySummary],
        predicate: (DaySummary) -> Bool
    ) -> Int {
        guard !summaries.isEmpty else { return 0 }
        let sorted = summaries.sorted { $0.day < $1.day }
        var best = 0
        var running = 0
        var previous: Int?
        for summary in sorted {
            if predicate(summary) {
                if let previous, summary.day.value == previous + 1 {
                    running += 1
                } else {
                    running = 1
                }
            } else {
                running = 0
            }
            best = max(best, running)
            previous = summary.day.value
        }
        return best
    }
}
