import XCTest
@testable import FitBudgetCore

final class DailyChecklistTests: XCTestCase {

    private func fullDay() -> DaySummary {
        DaySummary(
            day: DayKey(100),
            plannedMealTypes: Set(MealType.allCases),
            completedMealTypes: Set(MealType.allCases),
            mealItemsPlanned: 12,
            mealItemsCompleted: 12,
            spent: 95,
            budget: 100,
            waterMl: 2_500,
            waterTargetMl: 2_500,
            steps: 9_000,
            stepGoal: 8_000,
            workoutsCompleted: 1,
            weightKg: 84.2
        )
    }

    func testChecklistHasEightSpecifiedRows() {
        let items = DailyChecklist.build(DaySummary(day: DayKey(1)))
        XCTAssertEqual(items.count, 8)
        XCTAssertEqual(
            items.map(\.id),
            [
                DailyChecklist.keyBreakfast,
                DailyChecklist.keyLunch,
                DailyChecklist.keySnack,
                DailyChecklist.keyDinner,
                DailyChecklist.keyWater,
                DailyChecklist.keyWorkout,
                DailyChecklist.keySteps,
                DailyChecklist.keyWeight
            ]
        )
    }

    func testEmptyDayIsZeroPercent() {
        let summary = DaySummary(day: DayKey(1), waterTargetMl: 2_500, stepGoal: 8_000)
        XCTAssertEqual(DailyChecklist.completionPercent(summary), 0)
        XCTAssertTrue(DailyChecklist.build(summary).allSatisfy { !$0.done })
    }

    func testFullyCompletedDayIsOneHundredPercent() {
        XCTAssertEqual(DailyChecklist.completionPercent(fullDay()), 100)
        XCTAssertTrue(DailyChecklist.build(fullDay()).allSatisfy(\.done))
    }

    func testSixOfEightRowsIsSeventyFivePercent() {
        let summary = DaySummary(
            day: DayKey(100),
            plannedMealTypes: Set(MealType.allCases),
            completedMealTypes: Set(MealType.allCases),
            spent: 95,
            budget: 100,
            waterMl: 2_500,
            waterTargetMl: 2_500,
            steps: 2_000,
            stepGoal: 8_000,
            workoutsCompleted: 1,
            weightKg: nil
        )
        XCTAssertEqual(DailyChecklist.completionPercent(summary), 75)
    }

    func testMealCountsAsDoneOnlyWhenEveryItemIsTicked() {
        let summary = DaySummary(
            day: DayKey(1),
            plannedMealTypes: [.breakfast, .lunch],
            completedMealTypes: [.breakfast],
            mealItemsPlanned: 6,
            mealItemsCompleted: 3
        )
        let items = DailyChecklist.build(summary)
        XCTAssertTrue(items.first { $0.id == DailyChecklist.keyBreakfast }!.done)
        XCTAssertFalse(items.first { $0.id == DailyChecklist.keyLunch }!.done)
        XCTAssertFalse(summary.dietGoalMet)
    }

    func testDietGoalNeedsEveryPlannedMeal() {
        let planned: Set<MealType> = [.breakfast, .lunch, .dinner]
        XCTAssertTrue(
            DaySummary(day: DayKey(1), plannedMealTypes: planned, completedMealTypes: planned).dietGoalMet
        )
        XCTAssertFalse(
            DaySummary(day: DayKey(1), plannedMealTypes: planned, completedMealTypes: [.breakfast]).dietGoalMet
        )
    }

    func testDayWithNoPlanIsNotADietSuccess() {
        XCTAssertFalse(DaySummary(day: DayKey(1)).dietGoalMet)
    }

    func testRowsWithoutPlannedMealSaySo() {
        let summary = DaySummary(day: DayKey(1), plannedMealTypes: [.breakfast])
        let dinner = DailyChecklist.build(summary).first { $0.id == DailyChecklist.keyDinner }!
        XCTAssertEqual(dinner.detail, "Nothing planned")
    }

    func testGoalFlagsRequireConfiguredGoal() {
        let noGoals = DaySummary(day: DayKey(1), waterMl: 500, steps: 100)
        XCTAssertFalse(noGoals.waterGoalMet)
        XCTAssertFalse(noGoals.stepGoalMet)
    }

    func testHasAnyActivityDetectsUntouchedDay() {
        XCTAssertFalse(DaySummary(day: DayKey(1), waterTargetMl: 2_500, stepGoal: 8_000).hasAnyActivity)
        XCTAssertTrue(DaySummary(day: DayKey(1), waterMl: 250).hasAnyActivity)
        XCTAssertTrue(fullDay().hasAnyActivity)
    }

    func testCompletionFractionOfEmptyListIsZero() {
        XCTAssertEqual(DailyChecklist.completionFraction([]), 0, accuracy: 0.001)
    }
}

