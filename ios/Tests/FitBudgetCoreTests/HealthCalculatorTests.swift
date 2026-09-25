import XCTest
@testable import FitBudgetCore

final class HealthCalculatorTests: XCTestCase {

    private let calendar = DayCalendar.calendar(timeZone: TimeZone(identifier: "Asia/Kolkata")!)

    private func date(_ year: Int, _ month: Int, _ day: Int) -> Date {
        var components = DateComponents()
        components.year = year
        components.month = month
        components.day = day
        return calendar.date(from: components)!
    }

    // MARK: - Age

    func testAgeIsCompletedYears() {
        let dob = date(2006, 11, 22)
        XCTAssertEqual(HealthCalculator.age(dateOfBirth: dob, on: date(2025, 9, 22), calendar: calendar), 18)
        XCTAssertEqual(HealthCalculator.age(dateOfBirth: dob, on: date(2025, 11, 22), calendar: calendar), 19)
        XCTAssertEqual(HealthCalculator.age(dateOfBirth: dob, on: date(2025, 11, 21), calendar: calendar), 18)
    }

    func testAgeOfFutureBirthDateIsZero() {
        let dob = date(2030, 1, 1)
        XCTAssertEqual(HealthCalculator.age(dateOfBirth: dob, on: date(2025, 1, 1), calendar: calendar), 0)
    }

    // MARK: - BMI

    func testBMIUsesWeightOverHeightSquared() {
        XCTAssertEqual(HealthCalculator.bmi(weightKg: 85, heightCm: 172), 28.73, accuracy: 0.01)
        XCTAssertEqual(HealthCalculator.bmi(weightKg: 70, heightCm: 175), 22.86, accuracy: 0.01)
    }

    func testBMIReturnsZeroForImpossibleInput() {
        XCTAssertEqual(HealthCalculator.bmi(weightKg: 85, heightCm: 0), 0)
        XCTAssertEqual(HealthCalculator.bmi(weightKg: 85, heightCm: -10), 0)
        XCTAssertEqual(HealthCalculator.bmi(weightKg: 0, heightCm: 172), 0)
    }

    func testBMICategories() {
        XCTAssertEqual(HealthCalculator.bmiCategory(17), "Underweight")
        XCTAssertEqual(HealthCalculator.bmiCategory(22), "Healthy range")
        XCTAssertEqual(HealthCalculator.bmiCategory(27), "Overweight")
        XCTAssertEqual(HealthCalculator.bmiCategory(31), "Obese range")
        XCTAssertEqual(HealthCalculator.bmiCategory(0), "—")
    }

    func testHealthyWeightRange() {
        let range = HealthCalculator.healthyWeightRange(heightCm: 172)
        XCTAssertEqual(range.lowerBound, 54.7, accuracy: 0.1)
        XCTAssertEqual(range.upperBound, 73.6, accuracy: 0.1)
    }

    // MARK: - Progress

    func testWeightDifference() {
        XCTAssertEqual(HealthCalculator.weightDifference(currentKg: 85, targetKg: 75), 10, accuracy: 0.001)
        XCTAssertEqual(HealthCalculator.weightDifference(currentKg: 73, targetKg: 75), -2, accuracy: 0.001)
    }

    func testProgressPercentFormula() {
        XCTAssertEqual(
            HealthCalculator.progressPercent(startKg: 85, currentKg: 80, targetKg: 75),
            50, accuracy: 0.001
        )
        XCTAssertEqual(
            HealthCalculator.progressPercent(startKg: 85, currentKg: 85, targetKg: 75),
            0, accuracy: 0.001
        )
        XCTAssertEqual(
            HealthCalculator.progressPercent(startKg: 85, currentKg: 75, targetKg: 75),
            100, accuracy: 0.001
        )
    }

    func testProgressPercentIsClamped() {
        XCTAssertEqual(
            HealthCalculator.progressPercent(startKg: 85, currentKg: 90, targetKg: 75),
            0, accuracy: 0.001
        )
        XCTAssertEqual(
            HealthCalculator.progressPercent(startKg: 85, currentKg: 70, targetKg: 75),
            100, accuracy: 0.001
        )
    }

    func testProgressPercentHandlesStartEqualToTarget() {
        XCTAssertEqual(
            HealthCalculator.progressPercent(startKg: 75, currentKg: 75, targetKg: 75),
            100, accuracy: 0.001
        )
        XCTAssertEqual(
            HealthCalculator.progressPercent(startKg: 75, currentKg: 78, targetKg: 75),
            0, accuracy: 0.001
        )
    }

    // MARK: - Energy

    func testBMRForMen() {
        // 10*85 + 6.25*172 - 5*18 + 5 = 1840
        XCTAssertEqual(
            HealthCalculator.bmr(weightKg: 85, heightCm: 172, age: 18, gender: .male),
            1840, accuracy: 0.5
        )
    }

    func testBMRForWomen() {
        // 10*65 + 6.25*160 - 5*30 - 161 = 1339
        XCTAssertEqual(
            HealthCalculator.bmr(weightKg: 65, heightCm: 160, age: 30, gender: .female),
            1339, accuracy: 0.5
        )
    }

    func testBMRIsZeroForImpossibleMeasurements() {
        XCTAssertEqual(HealthCalculator.bmr(weightKg: 0, heightCm: 172, age: 25, gender: .male), 0)
        XCTAssertEqual(HealthCalculator.bmr(weightKg: 80, heightCm: 0, age: 25, gender: .male), 0)
    }

