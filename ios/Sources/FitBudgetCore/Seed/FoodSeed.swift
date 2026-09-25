import Foundation

/// Offline food database seed: common, low-cost Indian foods.
///
/// Calories/protein are rounded reference values for a typical home-cooked serving and costs are
/// approximate Indian market prices. Everything here is an estimate and the user can edit any food
/// or add their own.
public enum FoodSeed {

    public static let items: [FoodItem] = affordableProtein + staples + dairy + vegetables + fruit + extras

    // MARK: - Affordable protein

    private static let affordableProtein: [FoodItem] = [
        FoodItem(name: "Boiled Egg", servingLabel: "1 egg (50 g)", calories: 78, proteinG: 6.3,
                 carbsG: 0.6, fatG: 5.3, costRupees: 7, category: .egg, role: .protein),
        FoodItem(name: "Egg Omelette", servingLabel: "2 eggs + onion", calories: 190, proteinG: 13,
                 carbsG: 3, fatG: 14, costRupees: 18, category: .egg, role: .protein),
        FoodItem(name: "Egg White", servingLabel: "2 egg whites", calories: 34, proteinG: 7.2,
                 carbsG: 0.5, fatG: 0.1, costRupees: 14, category: .egg, role: .protein),
        FoodItem(name: "Toor Dal (cooked)", servingLabel: "1 bowl (150 g)", calories: 150, proteinG: 9,
                 carbsG: 21, fatG: 3, costRupees: 12, category: .veg, role: .protein),
        FoodItem(name: "Masoor Dal (cooked)", servingLabel: "1 bowl (150 g)", calories: 140, proteinG: 9,
                 carbsG: 20, fatG: 2, costRupees: 11, category: .veg, role: .protein),
        FoodItem(name: "Moong Dal (cooked)", servingLabel: "1 bowl (150 g)", calories: 145, proteinG: 9.5,
                 carbsG: 20, fatG: 2, costRupees: 12, category: .veg, role: .protein),
        FoodItem(name: "Boiled Chana", servingLabel: "1 bowl (100 g)", calories: 164, proteinG: 8.9,
                 carbsG: 27, fatG: 2.6, costRupees: 14, category: .veg, role: .protein),
        FoodItem(name: "Roasted Chana", servingLabel: "30 g", calories: 110, proteinG: 6,
                 carbsG: 18, fatG: 1.5, costRupees: 5, category: .veg, role: .snack),
        FoodItem(name: "Soya Chunks (cooked)", servingLabel: "50 g dry", calories: 172, proteinG: 26,
                 carbsG: 16, fatG: 0.5, costRupees: 10, category: .veg, role: .protein),
        FoodItem(name: "Sprouted Moong", servingLabel: "1 bowl (100 g)", calories: 120, proteinG: 8,
                 carbsG: 19, fatG: 0.6, costRupees: 10, category: .veg, role: .protein),
        FoodItem(name: "Rajma (cooked)", servingLabel: "1 bowl (150 g)", calories: 180, proteinG: 10,
                 carbsG: 30, fatG: 1, costRupees: 16, category: .veg, role: .protein),
        FoodItem(name: "Paneer", servingLabel: "50 g", calories: 145, proteinG: 9,
                 carbsG: 2, fatG: 11, costRupees: 25, category: .veg, role: .protein),
        FoodItem(name: "Peanuts (roasted)", servingLabel: "30 g", calories: 170, proteinG: 7.6,
                 carbsG: 6, fatG: 14, costRupees: 6, category: .veg, role: .protein),
        FoodItem(name: "Sattu Drink", servingLabel: "30 g sattu", calories: 120, proteinG: 6,
                 carbsG: 20, fatG: 1.5, costRupees: 8, category: .veg, role: .beverage),
        FoodItem(name: "Chicken Breast", servingLabel: "100 g", calories: 165, proteinG: 31,
                 carbsG: 0, fatG: 3.6, costRupees: 40, category: .nonVeg, role: .protein),
        FoodItem(name: "Chicken Curry", servingLabel: "1 bowl (150 g)", calories: 240, proteinG: 22,
                 carbsG: 6, fatG: 14, costRupees: 60, category: .nonVeg, role: .protein),
        FoodItem(name: "Rohu Fish (curry cut)", servingLabel: "100 g", calories: 140, proteinG: 20,
                 carbsG: 0, fatG: 6, costRupees: 40, category: .nonVeg, role: .protein)
    ]