final class StreakCalculatorTests: XCTestCase {

    private let today = DayKey(20_000)

    private func day(
        _ offset: Int,
        diet: Bool = false,
        workout: Bool = false,
        water: Bool = false,
        budget: Bool = false
    ) -> DaySummary {
        DaySummary(
            day: today - offset,
            plannedMealTypes: diet ? Set(MealType.allCases) : [],
            completedMealTypes: diet ? Set(MealType.allCases) : [],
            spent: budget ? 80 : 0,
            budget: 100,
            waterMl: water ? 2_500 : 0,
            waterTargetMl: 2_500,
            workoutsCompleted: workout ? 1 : 0
        )
    }

    func testEmptyHistoryHasNoStreaks() {
        let streaks = StreakCalculator.compute([], today: today)
        XCTAssertEqual(streaks.diet, 0)
        XCTAssertEqual(streaks.workout, 0)
        XCTAssertEqual(streaks.water, 0)
        XCTAssertEqual(streaks.budget, 0)
    }

    func testConsecutiveCompletedDaysBuildStreak() {
        let summaries = (0...6).map { day($0, diet: true) }
        XCTAssertEqual(StreakCalculator.compute(summaries, today: today).diet, 7)
    }

    func testGapBreaksStreak() {
        let summaries = [
            day(0, diet: true),
            day(1, diet: true),
            day(2, diet: false),
            day(3, diet: true)
        ]
        XCTAssertEqual(StreakCalculator.compute(summaries, today: today).diet, 2)
    }

    func testTodayStillInProgressKeepsYesterdayStreak() {
        let summaries = [day(0, diet: false), day(1, diet: true), day(2, diet: true)]
        XCTAssertEqual(StreakCalculator.compute(summaries, today: today).diet, 2)
    }

    func testUnfinishedTodayDoesNotInflateStreak() {
        let summaries = [day(0, diet: false), day(1, diet: false), day(2, diet: true)]
        XCTAssertEqual(StreakCalculator.compute(summaries, today: today).diet, 0)
    }

    func testMissingDayRecordEndsStreak() {
        let summaries = [day(0, diet: true), day(2, diet: true)]
        XCTAssertEqual(StreakCalculator.compute(summaries, today: today).diet, 1)
    }

    func testEachTrackerHasIndependentStreak() {
        let summaries = [
            day(0, diet: true, workout: false, water: true, budget: true),
            day(1, diet: true, workout: true, water: false, budget: true),
            day(2, diet: false, workout: true, water: true, budget: true)
        ]
        let streaks = StreakCalculator.compute(summaries, today: today)
        // diet: today + yesterday, broken two days ago
        XCTAssertEqual(streaks.diet, 2)
        // water: only today (yesterday missed the target)
        XCTAssertEqual(streaks.water, 1)
        // workout: not done today yet, so the run ending yesterday is still alive
        XCTAssertEqual(streaks.workout, 2)
        XCTAssertEqual(streaks.budget, 3)
    }

    func testDayWithoutTrackedSpendingCannotExtendBudgetStreak() {
        let summaries = [
            day(0, budget: true),
            DaySummary(day: today - 1, spent: 0, budget: 100),
            day(2, budget: true)
        ]
        XCTAssertEqual(StreakCalculator.compute(summaries, today: today).budget, 1)
    }

    func testOverspendingBreaksBudgetStreak() {
        let summaries = [
            day(0, budget: true),
            DaySummary(day: today - 1, spent: 150, budget: 100),
            day(2, budget: true)
        ]
        XCTAssertEqual(StreakCalculator.compute(summaries, today: today).budget, 1)
    }

    func testLongestStreakFindsBestRun() {
        let summaries = [
            day(9, diet: true),
            day(8, diet: true),
            day(7, diet: true),
            day(6, diet: true),
            day(5, diet: false),
            day(4, diet: true),
            day(3, diet: true)
        ]
        XCTAssertEqual(StreakCalculator.longestStreak(summaries) { $0.dietGoalMet }, 4)
    }

    func testLongestStreakIsZeroWhenNothingQualifies() {
        let summaries = (0...4).map { day($0) }
        XCTAssertEqual(StreakCalculator.longestStreak(summaries) { $0.dietGoalMet }, 0)
    }

    func testOverallStreakNeedsThreeQuartersOfChecklist() {
        let strong = DaySummary(
            day: today,
            plannedMealTypes: Set(MealType.allCases),
            completedMealTypes: Set(MealType.allCases),
            waterMl: 2_500,
            waterTargetMl: 2_500,
            steps: 9_000,
            stepGoal: 8_000,
            workoutsCompleted: 0,
            weightKg: nil
        )
        XCTAssertEqual(StreakCalculator.compute([strong], today: today).overall, 1)
    }
}
