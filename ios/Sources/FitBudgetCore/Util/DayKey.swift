import Foundation

/// A calendar day expressed as "days since 1970-01-01" in the user's own calendar.
///
/// This mirrors the Android build's `LocalDate.toEpochDay()` storage so both apps agree on what
/// "today" means, and it makes the midnight roll-over inherent: a new day simply has no rows yet.
public struct DayKey: Hashable, Comparable, Codable, Sendable {
    public let value: Int

    public init(_ value: Int) {
        self.value = value
    }

    public static func < (lhs: DayKey, rhs: DayKey) -> Bool { lhs.value < rhs.value }

    public func adding(_ days: Int) -> DayKey { DayKey(value + days) }

    public static func + (lhs: DayKey, rhs: Int) -> DayKey { lhs.adding(rhs) }

    public static func - (lhs: DayKey, rhs: Int) -> DayKey { lhs.adding(-rhs) }
}

public enum DayCalendar {

    /// Calendar used for every day-boundary calculation. Injectable so tests are deterministic.
    public static func calendar(timeZone: TimeZone? = nil) -> Calendar {
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = timeZone ?? TimeZone.current
        calendar.firstWeekday = 2 // Monday, matching the reminder day bitmask
        return calendar
    }

    /// Local midnight of 1970-01-01, the anchor for epoch-day maths.
    private static func epochStart(_ calendar: Calendar) -> Date {
        var components = DateComponents()
        components.year = 1970
        components.month = 1
        components.day = 1
        return calendar.date(from: components) ?? Date(timeIntervalSince1970: 0)
    }

    public static func dayKey(for date: Date, calendar: Calendar = DayCalendar.calendar()) -> DayKey {
        let start = calendar.startOfDay(for: date)
        let days = calendar.dateComponents([.day], from: epochStart(calendar), to: start).day ?? 0
        return DayKey(days)
    }

    public static func today(calendar: Calendar = DayCalendar.calendar()) -> DayKey {
        dayKey(for: Date(), calendar: calendar)
    }

    public static func date(from day: DayKey, calendar: Calendar = DayCalendar.calendar()) -> Date {
        calendar.date(byAdding: .day, value: day.value, to: epochStart(calendar)) ?? Date()
    }

    /// Inclusive range of the last `days` days, ending at `endingAt`.
    public static func lastDays(_ days: Int, endingAt end: DayKey) -> [DayKey] {
        let count = max(days, 1)
        return ((end.value - count + 1)...end.value).map(DayKey.init)
    }

    public static func monthRange(
        year: Int,
        month: Int,
        calendar: Calendar = DayCalendar.calendar()
    ) -> [DayKey] {
        var components = DateComponents()
        components.year = year
        components.month = month
        components.day = 1
        guard let first = calendar.date(from: components),
              let range = calendar.range(of: .day, in: .month, for: first) else { return [] }
        let firstKey = dayKey(for: first, calendar: calendar)
        return (0..<range.count).map { firstKey.adding($0) }
    }

    public static func monthRange(
        containing day: DayKey,
        calendar: Calendar = DayCalendar.calendar()
    ) -> [DayKey] {
        let date = self.date(from: day, calendar: calendar)
        let components = calendar.dateComponents([.year, .month], from: date)
        guard let year = components.year, let month = components.month else { return [day] }
        return monthRange(year: year, month: month, calendar: calendar)
    }

    /// 1 = Monday … 7 = Sunday, matching the reminder repeat bitmask.
    public static func isoWeekday(
        of day: DayKey,
        calendar: Calendar = DayCalendar.calendar()
    ) -> Int {
        let date = self.date(from: day, calendar: calendar)
        let weekday = calendar.component(.weekday, from: date) // 1 = Sunday
        return weekday == 1 ? 7 : weekday - 1
    }
}

/// Repeat-day bitmask helpers. Bit 0 = Monday … bit 6 = Sunday.
public enum WeekdayMask {

    public static let all = 0b1111111
    public static let weekdays = 0b0011111
    public static let weekends = 0b1100000

    public static func contains(_ mask: Int, isoWeekday: Int) -> Bool {
        guard (1...7).contains(isoWeekday) else { return false }
        return mask & (1 << (isoWeekday - 1)) != 0
    }

    public static func toggling(_ mask: Int, isoWeekday: Int) -> Int {
        guard (1...7).contains(isoWeekday) else { return mask }
        let bit = 1 << (isoWeekday - 1)
        return (mask & bit != 0 ? mask & ~bit : mask | bit) & all
    }

    public static func isoWeekdays(in mask: Int) -> [Int] {
        (1...7).filter { contains(mask, isoWeekday: $0) }
    }

    public static func shortName(isoWeekday: Int) -> String {
        switch isoWeekday {
        case 1: return "Mon"
        case 2: return "Tue"
        case 3: return "Wed"
        case 4: return "Thu"
        case 5: return "Fri"
        case 6: return "Sat"
        default: return "Sun"
        }
    }

    public static func describe(_ mask: Int) -> String {
        let normalised = mask & all
        switch normalised {
        case 0: return "Never"
        case all: return "Every day"
        case weekdays: return "Weekdays"
        case weekends: return "Weekends"
        default:
            return isoWeekdays(in: normalised).map(shortName(isoWeekday:)).joined(separator: ", ")
        }
    }
}
