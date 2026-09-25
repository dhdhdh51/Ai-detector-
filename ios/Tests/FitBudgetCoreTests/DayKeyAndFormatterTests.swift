import XCTest
@testable import FitBudgetCore

final class DayKeyTests: XCTestCase {

    private let calendar = DayCalendar.calendar(timeZone: TimeZone(identifier: "Asia/Kolkata")!)

    private func date(_ year: Int, _ month: Int, _ day: Int) -> Date {
        var components = DateComponents()
        components.year = year
        components.month = month
        components.day = day
        return calendar.date(from: components)!
    }

    func testEpochDayMatchesJavaLocalDateSemantics() {
        // LocalDate.of(1970, 1, 1).toEpochDay() == 0
        XCTAssertEqual(DayCalendar.dayKey(for: date(1970, 1, 1), calendar: calendar).value, 0)
        XCTAssertEqual(DayCalendar.dayKey(for: date(1970, 1, 2), calendar: calendar).value, 1)
        // LocalDate.of(2025, 9, 22).toEpochDay() == 20353
        XCTAssertEqual(DayCalendar.dayKey(for: date(2025, 9, 22), calendar: calendar).value, 20_353)
    }

    func testDayKeyRoundTrips() {
        let original = date(2025, 9, 22)
        let key = DayCalendar.dayKey(for: original, calendar: calendar)
        let restored = DayCalendar.date(from: key, calendar: calendar)
        XCTAssertEqual(DayCalendar.dayKey(for: restored, calendar: calendar), key)
    }

    func testTimeOfDayDoesNotChangeTheDay() {
        var components = DateComponents()
        components.year = 2025
        components.month = 9
        components.day = 22
        components.hour = 23
        components.minute = 59
        let lateNight = calendar.date(from: components)!
        XCTAssertEqual(
            DayCalendar.dayKey(for: lateNight, calendar: calendar).value,
            DayCalendar.dayKey(for: date(2025, 9, 22), calendar: calendar).value
        )
    }

    func testArithmetic() {
        let key = DayKey(100)
        XCTAssertEqual((key + 5).value, 105)
        XCTAssertEqual((key - 5).value, 95)
        XCTAssertEqual(key.adding(-1).value, 99)
        XCTAssertTrue(DayKey(99) < DayKey(100))
    }

    func testLastDaysIsInclusiveAndEndsToday() {
        let days = DayCalendar.lastDays(7, endingAt: DayKey(100))
        XCTAssertEqual(days.count, 7)
        XCTAssertEqual(days.first?.value, 94)
        XCTAssertEqual(days.last?.value, 100)
    }

    func testMonthRangeCoversWholeMonth() {
        XCTAssertEqual(DayCalendar.monthRange(year: 2025, month: 2, calendar: calendar).count, 28)
        XCTAssertEqual(DayCalendar.monthRange(year: 2024, month: 2, calendar: calendar).count, 29)
        XCTAssertEqual(DayCalendar.monthRange(year: 2025, month: 9, calendar: calendar).count, 30)
    }

    func testMonthRangeContainingDay() {
        let key = DayCalendar.dayKey(for: date(2025, 9, 22), calendar: calendar)
        let range = DayCalendar.monthRange(containing: key, calendar: calendar)
        XCTAssertEqual(range.count, 30)
        XCTAssertTrue(range.contains(key))
    }

    func testIsoWeekday() {
        // 22 September 2025 is a Monday
        let monday = DayCalendar.dayKey(for: date(2025, 9, 22), calendar: calendar)
        XCTAssertEqual(DayCalendar.isoWeekday(of: monday, calendar: calendar), 1)
        XCTAssertEqual(DayCalendar.isoWeekday(of: monday + 6, calendar: calendar), 7)
    }
}

final class WeekdayMaskTests: XCTestCase {

    func testContainsAndToggle() {
        var mask = WeekdayMask.all
        XCTAssertTrue(WeekdayMask.contains(mask, isoWeekday: 1))
        mask = WeekdayMask.toggling(mask, isoWeekday: 1)
        XCTAssertFalse(WeekdayMask.contains(mask, isoWeekday: 1))
        mask = WeekdayMask.toggling(mask, isoWeekday: 1)
        XCTAssertTrue(WeekdayMask.contains(mask, isoWeekday: 1))
    }

    func testDescribe() {
        XCTAssertEqual(WeekdayMask.describe(WeekdayMask.all), "Every day")
        XCTAssertEqual(WeekdayMask.describe(0), "Never")
        XCTAssertEqual(WeekdayMask.describe(WeekdayMask.weekdays), "Weekdays")
        XCTAssertEqual(WeekdayMask.describe(WeekdayMask.weekends), "Weekends")
        XCTAssertEqual(WeekdayMask.describe(0b0000101), "Mon, Wed")
    }

    func testIsoWeekdaysInMask() {
        XCTAssertEqual(WeekdayMask.isoWeekdays(in: WeekdayMask.weekends), [6, 7])
        XCTAssertEqual(WeekdayMask.isoWeekdays(in: WeekdayMask.all).count, 7)
    }

    func testOutOfRangeWeekdayIsIgnored() {
        XCTAssertFalse(WeekdayMask.contains(WeekdayMask.all, isoWeekday: 0))
        XCTAssertFalse(WeekdayMask.contains(WeekdayMask.all, isoWeekday: 8))
        XCTAssertEqual(WeekdayMask.toggling(WeekdayMask.all, isoWeekday: 9), WeekdayMask.all)
    }
}