    // MARK: - Staples

    private static let staples: [FoodItem] = [
        FoodItem(name: "Roti (wheat)", servingLabel: "1 roti (40 g atta)", calories: 110, proteinG: 3.2,
                 carbsG: 22, fatG: 1, costRupees: 3, category: .veg, role: .staple),
        FoodItem(name: "Rice (cooked)", servingLabel: "1 cup (150 g)", calories: 200, proteinG: 4,
                 carbsG: 44, fatG: 0.4, costRupees: 6, category: .veg, role: .staple),
        FoodItem(name: "Poha", servingLabel: "1 plate (150 g)", calories: 250, proteinG: 5,
                 carbsG: 45, fatG: 6, costRupees: 12, category: .veg, role: .staple),
        FoodItem(name: "Oats (cooked in water)", servingLabel: "40 g dry", calories: 150, proteinG: 5,
                 carbsG: 27, fatG: 3, costRupees: 10, category: .veg, role: .staple),
        FoodItem(name: "Upma", servingLabel: "1 plate (200 g)", calories: 230, proteinG: 6,
                 carbsG: 38, fatG: 6, costRupees: 12, category: .veg, role: .staple),
        FoodItem(name: "Idli", servingLabel: "2 pieces", calories: 140, proteinG: 4,
                 carbsG: 28, fatG: 0.6, costRupees: 12, category: .veg, role: .staple),
        FoodItem(name: "Plain Dosa", servingLabel: "1 dosa", calories: 160, proteinG: 4,
                 carbsG: 26, fatG: 4, costRupees: 15, category: .veg, role: .staple),
        FoodItem(name: "Whole Wheat Bread", servingLabel: "2 slices", calories: 140, proteinG: 5,
                 carbsG: 26, fatG: 2, costRupees: 10, category: .veg, role: .staple),
        FoodItem(name: "Daliya (broken wheat)", servingLabel: "50 g dry", calories: 170, proteinG: 6,
                 carbsG: 34, fatG: 1, costRupees: 8, category: .veg, role: .staple)
    ]

    // MARK: - Dairy

    private static let dairy: [FoodItem] = [
        FoodItem(name: "Toned Milk", servingLabel: "1 glass (200 ml)", calories: 120, proteinG: 6.4,
                 carbsG: 10, fatG: 5, costRupees: 12, category: .veg, role: .dairy),
        FoodItem(name: "Curd (dahi)", servingLabel: "1 bowl (150 g)", calories: 90, proteinG: 5,
                 carbsG: 7, fatG: 4.5, costRupees: 12, category: .veg, role: .dairy),
        FoodItem(name: "Buttermilk (chaas)", servingLabel: "1 glass (250 ml)", calories: 40, proteinG: 2,
                 carbsG: 4, fatG: 1, costRupees: 8, category: .veg, role: .beverage),
        FoodItem(name: "Ghee", servingLabel: "1 tsp (5 g)", calories: 45, proteinG: 0,
                 carbsG: 0, fatG: 5, costRupees: 6, category: .veg, role: .snack)
    ]

    // MARK: - Vegetables

