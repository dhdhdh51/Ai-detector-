import Foundation

/// Every value produced here is an **estimate** and is labelled as such in the UI.
/// The app never presents a diagnosis or a guaranteed outcome.
public enum HealthCalculator {

    /// Calories in roughly 1 kg of body fat, the standard planning figure.
    public static let kcalPerKg: Double = 7_700

    public static let minSafeCaloriesMale: Double = 1_500
    public static let minSafeCaloriesFemale: Double = 1_200

    // MARK: - Age

    public static func age(
        dateOfBirth: Date,
        on reference: Date = Date(),
        calendar: Calendar = DayCalendar.calendar()
    ) -> Int {
        if dateOfBirth > reference { return 0 }
        let years = calendar.dateComponents([.year], from: dateOfBirth, to: reference).year ?? 0
        return max(years, 0)
    }

    // MARK: - BMI

    /// BMI = weight(kg) / height(m)². Returns 0 for impossible input instead of crashing.
    public static func bmi(weightKg: Double, heightCm: Double) -> Double {
        guard heightCm > 0, weightKg > 0 else { return 0 }
        let heightM = heightCm / 100
        return weightKg / (heightM * heightM)
    }

    public static func bmiCategory(_ bmi: Double) -> String {
        switch bmi {
        case ..<0.001: return "—"
        case ..<18.5: return "Underweight"
        case ..<25: return "Healthy range"
        case ..<30: return "Overweight"
        default: return "Obese range"
        }
    }

    /// Healthy-BMI weight span for a height, used to sanity-check a target weight.
    public static func healthyWeightRange(heightCm: Double) -> ClosedRange<Double> {
        guard heightCm > 0 else { return 0...0 }
        let heightM = heightCm / 100
        return (18.5 * heightM * heightM)...(24.9 * heightM * heightM)
    }

    // MARK: - Progress

    /// Remaining weight to the target. Negative means the target is already passed.
    public static func weightDifference(currentKg: Double, targetKg: Double) -> Double {
        currentKg - targetKg
    }

    /// Progress = (start - current) / (start - target) × 100, clamped to 0...100.
    public static func progressPercent(startKg: Double, currentKg: Double, targetKg: Double) -> Double {
        let span = startKg - targetKg
        if abs(span) < 0.001 {
            return currentKg <= targetKg ? 100 : 0
        }
        let done = (startKg - currentKg) / span * 100
        return min(max(done, 0), 100)
    }

    // MARK: - Energy

    /// Mifflin-St Jeor basal metabolic rate estimate.
    public static func bmr(weightKg: Double, heightCm: Double, age: Int, gender: Gender) -> Double {
        guard weightKg > 0, heightCm > 0 else { return 0 }
        let safeAge = Double(min(max(age, 10), 100))
        let base = 10 * weightKg + 6.25 * heightCm - 5 * safeAge
        let adjusted: Double
        switch gender {
        case .male: adjusted = base + 5
        case .female: adjusted = base - 161
        // Neutral midpoint so the estimate is never wildly off in either direction.
        case .other: adjusted = base - 78
        }
        return max(adjusted, 0)
    }

    /// Total daily energy expenditure estimate = BMR × activity factor.
    public static func tdee(bmr: Double, activityLevel: ActivityLevel) -> Double {
        bmr * activityLevel.factor
    }

    /// Suggested intake for a fat-loss goal: a moderate deficit off maintenance, never below a
    /// conservative floor. Returns maintenance when the user is at or below target.
    public static func calorieTarget(tdee: Double, gender: Gender, losingWeight: Bool) -> Double {
        guard tdee > 0 else { return 0 }
        guard losingWeight else { return roundToNearest(tdee, step: 10) }
        let floor: Double
        switch gender {
        case .female: floor = minSafeCaloriesFemale
        case .male: floor = minSafeCaloriesMale
        case .other: floor = (minSafeCaloriesMale + minSafeCaloriesFemale) / 2
        }
        let deficit = min(max(tdee * 0.20, 300), 700)
        return roundToNearest(max(tdee - deficit, floor), step: 10)
    }

    /// The deficit band shown in the UI, e.g. 300–550 kcal below maintenance.
    public static func deficitRange(tdee: Double) -> ClosedRange<Int> {
        guard tdee > 0 else { return 0...0 }
        let low = Int(min(max(tdee * 0.10, 200), 500).rounded())
        let high = Int(min(max(tdee * 0.22, 300), 750).rounded())
        return low...max(high, low)
    }

    /// kg/week implied by a calorie deficit.
    public static func weeklyWeightChangeKg(tdee: Double, intakeCalories: Double) -> Double {
        guard tdee > 0 else { return 0 }
        return (tdee - intakeCalories) * 7 / kcalPerKg
    }

    /// Weeks to reach the target at the current estimated pace. Nil when not applicable.
    public static func weeksToTarget(currentKg: Double, targetKg: Double, weeklyLossKg: Double) -> Int? {
        let remaining = currentKg - targetKg
        guard remaining > 0, weeklyLossKg > 0.01 else { return nil }
        return Int((remaining / weeklyLossKg).rounded(.up))
    }

    /// Protein target for a fat-loss phase, in grams.
    public static func proteinTargetGrams(weightKg: Double) -> Double {
        guard weightKg > 0 else { return 0 }
        return roundToNearest(weightKg * 1.4, step: 5)
    }

    /// Suggested daily water in ml (≈35 ml per kg), rounded to the nearest 100 ml and clamped.
    public static func suggestedWaterMl(weightKg: Double) -> Int {
        guard weightKg > 0 else { return 2_500 }
        let raw = weightKg * 35
        let rounded = Int((raw / 100).rounded()) * 100
        return min(max(rounded, 2_000), 4_000)
    }

    /// Average weekly change between the first and last log of a period.
    public static func averageWeeklyChangeKg(
        firstWeightKg: Double,
        lastWeightKg: Double,
        daysBetween: Int
    ) -> Double {
        guard daysBetween > 0 else { return 0 }
        return (lastWeightKg - firstWeightKg) / Double(daysBetween) * 7
    }

    private static func roundToNearest(_ value: Double, step: Int) -> Double {
        guard step > 0 else { return value }
        return (value / Double(step)).rounded() * Double(step)
    }
}
