import Foundation

/// Biological sex, used only to pick the right BMR formula.
public enum Gender: String, CaseIterable, Codable, Sendable {
    case male = "MALE"
    case female = "FEMALE"
    case other = "OTHER"

    public var label: String {
        switch self {
        case .male: return "Male"
        case .female: return "Female"
        case .other: return "Other"
        }
    }

    public static func from(_ raw: String?) -> Gender {
        guard let raw, let value = Gender(rawValue: raw) else { return .other }
        return value
    }
}

public enum ActivityLevel: String, CaseIterable, Codable, Sendable {
    case sedentary = "SEDENTARY"
    case light = "LIGHT"
    case moderate = "MODERATE"
    case veryActive = "VERY_ACTIVE"
    case extraActive = "EXTRA_ACTIVE"

    public var label: String {
        switch self {
        case .sedentary: return "Sedentary"
        case .light: return "Lightly active"
        case .moderate: return "Moderately active"
        case .veryActive: return "Very active"
        case .extraActive: return "Extra active"
        }
    }

    public var detail: String {
        switch self {
        case .sedentary: return "Desk job, very little movement"
        case .light: return "Light walking, 1–3 days a week"
        case .moderate: return "Exercise 3–5 days a week"
        case .veryActive: return "Hard exercise 6–7 days a week"
        case .extraActive: return "Physical job or twice-daily training"
        }
    }

    public var factor: Double {
        switch self {
        case .sedentary: return 1.2
        case .light: return 1.375
        case .moderate: return 1.55
        case .veryActive: return 1.725
        case .extraActive: return 1.9
        }
    }

    public static func from(_ raw: String?) -> ActivityLevel {
        guard let raw, let value = ActivityLevel(rawValue: raw) else { return .light }
        return value
    }
}

public enum DietPreference: String, CaseIterable, Codable, Sendable {
    case vegetarian = "VEGETARIAN"
    case eggetarian = "EGGETARIAN"
    case nonVegetarian = "NON_VEGETARIAN"

    public var label: String {
        switch self {
        case .vegetarian: return "Vegetarian"
        case .eggetarian: return "Eggetarian"
        case .nonVegetarian: return "Non-vegetarian"
        }
    }

    public static func from(_ raw: String?) -> DietPreference {
        guard let raw, let value = DietPreference(rawValue: raw) else { return .vegetarian }
        return value
    }
}

public enum FoodCategory: String, CaseIterable, Codable, Sendable {
    case veg = "VEG"
    case egg = "EGG"
    case nonVeg = "NON_VEG"

    public var label: String {
        switch self {
        case .veg: return "Veg"
        case .egg: return "Egg"
        case .nonVeg: return "Non-veg"
        }
    }

    public func allowed(for preference: DietPreference) -> Bool {
        switch preference {
        case .vegetarian: return self == .veg
        case .eggetarian: return self == .veg || self == .egg
        case .nonVegetarian: return true
        }
    }

    public static func from(_ raw: String?) -> FoodCategory {
        guard let raw, let value = FoodCategory(rawValue: raw) else { return .veg }
        return value
    }
}

/// Broad role a food plays on the plate, used to keep generated meals sensible.
public enum FoodRole: String, CaseIterable, Codable, Sendable {
    case protein = "PROTEIN"
    case staple = "STAPLE"
    case vegetable = "VEGETABLE"
    case fruit = "FRUIT"
    case dairy = "DAIRY"
    case snack = "SNACK"
    case beverage = "BEVERAGE"

    public var label: String {
        rawValue.lowercased().capitalizedFirst
    }

    public static func from(_ raw: String?) -> FoodRole {
        guard let raw, let value = FoodRole(rawValue: raw) else { return .snack }
        return value
    }
}

public enum MealType: String, CaseIterable, Codable, Sendable {
    case breakfast = "BREAKFAST"
    case lunch = "LUNCH"
    case snack = "SNACK"
    case dinner = "DINNER"

    public var label: String {
        switch self {
        case .breakfast: return "Breakfast"
        case .lunch: return "Lunch"
        case .snack: return "Evening Snack"
        case .dinner: return "Dinner"
        }
    }

    public var defaultHour: Int {
        switch self {
        case .breakfast: return 8
        case .lunch: return 13
        case .snack: return 17
        case .dinner: return 20
        }
    }

    public var defaultMinute: Int { 0 }

    /// Share of the daily calorie target this meal should roughly cover.
    public var calorieShare: Double {
        switch self {
        case .breakfast: return 0.28
        case .lunch: return 0.32
        case .snack: return 0.12
        case .dinner: return 0.28
        }
    }

    /// Share of the daily budget this meal may use.
    public var budgetShare: Double {
        switch self {
        case .breakfast: return 0.26
        case .lunch: return 0.32
        case .snack: return 0.12
        case .dinner: return 0.30
        }
    }

    public var defaultMinutesOfDay: Int { defaultHour * 60 + defaultMinute }

    public var sortIndex: Int {
        switch self {
        case .breakfast: return 0
        case .lunch: return 1
        case .snack: return 2
        case .dinner: return 3
        }
    }

    public static func from(_ raw: String?) -> MealType? {
        guard let raw else { return nil }
        return MealType(rawValue: raw)
    }
}

