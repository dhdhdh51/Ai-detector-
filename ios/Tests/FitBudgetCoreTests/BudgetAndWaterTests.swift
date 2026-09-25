import XCTest
@testable import FitBudgetCore

final class BudgetCalculatorTests: XCTestCase {

    func testRemainingIsBudgetMinusSpending() {
        let snapshot = BudgetCalculator.snapshot(budget: 100, spent: 42)
        XCTAssertEqual(snapshot.remaining, 58, accuracy: 0.001)
        XCTAssertFalse(snapshot.isOverBudget)
        XCTAssertEqual(snapshot.overBy, 0, accuracy: 0.001)
    }

    func testOverBudgetReportsOvershoot() {
        let snapshot = BudgetCalculator.snapshot(budget: 100, spent: 118)
        XCTAssertTrue(snapshot.isOverBudget)
        XCTAssertEqual(snapshot.overBy, 18, accuracy: 0.001)
        XCTAssertEqual(snapshot.remaining, -18, accuracy: 0.001)
        XCTAssertEqual(snapshot.usedFraction, 1, accuracy: 0.001)
    }

    func testUsedFractionIsClampedAndSafeForZeroBudget() {
        XCTAssertEqual(BudgetCalculator.snapshot(budget: 100, spent: 42).usedFraction, 0.42, accuracy: 0.001)
        XCTAssertEqual(BudgetCalculator.snapshot(budget: 0, spent: 42).usedFraction, 0, accuracy: 0.001)
    }

    func testNegativeInputsAreClamped() {
        let snapshot = BudgetCalculator.snapshot(budget: -50, spent: -10)
        XCTAssertEqual(snapshot.budget, 0, accuracy: 0.001)
        XCTAssertEqual(snapshot.spent, 0, accuracy: 0.001)
    }

    func testProjectedSpendIncludesPendingPlan() {
        let snapshot = BudgetCalculator.snapshot(budget: 100, spent: 60, pendingPlannedCost: 50)
        XCTAssertEqual(snapshot.projectedSpend, 110, accuracy: 0.001)
        XCTAssertTrue(snapshot.projectedOverBudget)
        XCTAssertFalse(snapshot.isOverBudget)
    }

    func testLineCost() {
        XCTAssertEqual(BudgetCalculator.lineCost(costPerServing: 7, quantity: 3), 21, accuracy: 0.001)
        XCTAssertEqual(BudgetCalculator.lineCost(costPerServing: 7, quantity: 1.5), 10.5, accuracy: 0.001)
        XCTAssertEqual(BudgetCalculator.lineCost(costPerServing: 7, quantity: -2), 0, accuracy: 0.001)
    }

    func testMonthlySummaryCountsOnlyTrackedDays() {
        let summaries = [
            DaySummary(day: DayKey(1), spent: 90, budget: 100),
            DaySummary(day: DayKey(2), spent: 120, budget: 100),
            DaySummary(day: DayKey(3), spent: 0, budget: 100),
            DaySummary(day: DayKey(4), spent: 100, budget: 100)
        ]
        let monthly = BudgetCalculator.monthly(summaries)

        XCTAssertEqual(monthly.daysTracked, 3)
        XCTAssertEqual(monthly.totalBudget, 300, accuracy: 0.001)
        XCTAssertEqual(monthly.totalSpent, 310, accuracy: 0.001)
        XCTAssertEqual(monthly.daysUnderBudget, 2)
        XCTAssertEqual(monthly.daysOverBudget, 1)
        XCTAssertEqual(monthly.averageDailySpend, 103.33, accuracy: 0.01)
        XCTAssertEqual(monthly.adherencePercent, 67)
    }

    func testMonthlySummaryOfEmptyMonth() {
        let monthly = BudgetCalculator.monthly([])
        XCTAssertEqual(monthly.daysTracked, 0)
        XCTAssertEqual(monthly.averageDailySpend, 0, accuracy: 0.001)
        XCTAssertEqual(monthly.adherencePercent, 0)
    }

