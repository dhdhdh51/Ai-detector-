import Foundation
import SwiftData
import FitBudgetCore

// Every dated record is keyed by `day` (days since 1970-01-01 in the user's own calendar), exactly
// like the Android build, so the two apps agree on what "today" means and a new day inherently
// starts empty. Enums are stored as their stable raw strings.

// MARK: - Profile

@Model
final class ProfileRecord {
    var singletonID: Int = 1
    var name: String = ""
    var dobDay: Int = ProfileRecord.defaultDobDay
    var genderRaw: String = Gender.male.rawValue
    var heightCm: Double = 172
    var startWeightKg: Double = 85
    var currentWeightKg: Double = 85
    var targetWeightKg: Double = 75
    var dailyBudget: Double = 100
    var activityRaw: String = ActivityLevel.light.rawValue
    var dietRaw: String = DietPreference.eggetarian.rawValue
    var wakeMinutes: Int = 6 * 60 + 30
    var sleepMinutes: Int = 22 * 60 + 30
    /// Manual calorie override; nil means "use the estimate".
    var calorieTargetOverride: Double?
    var onboardingComplete: Bool = false
    var createdAt: Date = Date()
    var updatedAt: Date = Date()

    init() {}

    /// Example value pre-filled on the onboarding form; the user can change everything.
    static var defaultDobDay: Int {
        let calendar = DayCalendar.calendar()
        var components = DateComponents()
        components.year = 2006
        components.month = 11
        components.day = 22
        let date = calendar.date(from: components) ?? Date()
        return DayCalendar.dayKey(for: date, calendar: calendar).value
    }

    var gender: Gender {
        get { Gender.from(genderRaw) }
        set { genderRaw = newValue.rawValue }
    }

    var activityLevel: ActivityLevel {
        get { ActivityLevel.from(activityRaw) }
        set { activityRaw = newValue.rawValue }
    }

    var dietPreference: DietPreference {
        get { DietPreference.from(dietRaw) }
        set { dietRaw = newValue.rawValue }
    }

    var dateOfBirth: Date { DayCalendar.date(from: DayKey(dobDay)) }

    var age: Int { HealthCalculator.age(dateOfBirth: dateOfBirth) }

    var bmi: Double { HealthCalculator.bmi(weightKg: currentWeightKg, heightCm: heightCm) }

    var bmiCategory: String { HealthCalculator.bmiCategory(bmi) }

    var remainingWeightKg: Double {
        HealthCalculator.weightDifference(currentKg: currentWeightKg, targetKg: targetWeightKg)
    }

    var progressPercent: Double {
        HealthCalculator.progressPercent(
            startKg: startWeightKg,
            currentKg: currentWeightKg,
            targetKg: targetWeightKg
        )
    }

    var isLosingWeight: Bool { currentWeightKg > targetWeightKg }

    var bmr: Double {
        HealthCalculator.bmr(weightKg: currentWeightKg, heightCm: heightCm, age: age, gender: gender)
    }

    var tdee: Double { HealthCalculator.tdee(bmr: bmr, activityLevel: activityLevel) }

    var estimatedCalorieTarget: Double {
        calorieTargetOverride
            ?? HealthCalculator.calorieTarget(tdee: tdee, gender: gender, losingWeight: isLosingWeight)
    }

    var proteinTargetGrams: Double { HealthCalculator.proteinTargetGrams(weightKg: currentWeightKg) }

    var healthyWeightRange: ClosedRange<Double> {
        HealthCalculator.healthyWeightRange(heightCm: heightCm)
    }
}

// MARK: - Food database

@Model
final class FoodRecord {
    /// Lower-cased trimmed name; uniqueness is enforced in `AppStore` so duplicates are a handled
    /// error rather than a crash.
    var nameKey: String = ""
    var name: String = ""
    var servingLabel: String = ""
    var calories: Double = 0
    var proteinG: Double = 0
    var carbsG: Double = 0
    var fatG: Double = 0
    var costRupees: Double = 0
    var categoryRaw: String = FoodCategory.veg.rawValue
    var roleRaw: String = FoodRole.snack.rawValue
    var isCustom: Bool = false
    var createdAt: Date = Date()

    init(item: FoodItem) {
        self.nameKey = item.nameKey
        self.name = item.name
        self.servingLabel = item.servingLabel
        self.calories = item.calories
        self.proteinG = item.proteinG
        self.carbsG = item.carbsG
        self.fatG = item.fatG
        self.costRupees = item.costRupees
        self.categoryRaw = item.category.rawValue
        self.roleRaw = item.role.rawValue
        self.isCustom = item.isCustom
        self.createdAt = Date()
    }

