import Foundation

/// Builds one day's meal plan from the offline food database.
///
/// Priorities, in order:
///  1. respect the user's diet preference and food exclusions,
///  2. stay inside the daily rupee budget,
///  3. get as close as possible to the estimated calorie target,
///  4. prefer the cheapest protein per rupee.
///
/// Pure and deterministic for a given day, so re-opening the app never reshuffles today's plan
/// while different days still get some variety.
public enum MealPlanGenerator {

    public struct Request: Equatable, Sendable {
        public let day: DayKey
        public let calorieTarget: Double
        public let dailyBudget: Double
        public let preference: DietPreference
        public let excludedKeys: Set<String>
        public let mealTimes: [MealType: Int]

        public init(
            day: DayKey,
            calorieTarget: Double,
            dailyBudget: Double,
            preference: DietPreference,
            excludedKeys: Set<String> = [],
            mealTimes: [MealType: Int] = [:]
        ) {
            self.day = day
            self.calorieTarget = calorieTarget
            self.dailyBudget = dailyBudget
            self.preference = preference
            self.excludedKeys = excludedKeys
            self.mealTimes = mealTimes
        }
    }

    /// Roles each meal is built from, in plate order.
    private static let composition: [MealType: [FoodRole]] = [
        .breakfast: [.staple, .protein, .dairy],
        .lunch: [.staple, .protein, .vegetable],
        .snack: [.snack, .fruit],
        .dinner: [.staple, .protein, .vegetable]
    ]

    /// Items in these roles are dropped first when the day does not fit the budget.
    private static let optionalRoles: Set<FoodRole> = [.fruit, .dairy, .beverage, .snack]

    private static let maxStapleServings: Double = 5

    public static func generate(request: Request, foods: [FoodItem]) -> [PlannedMealItem] {
        let available = foods.filter { food in
            food.category.allowed(for: request.preference)
                && !request.excludedKeys.contains(food.nameKey)
        }
        guard !available.isEmpty else { return [] }

        var byRole: [FoodRole: [FoodItem]] = [:]
        for food in available {
            byRole[food.role, default: []].append(food)
        }

        var draft: [DraftItem] = []
        for mealType in MealType.allCases {
            let mealCalories = request.calorieTarget * mealType.calorieShare
            let mealBudget = request.dailyBudget * mealType.budgetShare
            draft += buildMeal(
                mealType: mealType,
                roles: composition[mealType] ?? [.staple, .protein],
                byRole: byRole,
                fallback: available,
                calorieTarget: mealCalories,
                budget: mealBudget,
                seed: request.day.value + mealType.sortIndex * 7
            )
        }

        trimToBudget(&draft, dailyBudget: request.dailyBudget)

        return draft.enumerated().map { index, item in
            let minutes = request.mealTimes[item.mealType] ?? item.mealType.defaultMinutesOfDay
            return PlannedMealItem(
                day: request.day,
                mealType: item.mealType,
                mealLabel: item.mealType.label,
                minutesOfDay: minutes,
                foodNameKey: item.food.nameKey,
                foodName: item.food.name,
                servingLabel: item.food.servingLabel,
                quantity: item.quantity,
                caloriesPerServing: item.food.calories,
                proteinPerServing: item.food.proteinG,
                costPerServing: item.food.costRupees,
                sortOrder: index
            )
        }
    }

    private struct DraftItem {
        let mealType: MealType
        let food: FoodItem
        var quantity: Double

        var calories: Double { food.calories * quantity }
        var cost: Double { food.costRupees * quantity }
    }