    func testDayWithoutSpendingIsNotABudgetSuccess() {
        XCTAssertFalse(DaySummary(day: DayKey(1), spent: 0, budget: 100).budgetGoalMet)
        XCTAssertTrue(DaySummary(day: DayKey(2), spent: 80, budget: 100).budgetGoalMet)

        let overspent = DaySummary(day: DayKey(3), spent: 180, budget: 100)
        XCTAssertFalse(overspent.budgetGoalMet)
        XCTAssertTrue(overspent.isOverBudget)
    }
}

final class WaterCalculatorTests: XCTestCase {

    func testRemainingNeverNegative() {
        XCTAssertEqual(WaterCalculator.snapshot(consumedMl: 1_500, targetMl: 2_500).remainingMl, 1_000)
        XCTAssertEqual(WaterCalculator.snapshot(consumedMl: 3_000, targetMl: 2_500).remainingMl, 0)
    }

    func testGoalMetAtOrAboveTarget() {
        XCTAssertFalse(WaterCalculator.snapshot(consumedMl: 2_499, targetMl: 2_500).isGoalMet)
        XCTAssertTrue(WaterCalculator.snapshot(consumedMl: 2_500, targetMl: 2_500).isGoalMet)
        XCTAssertTrue(WaterCalculator.snapshot(consumedMl: 4_000, targetMl: 2_500).isGoalMet)
    }

    func testFractionAndPercentAreClamped() {
        XCTAssertEqual(WaterCalculator.snapshot(consumedMl: 1_500, targetMl: 2_500).fraction, 0.6, accuracy: 0.001)
        XCTAssertEqual(WaterCalculator.snapshot(consumedMl: 1_500, targetMl: 2_500).percent, 60)
        XCTAssertEqual(WaterCalculator.snapshot(consumedMl: 5_000, targetMl: 2_500).fraction, 1, accuracy: 0.001)
        XCTAssertEqual(WaterCalculator.snapshot(consumedMl: 5_000, targetMl: 2_500).percent, 100)
    }

    func testZeroTargetCannotDivideByZero() {
        let snapshot = WaterCalculator.snapshot(consumedMl: 1_000, targetMl: 0)
        XCTAssertEqual(snapshot.fraction, 0, accuracy: 0.001)
        XCTAssertFalse(snapshot.isGoalMet)
    }

    func testNegativeIntakeIsClamped() {
        XCTAssertEqual(WaterCalculator.snapshot(consumedMl: -500, targetMl: 2_500).consumedMl, 0)
    }

    func testGlassesRemainingRoundsUp() {
        XCTAssertEqual(WaterCalculator.snapshot(consumedMl: 1_500, targetMl: 2_500).glassesRemaining, 4)
        XCTAssertEqual(WaterCalculator.snapshot(consumedMl: 1_400, targetMl: 2_500).glassesRemaining, 5)
        XCTAssertEqual(WaterCalculator.snapshot(consumedMl: 2_500, targetMl: 2_500).glassesRemaining, 0)
    }

    func testTotalSumsOnlyPositiveEntries() {
        XCTAssertEqual(WaterCalculator.total([250, 500, 1_000]), 1_750)
        XCTAssertEqual(WaterCalculator.total([250, -500, 500]), 750)
        XCTAssertEqual(WaterCalculator.total([]), 0)
    }

    func testAverageOfEmptyHistoryIsZero() {
        XCTAssertEqual(WaterCalculator.averageMl([]), 0)
        XCTAssertEqual(WaterCalculator.averageMl([1_500, 2_500]), 2_000)
    }

    func testAdherenceIsShareOfDaysMeetingTarget() {
        let totals = [2_500, 2_600, 1_000, 2_500]
        XCTAssertEqual(
            WaterCalculator.adherenceFraction(dailyTotals: totals, targetMl: 2_500),
            0.75, accuracy: 0.001
        )
        XCTAssertEqual(WaterCalculator.adherenceFraction(dailyTotals: totals, targetMl: 0), 0, accuracy: 0.001)
        XCTAssertEqual(WaterCalculator.adherenceFraction(dailyTotals: [], targetMl: 2_500), 0, accuracy: 0.001)
    }

    func testQuickAmountsMatchSpecifiedButtons() {
        XCTAssertEqual(WaterCalculator.quickAmountsMl, [250, 500, 750, 1_000])
    }
}