    func testTDEEMultipliesByActivityFactor() {
        XCTAssertEqual(HealthCalculator.tdee(bmr: 1840, activityLevel: .light), 2530, accuracy: 0.5)
        XCTAssertEqual(HealthCalculator.tdee(bmr: 1840, activityLevel: .sedentary), 2208, accuracy: 0.5)
    }

    func testCalorieTargetAppliesDeficitWhenLosing() {
        let tdee = 2530.0
        let target = HealthCalculator.calorieTarget(tdee: tdee, gender: .male, losingWeight: true)
        XCTAssertLessThan(target, tdee)
        XCTAssertGreaterThanOrEqual(target, 1500)
        XCTAssertEqual(target, 2020, accuracy: 0.1)
    }

    func testCalorieTargetEqualsMaintenanceWhenNotLosing() {
        XCTAssertEqual(
            HealthCalculator.calorieTarget(tdee: 2530, gender: .male, losingWeight: false),
            2530, accuracy: 0.1
        )
    }

    func testCalorieTargetRespectsSafetyFloor() {
        let target = HealthCalculator.calorieTarget(tdee: 1400, gender: .female, losingWeight: true)
        XCTAssertGreaterThanOrEqual(target, 1200)
    }

    func testDeficitRangeIsSane() {
        let range = HealthCalculator.deficitRange(tdee: 2530)
        XCTAssertLessThanOrEqual(range.lowerBound, range.upperBound)
        XCTAssertGreaterThanOrEqual(range.lowerBound, 200)
        XCTAssertLessThanOrEqual(range.upperBound, 750)
        XCTAssertEqual(HealthCalculator.deficitRange(tdee: 0), 0...0)
    }

    func testWeeklyWeightChangeUses7700kcal() {
        XCTAssertEqual(
            HealthCalculator.weeklyWeightChangeKg(tdee: 2500, intakeCalories: 2000),
            0.4545, accuracy: 0.001
        )
        XCTAssertEqual(
            HealthCalculator.weeklyWeightChangeKg(tdee: 2500, intakeCalories: 2500),
            0, accuracy: 0.001
        )
        XCTAssertLessThan(HealthCalculator.weeklyWeightChangeKg(tdee: 2000, intakeCalories: 2500), 0)
    }

    func testWeeksToTargetIsNilWhenNotApplicable() {
        XCTAssertNil(HealthCalculator.weeksToTarget(currentKg: 75, targetKg: 75, weeklyLossKg: 0.5))
        XCTAssertNil(HealthCalculator.weeksToTarget(currentKg: 85, targetKg: 75, weeklyLossKg: 0))
    }

    func testWeeksToTargetRoundsUp() {
        XCTAssertEqual(HealthCalculator.weeksToTarget(currentKg: 85, targetKg: 75, weeklyLossKg: 0.5), 20)
        XCTAssertEqual(HealthCalculator.weeksToTarget(currentKg: 85, targetKg: 75, weeklyLossKg: 0.49), 21)
    }

    func testProteinTargetScalesWithWeight() {
        XCTAssertEqual(HealthCalculator.proteinTargetGrams(weightKg: 85), 120, accuracy: 0.1)
        XCTAssertEqual(HealthCalculator.proteinTargetGrams(weightKg: 0), 0, accuracy: 0.1)
    }

    func testSuggestedWaterStaysInRange() {
        XCTAssertEqual(HealthCalculator.suggestedWaterMl(weightKg: 85), 3_000)
        XCTAssertEqual(HealthCalculator.suggestedWaterMl(weightKg: 40), 2_000)
        XCTAssertEqual(HealthCalculator.suggestedWaterMl(weightKg: 200), 4_000)
        XCTAssertEqual(HealthCalculator.suggestedWaterMl(weightKg: 0), 2_500)
    }

    func testAverageWeeklyChange() {
        XCTAssertEqual(
            HealthCalculator.averageWeeklyChangeKg(firstWeightKg: 85, lastWeightKg: 83, daysBetween: 28),
            -0.5, accuracy: 0.001
        )
        XCTAssertEqual(
            HealthCalculator.averageWeeklyChangeKg(firstWeightKg: 85, lastWeightKg: 83, daysBetween: 0),
            0, accuracy: 0.001
        )
    }

    /// The iOS and Android builds must agree, so this pins the shared example profile end to end.
    func testExampleProfileMatchesAndroidBuild() {
        let bmi = HealthCalculator.bmi(weightKg: 85, heightCm: 172)
        XCTAssertEqual(bmi, 28.73, accuracy: 0.01)
        XCTAssertEqual(HealthCalculator.bmiCategory(bmi), "Overweight")

        let bmr = HealthCalculator.bmr(weightKg: 85, heightCm: 172, age: 18, gender: .male)
        let tdee = HealthCalculator.tdee(bmr: bmr, activityLevel: .light)
        XCTAssertEqual(tdee, 2530, accuracy: 1)

        let target = HealthCalculator.calorieTarget(tdee: tdee, gender: .male, losingWeight: true)
        XCTAssertEqual(target, 2020, accuracy: 0.5)
        XCTAssertEqual(HealthCalculator.proteinTargetGrams(weightKg: 85), 120, accuracy: 0.5)
        XCTAssertEqual(HealthCalculator.suggestedWaterMl(weightKg: 85), 3_000)
    }
}
