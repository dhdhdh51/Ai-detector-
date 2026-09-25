import XCTest
@testable import FitBudgetCore

final class ValidatorsTests: XCTestCase {

    private let calendar = DayCalendar.calendar(timeZone: TimeZone(identifier: "Asia/Kolkata")!)

    private func date(_ year: Int, _ month: Int, _ day: Int) -> Date {
        var components = DateComponents()
        components.year = year
        components.month = month
        components.day = day
        return calendar.date(from: components)!
    }

    func testHeightBounds() {
        XCTAssertTrue(Validators.height(100).isValid)
        XCTAssertTrue(Validators.height(172).isValid)
        XCTAssertTrue(Validators.height(250).isValid)
        XCTAssertFalse(Validators.height(99.9).isValid)
        XCTAssertFalse(Validators.height(251).isValid)
        XCTAssertFalse(Validators.height(0).isValid)
        XCTAssertFalse(Validators.height(-172).isValid)
        XCTAssertFalse(Validators.height(nil).isValid)
    }

    func testWeightBounds() {
        XCTAssertTrue(Validators.weight(30).isValid)
        XCTAssertTrue(Validators.weight(85).isValid)
        XCTAssertTrue(Validators.weight(300).isValid)
        XCTAssertFalse(Validators.weight(29).isValid)
        XCTAssertFalse(Validators.weight(301).isValid)
        XCTAssertFalse(Validators.weight(nil).isValid)
    }

    func testTargetWeightAboveCurrentIsRejected() {
        XCTAssertFalse(Validators.targetWeight(90, currentKg: 85).isValid)
        XCTAssertTrue(Validators.targetWeight(75, currentKg: 85).isValid)
        XCTAssertTrue(Validators.targetWeight(85, currentKg: 85).isValid)
    }

    func testUnrealisticGapIsRejected() {
        XCTAssertFalse(Validators.targetWeight(60, currentKg: 290).isValid)
    }

    func testTargetWeightHonoursAbsoluteBounds() {
        XCTAssertFalse(Validators.targetWeight(20, currentKg: 85).isValid)
        XCTAssertFalse(Validators.targetWeight(nil, currentKg: 85).isValid)
    }

    func testBudgetBounds() {
        XCTAssertTrue(Validators.budget(1).isValid)
        XCTAssertTrue(Validators.budget(100).isValid)
        XCTAssertTrue(Validators.budget(10_000).isValid)
        XCTAssertFalse(Validators.budget(0).isValid)
        XCTAssertFalse(Validators.budget(-100).isValid)
        XCTAssertFalse(Validators.budget(10_001).isValid)
        XCTAssertFalse(Validators.budget(nil).isValid)
    }

    func testWaterTargetBounds() {
        XCTAssertTrue(Validators.waterTarget(500).isValid)
        XCTAssertTrue(Validators.waterTarget(2_500).isValid)
        XCTAssertTrue(Validators.waterTarget(10_000).isValid)
        XCTAssertFalse(Validators.waterTarget(499).isValid)
        XCTAssertFalse(Validators.waterTarget(10_001).isValid)
        XCTAssertFalse(Validators.waterTarget(0).isValid)
    }

    func testStepBounds() {
        XCTAssertTrue(Validators.steps(0).isValid)
        XCTAssertTrue(Validators.steps(8_000).isValid)
        XCTAssertTrue(Validators.steps(100_000).isValid)
        XCTAssertFalse(Validators.steps(-1).isValid)
        XCTAssertFalse(Validators.steps(100_001).isValid)
    }

    func testDateOfBirthMustBePastAndSaneAge() {
        let today = date(2025, 9, 22)
        XCTAssertTrue(Validators.dateOfBirth(date(2006, 11, 22), on: today, calendar: calendar).isValid)
        XCTAssertFalse(Validators.dateOfBirth(date(2030, 1, 1), on: today, calendar: calendar).isValid)
        XCTAssertFalse(Validators.dateOfBirth(date(2020, 1, 1), on: today, calendar: calendar).isValid)
        XCTAssertFalse(Validators.dateOfBirth(date(1900, 1, 1), on: today, calendar: calendar).isValid)
        XCTAssertFalse(Validators.dateOfBirth(nil, on: today, calendar: calendar).isValid)
    }

    func testInvalidResultsCarryMessages() {
        XCTAssertNotNil(Validators.height(10).message)
        XCTAssertNotNil(Validators.budget(-1).message)
        XCTAssertNotNil(Validators.steps(200_000).message)
        XCTAssertNil(Validators.height(172).message)
    }

    func testNameValidation() {
        XCTAssertTrue(Validators.name("Rahul").isValid)
        XCTAssertFalse(Validators.name("").isValid)
        XCTAssertFalse(Validators.name("   ").isValid)
        XCTAssertFalse(Validators.name(String(repeating: "a", count: 41)).isValid)
    }

    func testQuantityRange() {
        XCTAssertTrue(Validators.quantity(1).isValid)
        XCTAssertTrue(Validators.quantity(0.25).isValid)
        XCTAssertFalse(Validators.quantity(0.1).isValid)
        XCTAssertFalse(Validators.quantity(25).isValid)
    }

    func testReminderIntervalRange() {
        XCTAssertTrue(Validators.reminderInterval(120).isValid)
        XCTAssertFalse(Validators.reminderInterval(5).isValid)
        XCTAssertFalse(Validators.reminderInterval(1_000).isValid)
    }

    func testReminderTimeRejectsImpossibleClock() {
        XCTAssertTrue(Validators.reminderTime(hour: 8, minute: 0).isValid)
        XCTAssertFalse(Validators.reminderTime(hour: 24, minute: 0).isValid)
        XCTAssertFalse(Validators.reminderTime(hour: 8, minute: 60).isValid)
        XCTAssertFalse(Validators.reminderTime(hour: nil, minute: 0).isValid)
    }

    func testParsingToleratesRupeesCommasAndSpaces() {
        XCTAssertEqual(Validators.parseDecimal("₹100"), 100)
        XCTAssertEqual(Validators.parseDecimal("1,500"), 1_500)
        XCTAssertEqual(Validators.parseDecimal(" 82.5 "), 82.5)
        XCTAssertNil(Validators.parseDecimal("abc"))
        XCTAssertNil(Validators.parseDecimal(""))
    }

    func testParseIntTruncatesDecimals() {
        XCTAssertEqual(Validators.parseInt("2500"), 2_500)
        XCTAssertEqual(Validators.parseInt("2,500"), 2_500)
        XCTAssertEqual(Validators.parseInt("2500.9"), 2_500)
        XCTAssertNil(Validators.parseInt("nope"))
    }
}