    var category: FoodCategory {
        get { FoodCategory.from(categoryRaw) }
        set { categoryRaw = newValue.rawValue }
    }

    var role: FoodRole {
        get { FoodRole.from(roleRaw) }
        set { roleRaw = newValue.rawValue }
    }

    var item: FoodItem {
        FoodItem(
            name: name,
            servingLabel: servingLabel,
            calories: calories,
            proteinG: proteinG,
            carbsG: carbsG,
            fatG: fatG,
            costRupees: costRupees,
            category: category,
            role: role,
            isCustom: isCustom
        )
    }

    var proteinPerRupee: Double { costRupees <= 0 ? proteinG : proteinG / costRupees }
}

// MARK: - Meals

@Model
final class MealEntryRecord {
    var day: Int = 0
    /// Nil for a user-created custom meal; the four standard meals use the enum raw value.
    var mealTypeRaw: String?
    var mealLabel: String = ""
    var minutesOfDay: Int = 0
    var foodNameKey: String = ""
    var foodName: String = ""
    var servingLabel: String = ""
    var quantity: Double = 1
    var caloriesPerServing: Double = 0
    var proteinPerServing: Double = 0
    var costPerServing: Double = 0
    var completed: Bool = false
    var completedAt: Date?
    var sortOrder: Int = 0

    init(
        day: Int,
        mealType: MealType?,
        mealLabel: String,
        minutesOfDay: Int,
        foodNameKey: String,
        foodName: String,
        servingLabel: String,
        quantity: Double,
        caloriesPerServing: Double,
        proteinPerServing: Double,
        costPerServing: Double,
        completed: Bool = false,
        sortOrder: Int = 0
    ) {
        self.day = day
        self.mealTypeRaw = mealType?.rawValue
        self.mealLabel = mealLabel
        self.minutesOfDay = minutesOfDay
        self.foodNameKey = foodNameKey
        self.foodName = foodName
        self.servingLabel = servingLabel
        self.quantity = quantity
        self.caloriesPerServing = caloriesPerServing
        self.proteinPerServing = proteinPerServing
        self.costPerServing = costPerServing
        self.completed = completed
        self.sortOrder = sortOrder
    }

    convenience init(planned: PlannedMealItem) {
        self.init(
            day: planned.day.value,
            mealType: planned.mealType,
            mealLabel: planned.mealLabel,
            minutesOfDay: planned.minutesOfDay,
            foodNameKey: planned.foodNameKey,
            foodName: planned.foodName,
            servingLabel: planned.servingLabel,
            quantity: planned.quantity,
            caloriesPerServing: planned.caloriesPerServing,
            proteinPerServing: planned.proteinPerServing,
            costPerServing: planned.costPerServing,
            sortOrder: planned.sortOrder
        )
    }

    var mealType: MealType? {
        get { MealType.from(mealTypeRaw) }
        set { mealTypeRaw = newValue?.rawValue }
    }

    var totalCalories: Double { caloriesPerServing * quantity }
    var totalProtein: Double { proteinPerServing * quantity }
    var totalCost: Double { costPerServing * quantity }

    /// Stable grouping key so custom meals sit in their own section.
    var groupKey: String { mealTypeRaw ?? "CUSTOM:\(mealLabel)" }
}

// MARK: - Logs

@Model
final class WeightLogRecord {
    /// One entry per calendar day; `AppStore` replaces an existing row for the same day.
    var day: Int = 0
    var weightKg: Double = 0
    var note: String?
    var createdAt: Date = Date()

    init(day: Int, weightKg: Double, note: String? = nil) {
        self.day = day
        self.weightKg = weightKg
        self.note = note
        self.createdAt = Date()
    }
}

@Model
final class WaterLogRecord {
    var day: Int = 0
    var amountMl: Int = 0
    var loggedAt: Date = Date()

    init(day: Int, amountMl: Int, loggedAt: Date = Date()) {
        self.day = day
        self.amountMl = amountMl
        self.loggedAt = loggedAt
    }
}

@Model
final class StepLogRecord {
    /// One row per day. Motion-sensor steps and manual corrections are kept apart so a manual
    /// entry never silently wipes sensor-counted steps.
    var day: Int = 0
    var sensorSteps: Int = 0
    var manualSteps: Int = 0
    var updatedAt: Date = Date()

    init(day: Int, sensorSteps: Int = 0, manualSteps: Int = 0) {
        self.day = day
        self.sensorSteps = sensorSteps
        self.manualSteps = manualSteps
        self.updatedAt = Date()
    }

    var totalSteps: Int { sensorSteps + manualSteps }

    var source: StepSource {
        if sensorSteps > 0 && manualSteps > 0 { return .mixed }
        if manualSteps > 0 { return .manual }
        return .sensor
    }
}