    private static func buildMeal(
        mealType: MealType,
        roles: [FoodRole],
        byRole: [FoodRole: [FoodItem]],
        fallback: [FoodItem],
        calorieTarget: Double,
        budget: Double,
        seed: Int
    ) -> [DraftItem] {
        var items: [DraftItem] = []
        var used: Set<String> = []

        for (slot, role) in roles.enumerated() {
            var candidates = (byRole[role] ?? []).filter { !used.contains($0.nameKey) }
            if candidates.isEmpty {
                candidates = fallback.filter { !used.contains($0.nameKey) }
            }
            guard !candidates.isEmpty else { continue }

            let ordered: [FoodItem]
            switch role {
            case .protein, .dairy:
                // Cheapest protein first.
                ordered = candidates.sorted { lhs, rhs in
                    lhs.proteinPerRupee == rhs.proteinPerRupee
                        ? lhs.nameKey < rhs.nameKey
                        : lhs.proteinPerRupee > rhs.proteinPerRupee
                }
            default:
                ordered = candidates.sorted { lhs, rhs in
                    lhs.costRupees == rhs.costRupees
                        ? lhs.nameKey < rhs.nameKey
                        : lhs.costRupees < rhs.costRupees
                }
            }

            // Rotate through the best half so days differ but stay affordable.
            let poolSize = max(3, ordered.count / 2)
            let pool = Array(ordered.prefix(poolSize))
            let index = modulo(seed + slot * 13, pool.count)
            let picked = pool[index]
            used.insert(picked.nameKey)

            let startQuantity: Double = (role == .staple && mealType != .breakfast) ? 2 : 1
            items.append(DraftItem(mealType: mealType, food: picked, quantity: startQuantity))
        }

        guard !items.isEmpty else { return items }
        balance(&items, calorieTarget: calorieTarget, budget: budget)
        return items
    }

    /// Nudges the staple (or largest) item so the meal lands near its calorie and cost slot.
    private static func balance(_ items: inout [DraftItem], calorieTarget: Double, budget: Double) {
        let flexibleIndex = items.firstIndex { $0.food.role == .staple }
            ?? items.indices.max(by: { items[$0].calories < items[$1].calories })
        guard let flexibleIndex, items[flexibleIndex].food.calories > 0 else { return }

        var guardCount = 0
        while guardCount < 20 {
            guardCount += 1
            let calories = items.reduce(0) { $0 + $1.calories }
            let cost = items.reduce(0) { $0 + $1.cost }
            let stapleCost = items[flexibleIndex].food.costRupees

            if calories < calorieTarget * 0.9,
               items[flexibleIndex].quantity + 0.5 <= maxStapleServings,
               cost + stapleCost * 0.5 <= budget * 1.1 {
                items[flexibleIndex].quantity += 0.5
            } else if calories > calorieTarget * 1.15, items[flexibleIndex].quantity - 0.5 >= 0.5 {
                items[flexibleIndex].quantity -= 0.5
            } else if cost > budget * 1.15, items[flexibleIndex].quantity - 0.5 >= 0.5 {
                items[flexibleIndex].quantity -= 0.5
            } else {
                return
            }
        }
    }

    /// Last pass over the whole day: drop optional extras, then shave staple servings, until the
    /// plan fits the daily budget. Never removes the last item of a meal.
    private static func trimToBudget(_ draft: inout [DraftItem], dailyBudget: Double) {
        guard dailyBudget > 0 else { return }

        var guardCount = 0
        while draft.reduce(0, { $0 + $1.cost }) > dailyBudget && guardCount < 40 {
            guardCount += 1

            let removable = draft.indices
                .filter { optionalRoles.contains(draft[$0].food.role) }
                .filter { index in
                    draft.filter { $0.mealType == draft[index].mealType }.count > 1
                }
                .max(by: { draft[$0].cost < draft[$1].cost })

            if let removable {
                draft.remove(at: removable)
                continue
            }

            let shrinkable = draft.indices
                .filter { draft[$0].quantity > 0.5 }
                .max(by: { draft[$0].food.costRupees < draft[$1].food.costRupees })

            guard let shrinkable else { break }
            draft[shrinkable].quantity -= 0.5
        }
    }

    /// Always-positive modulo, so a negative seed cannot crash the picker.
    private static func modulo(_ value: Int, _ count: Int) -> Int {
        guard count > 0 else { return 0 }
        let remainder = value % count
        return remainder < 0 ? remainder + count : remainder
    }
}
