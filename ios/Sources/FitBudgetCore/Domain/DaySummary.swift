import Foundation

/// Everything the app knows about one calendar day, assembled from real logged data only.
/// Checklists, streaks and reports are all derived from this - nothing is invented.
public struct DaySummary: Equatable, Sendable {
    public let day: DayKey
    public let plannedMealTypes: Set<MealType>
    public let completedMealTypes: Set<MealType>
    public let mealItemsPlanned: Int
    public let mealItemsCompleted: Int
    public let caloriesPlanned: Double
    public let caloriesConsumed: Double
    public let proteinConsumed: Double
    public let plannedCost: Double
    public let spent: Double
    public let budget: Double
    public let waterMl: Int
    public let waterTargetMl: Int
    public let steps: Int
    public let stepGoal: Int
    public let workoutsCompleted: Int
    public let weightKg: Double?

    public init(
        day: DayKey,
        plannedMealTypes: Set<MealType> = [],
        completedMealTypes: Set<MealType> = [],
        mealItemsPlanned: Int = 0,
        mealItemsCompleted: Int = 0,
        caloriesPlanned: Double = 0,
        caloriesConsumed: Double = 0,
        proteinConsumed: Double = 0,
        plannedCost: Double = 0,
        spent: Double = 0,
        budget: Double = 0,
        waterMl: Int = 0,
        waterTargetMl: Int = 0,
        steps: Int = 0,
        stepGoal: Int = 0,
        workoutsCompleted: Int = 0,
        weightKg: Double? = nil
    ) {
        self.day = day
        self.plannedMealTypes = plannedMealTypes
        self.completedMealTypes = completedMealTypes
        self.mealItemsPlanned = mealItemsPlanned
        self.mealItemsCompleted = mealItemsCompleted
        self.caloriesPlanned = caloriesPlanned
        self.caloriesConsumed = caloriesConsumed
        self.proteinConsumed = proteinConsumed
        self.plannedCost = plannedCost
        self.spent = spent
        self.budget = budget
        self.waterMl = waterMl
        self.waterTargetMl = waterTargetMl
        self.steps = steps
        self.stepGoal = stepGoal
        self.workoutsCompleted = workoutsCompleted
        self.weightKg = weightKg
    }

    public var weightLogged: Bool { weightKg != nil }

    /// Diet is "done" when every meal planned for the day is fully ticked off.
    public var dietGoalMet: Bool {
        !plannedMealTypes.isEmpty && plannedMealTypes.isSubset(of: completedMealTypes)
    }

    public var waterGoalMet: Bool { waterTargetMl > 0 && waterMl >= waterTargetMl }

    public var stepGoalMet: Bool { stepGoal > 0 && steps >= stepGoal }

    public var workoutGoalMet: Bool { workoutsCompleted > 0 }

    /// A budget day only counts when spending was actually tracked, otherwise an empty day would
    /// silently extend the budget streak.
    public var budgetGoalMet: Bool { budget > 0 && spent > 0 && spent <= budget }

    public var remainingBudget: Double { budget - spent }

    public var isOverBudget: Bool { budget > 0 && spent > budget }

    public var hasAnyActivity: Bool {
        mealItemsCompleted > 0 || waterMl > 0 || steps > 0
            || workoutsCompleted > 0 || weightLogged || spent > 0
    }
}

/// One row of the daily checklist.
public struct ChecklistItem: Identifiable, Equatable, Sendable {
    public let id: String
    public let label: String
    public let done: Bool
    public let detail: String?
    public let route: String?

    public init(id: String, label: String, done: Bool, detail: String? = nil, route: String? = nil) {
        self.id = id
        self.label = label
        self.done = done
        self.detail = detail
        self.route = route
    }
}

public enum DailyChecklist {

    public static let keyBreakfast = "breakfast"
    public static let keyLunch = "lunch"
    public static let keySnack = "snack"
    public static let keyDinner = "dinner"
    public static let keyWater = "water"
    public static let keyWorkout = "workout"
    public static let keySteps = "steps"
    public static let keyWeight = "weight"

    public static func build(_ summary: DaySummary) -> [ChecklistItem] {
        func mealItem(_ id: String, _ type: MealType) -> ChecklistItem {
            let planned = summary.plannedMealTypes.contains(type)
            return ChecklistItem(
                id: id,
                label: type.label,
                done: summary.completedMealTypes.contains(type),
                detail: planned ? nil : "Nothing planned",
                route: "diet"
            )
        }

        return [
            mealItem(keyBreakfast, .breakfast),
            mealItem(keyLunch, .lunch),
            mealItem(keySnack, .snack),
            mealItem(keyDinner, .dinner),
            ChecklistItem(
                id: keyWater,
                label: "Water goal",
                done: summary.waterGoalMet,
                detail: "\(summary.waterMl) / \(summary.waterTargetMl) ml",
                route: "water"
            ),
            ChecklistItem(
                id: keyWorkout,
                label: "Workout",
                done: summary.workoutGoalMet,
                detail: summary.workoutsCompleted > 0 ? "\(summary.workoutsCompleted) finished" : nil,
                route: "workout"
            ),
            ChecklistItem(
                id: keySteps,
                label: "Steps",
                done: summary.stepGoalMet,
                detail: "\(summary.steps) / \(summary.stepGoal)",
                route: "steps"
            ),
            ChecklistItem(
                id: keyWeight,
                label: "Weight logged",
                done: summary.weightLogged,
                detail: summary.weightKg.map { String(format: "%.1f kg", $0) },
                route: "weight"
            )
        ]
    }

    /// Completion as a 0...1 fraction of the eight checklist rows.
    public static func completionFraction(_ items: [ChecklistItem]) -> Double {
        guard !items.isEmpty else { return 0 }
        return Double(items.filter(\.done).count) / Double(items.count)
    }

    public static func completionPercent(_ summary: DaySummary) -> Int {
        Int(completionFraction(build(summary)) * 100)
    }
}