@Model
final class WorkoutSessionRecord {
    var day: Int = 0
    var templateID: String = ""
    var templateName: String = ""
    var categoryRaw: String = WorkoutCategory.fullBody.rawValue
    var startedAt: Date = Date()
    var finishedAt: Date = Date()
    var durationSeconds: Int = 0
    var exercisesCompleted: Int = 0
    var exercisesSkipped: Int = 0
    var exercisesTotal: Int = 0
    /// Rough energy estimate, always labelled as an estimate in the UI.
    var estimatedCalories: Int = 0
    var note: String?

    init(
        day: Int,
        templateID: String,
        templateName: String,
        category: WorkoutCategory,
        startedAt: Date,
        finishedAt: Date,
        durationSeconds: Int,
        exercisesCompleted: Int,
        exercisesSkipped: Int,
        exercisesTotal: Int,
        estimatedCalories: Int,
        note: String? = nil
    ) {
        self.day = day
        self.templateID = templateID
        self.templateName = templateName
        self.categoryRaw = category.rawValue
        self.startedAt = startedAt
        self.finishedAt = finishedAt
        self.durationSeconds = durationSeconds
        self.exercisesCompleted = exercisesCompleted
        self.exercisesSkipped = exercisesSkipped
        self.exercisesTotal = exercisesTotal
        self.estimatedCalories = estimatedCalories
        self.note = note
    }

    var category: WorkoutCategory {
        get { WorkoutCategory.from(categoryRaw) }
        set { categoryRaw = newValue.rawValue }
    }

    var completionFraction: Double {
        guard exercisesTotal > 0 else { return 0 }
        return min(max(Double(exercisesCompleted) / Double(exercisesTotal), 0), 1)
    }
}

@Model
final class ExpenseRecord {
    var day: Int = 0
    var label: String = ""
    var amount: Double = 0
    var createdAt: Date = Date()

    init(day: Int, label: String, amount: Double) {
        self.day = day
        self.label = label
        self.amount = amount
        self.createdAt = Date()
    }
}

// MARK: - Reminders

@Model
final class ReminderRecord {
    var kindRaw: String = ReminderKind.breakfast.rawValue
    var enabled: Bool = true
    var hour: Int = 8
    var minute: Int = 0
    /// Bit 0 = Monday … bit 6 = Sunday.
    var daysMask: Int = WeekdayMask.all
    /// Repeat interval for interval reminders (water). 0 for one-shot daily reminders.
    var intervalMinutes: Int = 0
    var soundEnabled: Bool = true
    /// Kept for backup parity with the Android build; iOS controls vibration at system level.
    var vibrationEnabled: Bool = true

    init(kind: ReminderKind) {
        self.kindRaw = kind.rawValue
        self.enabled = true
        self.hour = kind.defaultHour
        self.minute = kind.defaultMinute
        self.daysMask = WeekdayMask.all
        self.intervalMinutes = kind.defaultIntervalMinutes
        self.soundEnabled = true
        self.vibrationEnabled = true
    }

    var kind: ReminderKind {
        get { ReminderKind.from(kindRaw) }
        set { kindRaw = newValue.rawValue }
    }

    var minutesOfDay: Int { hour * 60 + minute }
}

// MARK: - Per-day goal snapshot

@Model
final class DayMetaRecord {
    /// Snapshot of the goals that applied on a given day, written once when the day is first
    /// opened, so changing a goal later never rewrites history.
    var day: Int = 0
    var budget: Double = 0
    var waterTargetMl: Int = 0
    var stepGoal: Int = 0
    var calorieTarget: Double = 0
    var planGenerated: Bool = false
    var createdAt: Date = Date()

    init(
        day: Int,
        budget: Double,
        waterTargetMl: Int,
        stepGoal: Int,
        calorieTarget: Double,
        planGenerated: Bool = false
    ) {
        self.day = day
        self.budget = budget
        self.waterTargetMl = waterTargetMl
        self.stepGoal = stepGoal
        self.calorieTarget = calorieTarget
        self.planGenerated = planGenerated
        self.createdAt = Date()
    }
}

// MARK: - Schema

enum FitBudgetSchema {
    /// Every model in the store. Adding a property to a model is a lightweight migration that
    /// SwiftData performs automatically; a destructive change would need a `VersionedSchema`.
    static let models: [any PersistentModel.Type] = [
        ProfileRecord.self,
        FoodRecord.self,
        MealEntryRecord.self,
        WeightLogRecord.self,
        WaterLogRecord.self,
        StepLogRecord.self,
        WorkoutSessionRecord.self,
        ExpenseRecord.self,
        ReminderRecord.self,
        DayMetaRecord.self
    ]
}
