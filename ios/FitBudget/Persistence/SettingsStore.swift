import Foundation
import FitBudgetCore

/// User preferences that are not part of the health profile.
struct AppSettings: Equatable {
    var themeMode: ThemeMode = .system
    var unitSystem: UnitSystem = .metric
    var waterTargetMl: Int = WaterCalculator.defaultTargetMl
    var stepGoal: Int = AppSettings.defaultStepGoal
    /// Master switch; individual reminders have their own toggle too.
    var remindersEnabled: Bool = true
    var notificationSoundEnabled: Bool = true
    /// Lower-cased food names the user never wants suggested, e.g. "boiled egg".
    var excludedFoodKeys: Set<String> = []
    /// Last day the roll-over pass ran.
    var lastRollOverDay: Int = 0

    static let defaultStepGoal = 8_000
}

/// `UserDefaults`-backed settings. Reads fall back to defaults so the app always starts, and every
/// value is clamped to the range the validators accept.
@MainActor
final class SettingsStore {

    private enum Key {
        static let theme = "fitbudget.theme"
        static let units = "fitbudget.units"
        static let waterTarget = "fitbudget.waterTargetMl"
        static let stepGoal = "fitbudget.stepGoal"
        static let remindersEnabled = "fitbudget.remindersEnabled"
        static let sound = "fitbudget.notificationSound"
        static let excluded = "fitbudget.excludedFoods"
        static let lastRollOver = "fitbudget.lastRollOverDay"
    }

    private let defaults: UserDefaults

    init(defaults: UserDefaults = .standard) {
        self.defaults = defaults
    }

    func load() -> AppSettings {
        var settings = AppSettings()
        settings.themeMode = ThemeMode.from(defaults.string(forKey: Key.theme))
        settings.unitSystem = UnitSystem.from(defaults.string(forKey: Key.units))

        let water = defaults.object(forKey: Key.waterTarget) as? Int ?? WaterCalculator.defaultTargetMl
        settings.waterTargetMl = min(
            max(water, Validators.waterTargetRange.lowerBound),
            Validators.waterTargetRange.upperBound
        )

        let steps = defaults.object(forKey: Key.stepGoal) as? Int ?? AppSettings.defaultStepGoal
        settings.stepGoal = min(
            max(steps, Validators.stepRange.lowerBound),
            Validators.stepRange.upperBound
        )

        settings.remindersEnabled = defaults.object(forKey: Key.remindersEnabled) as? Bool ?? true
        settings.notificationSoundEnabled = defaults.object(forKey: Key.sound) as? Bool ?? true
        settings.excludedFoodKeys = Set(defaults.stringArray(forKey: Key.excluded) ?? [])
        settings.lastRollOverDay = defaults.object(forKey: Key.lastRollOver) as? Int ?? 0
        return settings
    }

    func save(_ settings: AppSettings) {
        defaults.set(settings.themeMode.rawValue, forKey: Key.theme)
        defaults.set(settings.unitSystem.rawValue, forKey: Key.units)
        defaults.set(settings.waterTargetMl, forKey: Key.waterTarget)
        defaults.set(settings.stepGoal, forKey: Key.stepGoal)
        defaults.set(settings.remindersEnabled, forKey: Key.remindersEnabled)
        defaults.set(settings.notificationSoundEnabled, forKey: Key.sound)
        defaults.set(Array(settings.excludedFoodKeys).sorted(), forKey: Key.excluded)
        defaults.set(settings.lastRollOverDay, forKey: Key.lastRollOver)
    }

    /// Wipes preferences back to defaults (used by "Reset all data").
    func clear() {
        for key in [
            Key.theme, Key.units, Key.waterTarget, Key.stepGoal,
            Key.remindersEnabled, Key.sound, Key.excluded, Key.lastRollOver
        ] {
            defaults.removeObject(forKey: key)
        }
    }
}
