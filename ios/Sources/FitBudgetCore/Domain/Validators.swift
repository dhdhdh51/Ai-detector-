import Foundation

/// Result of validating one input field.
public struct FieldResult: Equatable, Sendable {
    public let isValid: Bool
    public let message: String?

    public init(isValid: Bool, message: String? = nil) {
        self.isValid = isValid
        self.message = message
    }

    public static let valid = FieldResult(isValid: true)

    public static func invalid(_ message: String) -> FieldResult {
        FieldResult(isValid: false, message: message)
    }
}

/// Central input validation, so the app can never persist an impossible value.
public enum Validators {

    public static let heightRange: ClosedRange<Double> = 100...250
    public static let weightRange: ClosedRange<Double> = 30...300
    public static let budgetRange: ClosedRange<Double> = 1...10_000
    public static let waterTargetRange: ClosedRange<Int> = 500...10_000
    public static let stepRange: ClosedRange<Int> = 0...100_000
    public static let ageRange: ClosedRange<Int> = 10...100
    public static let foodCostRange: ClosedRange<Double> = 0...10_000
    public static let foodCalorieRange: ClosedRange<Double> = 0...5_000
    public static let foodProteinRange: ClosedRange<Double> = 0...300
    public static let quantityRange: ClosedRange<Double> = 0.25...20
    public static let waterEntryRange: ClosedRange<Int> = 10...5_000
    public static let reminderIntervalRange: ClosedRange<Int> = 15...720

    public static func name(_ value: String) -> FieldResult {
        let trimmed = value.trimmingCharacters(in: .whitespacesAndNewlines)
        if trimmed.isEmpty { return .invalid("Enter your name.") }
        if trimmed.count > 40 { return .invalid("Keep the name under 40 characters.") }
        return .valid
    }

    public static func height(_ cm: Double?) -> FieldResult {
        guard let cm else { return .invalid("Enter your height in cm.") }
        guard heightRange.contains(cm) else {
            return .invalid("Height must be between 100 and 250 cm.")
        }
        return .valid
    }

    public static func weight(_ kg: Double?, label: String = "Weight") -> FieldResult {
        guard let kg else { return .invalid("Enter a \(label.lowercased()) in kg.") }
        guard weightRange.contains(kg) else {
            return .invalid("\(label) must be between 30 and 300 kg.")
        }
        return .valid
    }

    /// A weight-loss goal requires target <= current. Equal values are allowed (maintenance).
    public static func targetWeight(_ targetKg: Double?, currentKg: Double?) -> FieldResult {
        let base = weight(targetKg, label: "Target weight")
        guard base.isValid, let targetKg else { return base }
        guard let currentKg else { return .valid }
        if targetKg > currentKg {
            return .invalid("Target must be lower than or equal to your current weight.")
        }
        if currentKg - targetKg > 100 {
            return .invalid("That gap is too large to plan safely. Pick a closer target.")
        }
        return .valid
    }

    public static func budget(_ rupees: Double?) -> FieldResult {
        guard let rupees else { return .invalid("Enter a daily budget.") }
        guard budgetRange.contains(rupees) else {
            return .invalid("Budget must be between ₹1 and ₹10,000 a day.")
        }
        return .valid
    }

    public static func waterTarget(_ ml: Int?) -> FieldResult {
        guard let ml else { return .invalid("Enter a water target in ml.") }
        guard waterTargetRange.contains(ml) else {
            return .invalid("Water target must be between 500 ml and 10 L.")
        }
        return .valid
    }

    public static func waterEntry(_ ml: Int?) -> FieldResult {
        guard let ml else { return .invalid("Enter an amount in ml.") }
        guard waterEntryRange.contains(ml) else {
            return .invalid("Log between 10 ml and 5,000 ml at a time.")
        }
        return .valid
    }

    public static func steps(_ value: Int?) -> FieldResult {
        guard let value else { return .invalid("Enter a step count.") }
        guard stepRange.contains(value) else {
            return .invalid("Steps must be between 0 and 100,000.")
        }
        return .valid
    }

    public static func dateOfBirth(
        _ date: Date?,
        on reference: Date = Date(),
        calendar: Calendar = DayCalendar.calendar()
    ) -> FieldResult {
        guard let date else { return .invalid("Pick your date of birth.") }
        if date > reference { return .invalid("Date of birth cannot be in the future.") }
        let age = HealthCalculator.age(dateOfBirth: date, on: reference, calendar: calendar)
        guard ageRange.contains(age) else {
            return .invalid("Age must be between 10 and 100 years.")
        }
        return .valid
    }

    public static func foodName(_ value: String) -> FieldResult {
        let trimmed = value.trimmingCharacters(in: .whitespacesAndNewlines)
        if trimmed.isEmpty { return .invalid("Enter a food name.") }
        if trimmed.count > 50 { return .invalid("Keep the food name under 50 characters.") }
        return .valid
    }

    public static func servingLabel(_ value: String) -> FieldResult {
        let trimmed = value.trimmingCharacters(in: .whitespacesAndNewlines)
        if trimmed.isEmpty { return .invalid("Describe the serving, e.g. \"1 bowl (150 g)\".") }
        if trimmed.count > 40 { return .invalid("Keep the serving under 40 characters.") }
        return .valid
    }

    public static func calories(_ value: Double?) -> FieldResult {
        guard let value else { return .invalid("Enter approximate calories.") }
        guard foodCalorieRange.contains(value) else {
            return .invalid("Calories must be between 0 and 5,000.")
        }
        return .valid
    }

    public static func protein(_ value: Double?) -> FieldResult {
        guard let value else { return .invalid("Enter approximate protein in grams.") }
        guard foodProteinRange.contains(value) else {
            return .invalid("Protein must be between 0 and 300 g.")
        }
        return .valid
    }

    public static func cost(_ value: Double?) -> FieldResult {
        guard let value else { return .invalid("Enter an approximate cost.") }
        guard foodCostRange.contains(value) else {
            return .invalid("Cost must be between ₹0 and ₹10,000.")
        }
        return .valid
    }

    public static func quantity(_ value: Double?) -> FieldResult {
        guard let value else { return .invalid("Enter a quantity.") }
        guard quantityRange.contains(value) else {
            return .invalid("Quantity must be between 0.25 and 20 servings.")
        }
        return .valid
    }

    public static func reminderTime(hour: Int?, minute: Int?) -> FieldResult {
        guard let hour, let minute else { return .invalid("Pick a time.") }
        guard (0...23).contains(hour), (0...59).contains(minute) else {
            return .invalid("Pick a valid time of day.")
        }
        return .valid
    }

    public static func reminderInterval(_ minutes: Int?) -> FieldResult {
        guard let minutes else { return .invalid("Pick a repeat interval.") }
        guard reminderIntervalRange.contains(minutes) else {
            return .invalid("Interval must be between 15 minutes and 12 hours.")
        }
        return .valid
    }

    /// Parses user text tolerantly: strips ₹, spaces and grouping separators.
    public static func parseDecimal(_ raw: String) -> Double? {
        let cleaned = raw
            .trimmingCharacters(in: .whitespacesAndNewlines)
            .replacingOccurrences(of: "₹", with: "")
            .replacingOccurrences(of: ",", with: "")
            .replacingOccurrences(of: " ", with: "")
        guard !cleaned.isEmpty else { return nil }
        return Double(cleaned)
    }

    public static func parseInt(_ raw: String) -> Int? {
        guard let value = parseDecimal(raw), value.isFinite else { return nil }
        return Int(value)
    }
}