final class FormattersTests: XCTestCase {

    private let calendar = DayCalendar.calendar(timeZone: TimeZone(identifier: "Asia/Kolkata")!)

    func testRupeeFormatting() {
        XCTAssertEqual(Formatters.rupees(100.0), "₹100")
        XCTAssertEqual(Formatters.rupees(42.5), "₹42.50")
        XCTAssertEqual(Formatters.rupees(58), "₹58")
    }

    func testWeightAndHeightUnits() {
        XCTAssertEqual(Formatters.weight(85, units: .metric), "85.0 kg")
        XCTAssertEqual(Formatters.weight(85, units: .imperial), "187.4 lb")
        XCTAssertEqual(Formatters.height(172, units: .metric), "172 cm")
        XCTAssertEqual(Formatters.height(172, units: .imperial), "5' 8\"")
    }

    func testLitresAndMl() {
        XCTAssertEqual(Formatters.litres(2_500), "2.50 L")
        XCTAssertEqual(Formatters.litres(750), "750 ml")
        XCTAssertEqual(Formatters.ml(250), "250 ml")
    }

    func testSignedKg() {
        XCTAssertEqual(Formatters.signedKg(1.2), "+1.2 kg")
        XCTAssertEqual(Formatters.signedKg(-1.2), "-1.2 kg")
        XCTAssertEqual(Formatters.signedKg(0), "0.0 kg")
    }

    func testTimeFormattingUses12HourClock() {
        XCTAssertEqual(Formatters.time(hour: 8, minute: 0), "8:00 AM")
        XCTAssertEqual(Formatters.time(hour: 13, minute: 0), "1:00 PM")
        XCTAssertEqual(Formatters.time(hour: 0, minute: 0), "12:00 AM")
        XCTAssertEqual(Formatters.time(hour: 12, minute: 30), "12:30 PM")
        XCTAssertEqual(Formatters.time(hour: 22, minute: 30), "10:30 PM")
    }

    func testTimeFormattingClampsNonsense() {
        XCTAssertEqual(Formatters.time(hour: 99, minute: 99), "11:59 PM")
    }

    func testMinutesOfDayFormatting() {
        XCTAssertEqual(Formatters.time(minutesOfDay: 8 * 60), "8:00 AM")
        XCTAssertEqual(Formatters.time(minutesOfDay: 22 * 60 + 30), "10:30 PM")
    }

    func testDurationFormatting() {
        XCTAssertEqual(Formatters.duration(0), "0:00")
        XCTAssertEqual(Formatters.duration(65), "1:05")
        XCTAssertEqual(Formatters.duration(3_725), "1h 02m")
        XCTAssertEqual(Formatters.duration(-10), "0:00")
    }

    func testQuantityFormatting() {
        XCTAssertEqual(Formatters.quantity(2), "2")
        XCTAssertEqual(Formatters.quantity(1.5), "1.50")
    }

    func testGreetingChangesAcrossTheDay() {
        func at(_ hour: Int) -> Date {
            var components = DateComponents()
            components.year = 2025
            components.month = 9
            components.day = 22
            components.hour = hour
            return calendar.date(from: components)!
        }
        XCTAssertEqual(Formatters.greeting(for: at(7), calendar: calendar), "Good morning")
        XCTAssertEqual(Formatters.greeting(for: at(13), calendar: calendar), "Good afternoon")
        XCTAssertEqual(Formatters.greeting(for: at(18), calendar: calendar), "Good evening")
        XCTAssertEqual(Formatters.greeting(for: at(23), calendar: calendar), "Good night")
        XCTAssertEqual(Formatters.greeting(for: at(2), calendar: calendar), "Good night")
    }

    func testRelativeDayLabels() {
        let today = DayKey(100)
        XCTAssertEqual(Formatters.relativeDay(today, today: today), "Today")
        XCTAssertEqual(Formatters.relativeDay(today - 1, today: today), "Yesterday")
        XCTAssertEqual(Formatters.relativeDay(today + 1, today: today), "Tomorrow")
    }
}

final class WorkoutLibraryTests: XCTestCase {

    func testLibraryCoversEveryCategory() {
        for category in WorkoutCategory.allCases {
            XCTAssertFalse(
                WorkoutLibrary.templates(category: category).isEmpty,
                "category \(category) should have at least one workout"
            )
        }
    }

    func testTemplatesAreWellFormed() {
        XCTAssertEqual(WorkoutLibrary.templates.count, 8)
        for template in WorkoutLibrary.templates {
            XCTAssertFalse(template.exercises.isEmpty)
            XCTAssertGreaterThan(template.estimatedMinutes, 0)
            XCTAssertGreaterThan(template.estimatedCalories, 0)
            XCTAssertGreaterThan(template.totalSets, 0)
            for exercise in template.exercises {
                XCTAssertGreaterThan(exercise.sets, 0)
                XCTAssertFalse(exercise.repsLabel.isEmpty)
                XCTAssertFalse(exercise.instructions.isEmpty)
                if let duration = exercise.durationSeconds {
                    XCTAssertGreaterThan(duration, 0)
                    XCTAssertTrue(exercise.isTimed)
                }
            }
        }
    }

    func testTemplateIdsAreUniqueAndLookupWorks() {
        let ids = WorkoutLibrary.templates.map(\.id)
        XCTAssertEqual(Set(ids).count, ids.count)
        XCTAssertNotNil(WorkoutLibrary.template(id: "full_body_starter"))
        XCTAssertNil(WorkoutLibrary.template(id: "does_not_exist"))
    }
}
