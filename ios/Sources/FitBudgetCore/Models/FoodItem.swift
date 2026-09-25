import Foundation

/// A food as the domain layer sees it. The persistence layer maps its records onto this type, so
/// the meal-plan generator and every calculation stay free of SwiftData.
public struct FoodItem: Identifiable, Equatable, Sendable {
    /// Lower-cased trimmed name: the stable, unique identity of a food.
    public var id: String { nameKey }

    public let name: String
    public let nameKey: String
    public let servingLabel: String
    public let calories: Double
    public let proteinG: Double
    public let carbsG: Double
    public let fatG: Double
    /// Approximate market cost of one serving, in rupees.
    public let costRupees: Double
    public let category: FoodCategory
    public let role: FoodRole
    public let isCustom: Bool

    public init(
        name: String,
        servingLabel: String,
        calories: Double,
        proteinG: Double,
        carbsG: Double = 0,
        fatG: Double = 0,
        costRupees: Double,
        category: FoodCategory = .veg,
        role: FoodRole = .snack,
        isCustom: Bool = false
    ) {
        self.name = name
        self.nameKey = FoodItem.key(for: name)
        self.servingLabel = servingLabel
        self.calories = calories
        self.proteinG = proteinG
        self.carbsG = carbsG
        self.fatG = fatG
        self.costRupees = costRupees
        self.category = category
        self.role = role
        self.isCustom = isCustom
    }

    /// Grams of protein per rupee - the app's affordability ranking for protein sources.
    public var proteinPerRupee: Double {
        costRupees <= 0 ? proteinG : proteinG / costRupees
    }

    public static func key(for name: String) -> String {
        name.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
    }
}

/// One generated line of a day's plan. The persistence layer turns these into stored records.
public struct PlannedMealItem: Equatable, Sendable {
    public let day: DayKey
    /// Nil for a user-created custom meal; the four standard meals use the enum.
    public let mealType: MealType?
    public let mealLabel: String
    public let minutesOfDay: Int
    public let foodNameKey: String
    public let foodName: String
    public let servingLabel: String
    public let quantity: Double
    public let caloriesPerServing: Double
    public let proteinPerServing: Double
    public let costPerServing: Double
    public let sortOrder: Int

    public init(
        day: DayKey,
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
        sortOrder: Int
    ) {
        self.day = day
        self.mealType = mealType
        self.mealLabel = mealLabel
        self.minutesOfDay = minutesOfDay
        self.foodNameKey = foodNameKey
        self.foodName = foodName
        self.servingLabel = servingLabel
        self.quantity = quantity
        self.caloriesPerServing = caloriesPerServing
        self.proteinPerServing = proteinPerServing
        self.costPerServing = costPerServing
        self.sortOrder = sortOrder
    }

    public var totalCalories: Double { caloriesPerServing * quantity }
    public var totalProtein: Double { proteinPerServing * quantity }
    public var totalCost: Double { costPerServing * quantity }
}