    private static let vegetables: [FoodItem] = [
        FoodItem(name: "Seasonal Mixed Vegetables", servingLabel: "1 bowl (150 g)", calories: 90,
                 proteinG: 3, carbsG: 14, fatG: 2, costRupees: 12, category: .veg, role: .vegetable),
        FoodItem(name: "Boiled Potato", servingLabel: "1 medium (150 g)", calories: 130, proteinG: 3,
                 carbsG: 30, fatG: 0.2, costRupees: 6, category: .veg, role: .vegetable),
        FoodItem(name: "Sweet Potato", servingLabel: "1 medium (150 g)", calories: 130, proteinG: 2,
                 carbsG: 30, fatG: 0.1, costRupees: 12, category: .veg, role: .vegetable),
        FoodItem(name: "Tomato", servingLabel: "1 medium (100 g)", calories: 22, proteinG: 1,
                 carbsG: 4, fatG: 0.2, costRupees: 4, category: .veg, role: .vegetable),
        FoodItem(name: "Onion", servingLabel: "1 medium (100 g)", calories: 44, proteinG: 1.2,
                 carbsG: 10, fatG: 0.1, costRupees: 4, category: .veg, role: .vegetable),
        FoodItem(name: "Palak (spinach, cooked)", servingLabel: "1 bowl (150 g)", calories: 45,
                 proteinG: 3.4, carbsG: 5, fatG: 0.5, costRupees: 8, category: .veg, role: .vegetable),
        FoodItem(name: "Cabbage Sabzi", servingLabel: "1 bowl (150 g)", calories: 70, proteinG: 2,
                 carbsG: 10, fatG: 2.5, costRupees: 8, category: .veg, role: .vegetable),
        FoodItem(name: "Lauki (bottle gourd) Sabzi", servingLabel: "1 bowl (150 g)", calories: 65,
                 proteinG: 1.5, carbsG: 9, fatG: 2.5, costRupees: 8, category: .veg, role: .vegetable),
        FoodItem(name: "Carrot", servingLabel: "1 medium (80 g)", calories: 25, proteinG: 0.6,
                 carbsG: 6, fatG: 0.1, costRupees: 4, category: .veg, role: .vegetable),
        FoodItem(name: "Cucumber", servingLabel: "1 medium (150 g)", calories: 30, proteinG: 1,
                 carbsG: 5, fatG: 0.2, costRupees: 5, category: .veg, role: .vegetable),
        FoodItem(name: "Green Salad", servingLabel: "1 plate", calories: 50, proteinG: 2,
                 carbsG: 8, fatG: 0.5, costRupees: 10, category: .veg, role: .vegetable)
    ]

    // MARK: - Fruit

    private static let fruit: [FoodItem] = [
        FoodItem(name: "Banana", servingLabel: "1 medium (118 g)", calories: 105, proteinG: 1.3,
                 carbsG: 27, fatG: 0.4, costRupees: 5, category: .veg, role: .fruit),
        FoodItem(name: "Apple", servingLabel: "1 medium (180 g)", calories: 95, proteinG: 0.5,
                 carbsG: 25, fatG: 0.3, costRupees: 18, category: .veg, role: .fruit),
        FoodItem(name: "Guava", servingLabel: "1 medium (120 g)", calories: 65, proteinG: 2.6,
                 carbsG: 14, fatG: 0.9, costRupees: 10, category: .veg, role: .fruit),
        FoodItem(name: "Papaya", servingLabel: "1 bowl (150 g)", calories: 60, proteinG: 0.9,
                 carbsG: 15, fatG: 0.2, costRupees: 12, category: .veg, role: .fruit),
        FoodItem(name: "Orange", servingLabel: "1 medium (150 g)", calories: 60, proteinG: 1.2,
                 carbsG: 15, fatG: 0.2, costRupees: 12, category: .veg, role: .fruit),
        FoodItem(name: "Watermelon", servingLabel: "1 bowl (200 g)", calories: 60, proteinG: 1.2,
                 carbsG: 15, fatG: 0.3, costRupees: 10, category: .veg, role: .fruit)
    ]

    // MARK: - Beverages and extras

    private static let extras: [FoodItem] = [
        FoodItem(name: "Green Tea", servingLabel: "1 cup", calories: 2, proteinG: 0,
                 carbsG: 0, fatG: 0, costRupees: 4, category: .veg, role: .beverage),
        FoodItem(name: "Tea with Milk", servingLabel: "1 cup (150 ml)", calories: 90, proteinG: 2,
                 carbsG: 12, fatG: 3, costRupees: 8, category: .veg, role: .beverage),
        FoodItem(name: "Black Coffee", servingLabel: "1 cup", calories: 5, proteinG: 0.3,
                 carbsG: 0, fatG: 0, costRupees: 6, category: .veg, role: .beverage),
        FoodItem(name: "Jaggery (gud)", servingLabel: "10 g", calories: 38, proteinG: 0.1,
                 carbsG: 9.5, fatG: 0, costRupees: 2, category: .veg, role: .snack),
        FoodItem(name: "Mustard Oil", servingLabel: "1 tsp (5 ml)", calories: 45, proteinG: 0,
                 carbsG: 0, fatG: 5, costRupees: 2, category: .veg, role: .snack)
    ]
}
