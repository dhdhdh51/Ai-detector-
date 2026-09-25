import Foundation

public enum Formatters {

    public static func rupees(_ amount: Double) -> String {
        if abs(amount - amount.rounded()) < 0.005 {
            return "₹\(Int(amount.rounded()))"
        }
        return String(format: "₹%.2f", amount)
    }

    public static func rupees(_ amount: Int) -> String { "₹\(amount)" }

    public static func kg(_ value: Double) -> String { String(format: "%.1f kg", value) }

    public static func weight(_ valueKg: Double, units: UnitSystem) -> String {
        switch units {
        case .metric: return String(format: "%.1f kg", valueKg)
        case .imperial: return String(format: "%.1f lb", valueKg * 2.2046226)
        }
    }

    public static func height(_ valueCm: Double, units: UnitSystem) -> String {
        switch units {
        case .metric:
            return "\(Int(valueCm.rounded())) cm"
        case .imperial:
            let totalInches = valueCm / 2.54
            let feet = Int(totalInches / 12)
            let inches = Int((totalInches - Double(feet) * 12).rounded())
            return inches == 12 ? "\(feet + 1)' 0\"" : "\(feet)' \(inches)\""
        }
    }

    public static func signedKg(_ value: Double) -> String {
        if value > 0.05 { return String(format: "+%.1f kg", value) }
        if value < -0.05 { return String(format: "%.1f kg", value) }
        return "0.0 kg"
    }

    public static func litres(_ ml: Int) -> String {
        ml >= 1_000 ? String(format: "%.2f L", Double(ml) / 1_000) : "\(ml) ml"
    }

    public static func ml(_ ml: Int) -> String { "\(ml) ml" }

    public static func calories(_ value: Double) -> String { "\(Int(value.rounded())) kcal" }

    public static func grams(_ value: Double) -> String { String(format: "%.0f g", value) }

    public static func percent(_ fraction: Double) -> String {
        "\(Int((fraction * 100).rounded()))%"
    }

    public static func steps(_ value: Int) -> String {
        let formatter = NumberFormatter()
        formatter.numberStyle = .decimal
        formatter.groupingSeparator = ","
        formatter.locale = Locale(identifier: "en_IN")
        return formatter.string(from: NSNumber(value: value)) ?? "\(value)"
    }

    public static func quantity(_ value: Double) -> String {
        abs(value - value.rounded()) < 0.01
            ? "\(Int(value.rounded()))"
            : String(format: "%.2f", value)
    }

    public static func duration(_ seconds: Int) -> String {
        let safe = max(seconds, 0)
        let minutes = safe / 60
        let remaining = safe % 60
        if minutes >= 60 {
            return String(format: "%dh %02dm", minutes / 60, minutes % 60)
        }
        return String(format: "%d:%02d", minutes, remaining)
    }

    public static func time(hour: Int, minute: Int) -> String {
        let safeHour = min(max(hour, 0), 23)
        let safeMinute = min(max(minute, 0), 59)
        let suffix = safeHour < 12 ? "AM" : "PM"
        let display: Int
        if safeHour == 0 {
            display = 12
        } else if safeHour > 12 {
            display = safeHour - 12
        } else {
            display = safeHour
        }
        return String(format: "%d:%02d %@", display, safeMinute, suffix)
    }

    public static func time(minutesOfDay: Int) -> String {
        time(hour: minutesOfDay / 60, minute: minutesOfDay % 60)
    }

    public static func greeting(for date: Date = Date(), calendar: Calendar = DayCalendar.calendar()) -> String {
        switch calendar.component(.hour, from: date) {
        case 0...4: return "Good night"
        case 5...11: return "Good morning"
        case 12...16: return "Good afternoon"
        case 17...20: return "Good evening"
        default: return "Good night"
        }
    }

    public static func shortDate(_ day: DayKey, calendar: Calendar = DayCalendar.calendar()) -> String {
        let formatter = DateFormatter()
        formatter.calendar = calendar
        formatter.timeZone = calendar.timeZone
        formatter.dateFormat = "d MMM"
        return formatter.string(from: DayCalendar.date(from: day, calendar: calendar))
    }

    public static func fullDate(_ day: DayKey, calendar: Calendar = DayCalendar.calendar()) -> String {
        let formatter = DateFormatter()
        formatter.calendar = calendar
        formatter.timeZone = calendar.timeZone
        formatter.dateFormat = "d MMM yyyy"
        return formatter.string(from: DayCalendar.date(from: day, calendar: calendar))
    }

    public static func monthTitle(_ day: DayKey, calendar: Calendar = DayCalendar.calendar()) -> String {
        let formatter = DateFormatter()
        formatter.calendar = calendar
        formatter.timeZone = calendar.timeZone
        formatter.dateFormat = "MMMM yyyy"
        return formatter.string(from: DayCalendar.date(from: day, calendar: calendar))
    }

    public static func relativeDay(_ day: DayKey, today: DayKey = DayCalendar.today()) -> String {
        switch day.value - today.value {
        case 0: return "Today"
        case -1: return "Yesterday"
        case 1: return "Tomorrow"
        default: return shortDate(day)
        }
    }
}