/// The nine configurable reminders.
public enum ReminderKind: String, CaseIterable, Codable, Sendable {
    case breakfast = "BREAKFAST"
    case lunch = "LUNCH"
    case snack = "SNACK"
    case dinner = "DINNER"
    case water = "WATER"
    case workout = "WORKOUT"
    case walking = "WALKING"
    case weight = "WEIGHT"
    case sleep = "SLEEP"

    public var label: String {
        switch self {
        case .breakfast: return "Breakfast"
        case .lunch: return "Lunch"
        case .snack: return "Evening snack"
        case .dinner: return "Dinner"
        case .water: return "Water"
        case .workout: return "Workout"
        case .walking: return "Walking"
        case .weight: return "Weight check"
        case .sleep: return "Sleep"
        }
    }

    public var notificationTitle: String {
        switch self {
        case .breakfast: return "🍳 Breakfast time"
        case .lunch: return "🍛 Lunch time"
        case .snack: return "🥜 Snack time"
        case .dinner: return "🍽️ Dinner time"
        case .water: return "💧 Water reminder"
        case .workout: return "🏋️ Workout time"
        case .walking: return "🏃 Walk time"
        case .weight: return "⚖️ Weight check"
        case .sleep: return "😴 Wind down"
        }
    }

    public var notificationBody: String {
        switch self {
        case .breakfast: return "Stay on track with your FitBudget plan."
        case .lunch: return "Eat your planned lunch and log it."
        case .snack: return "A light, high-protein snack keeps you full."
        case .dinner: return "Finish the day inside your budget."
        case .water: return "Have a glass of water."
        case .workout: return "30 minutes for yourself."
        case .walking: return "A brisk walk now would hit your step goal."
        case .weight: return "Today's measurement?"
        case .sleep: return "Sleep on time — recovery is part of the plan."
        }
    }

    public var defaultHour: Int {
        switch self {
        case .breakfast: return 8
        case .lunch: return 13
        case .snack: return 17
        case .dinner: return 20
        case .water: return 9
        case .workout: return 18
        case .walking: return 7
        case .weight: return 7
        case .sleep: return 22
        }
    }

    public var defaultMinute: Int { self == .sleep ? 30 : 0 }

    /// Non-zero only for interval reminders (water repeats every two hours by default).
    public var defaultIntervalMinutes: Int { self == .water ? 120 : 0 }

    public var isInterval: Bool { defaultIntervalMinutes > 0 }

    /// In-app destination the notification opens.
    public var route: String {
        switch self {
        case .breakfast, .lunch, .snack, .dinner: return "diet"
        case .water: return "water"
        case .workout: return "workout"
        case .walking: return "steps"
        case .weight: return "weight"
        case .sleep: return "home"
        }
    }

    public var mealType: MealType? {
        switch self {
        case .breakfast: return .breakfast
        case .lunch: return .lunch
        case .snack: return .snack
        case .dinner: return .dinner
        default: return nil
        }
    }

    public var sortIndex: Int {
        ReminderKind.allCases.firstIndex(of: self) ?? 0
    }

    public static func from(_ raw: String?) -> ReminderKind {
        guard let raw, let value = ReminderKind(rawValue: raw) else { return .breakfast }
        return value
    }
}

public enum ThemeMode: String, CaseIterable, Codable, Sendable {
    case light = "LIGHT"
    case dark = "DARK"
    case system = "SYSTEM"

    public var label: String {
        switch self {
        case .light: return "Light"
        case .dark: return "Dark"
        case .system: return "System default"
        }
    }

    public static func from(_ raw: String?) -> ThemeMode {
        guard let raw, let value = ThemeMode(rawValue: raw) else { return .system }
        return value
    }
}

public enum UnitSystem: String, CaseIterable, Codable, Sendable {
    case metric = "METRIC"
    case imperial = "IMPERIAL"

    public var label: String {
        switch self {
        case .metric: return "Metric (kg, cm)"
        case .imperial: return "Imperial (lb, ft/in)"
        }
    }

    public static func from(_ raw: String?) -> UnitSystem {
        guard let raw, let value = UnitSystem(rawValue: raw) else { return .metric }
        return value
    }
}

public enum WorkoutCategory: String, CaseIterable, Codable, Sendable {
    case fullBody = "FULL_BODY"
    case homeWorkout = "HOME_WORKOUT"
    case walking = "WALKING"
    case strength = "STRENGTH"
    case mobility = "MOBILITY"

    public var label: String {
        switch self {
        case .fullBody: return "Full Body"
        case .homeWorkout: return "Home Workout"
        case .walking: return "Walking"
        case .strength: return "Strength"
        case .mobility: return "Mobility"
        }
    }

    public var detail: String {
        switch self {
        case .fullBody: return "Balanced session for the whole body"
        case .homeWorkout: return "No equipment, small space"
        case .walking: return "Low impact cardio"
        case .strength: return "Build and keep muscle while losing fat"
        case .mobility: return "Loosen up stiff joints"
        }
    }

    public static func from(_ raw: String?) -> WorkoutCategory {
        guard let raw, let value = WorkoutCategory(rawValue: raw) else { return .fullBody }
        return value
    }
}

public enum StepSource: String, Codable, Sendable {
    case sensor = "SENSOR"
    case manual = "MANUAL"
    case mixed = "MIXED"

    public var label: String {
        switch self {
        case .sensor: return "Device motion sensor"
        case .manual: return "Manual entry"
        case .mixed: return "Sensor + manual"
        }
    }
}

extension String {
    var capitalizedFirst: String {
        guard let first else { return self }
        return String(first).uppercased() + dropFirst()
    }
}
