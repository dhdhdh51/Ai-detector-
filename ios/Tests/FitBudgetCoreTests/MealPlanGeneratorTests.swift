import XCTest
@testable import FitBudgetCore

final class MealPlanGeneratorTests: XCTestCase {

    private let foods = FoodSeed.items

    private func request(
        day: Int = 20_000,
        calories: Double = 2_000,
        budget: Double = 100,
        preference: DietPreference = .eggetarian,
        excluded: Set<String> = []
    ) -> MealPlanGenerator.Request {
        MealPlanGenerator.Request(
            day: DayKey(day),
            calorieTarget: calories,
            dailyBudget: budget,
            preference: preference,
            excludedKeys: excluded
        )
    }

    func testPlanCoversAllFourStandardMeals() {
        let plan = MealPlanGenerator.generate(request: request(), foods: foods)
        let types = Set(plan.compactMap(\.mealType))
        XCTAssertEqual(types, Set(MealType.allCases))
    }

    func testPlanFitsInsideDailyBudget() {
        let plan = MealPlanGenerator.generate(request: request(budget: 100), foods: foods)
        let total = plan.reduce(0) { $0 + $1.totalCost }
        XCTAssertLessThanOrEqual(total, 100, "plan cost \(total) should not exceed ₹100")
    }

    func testVerySmallBudgetStillProducesFood() {
        let plan = MealPlanGenerator.generate(request: request(budget: 40), foods: foods)
        XCTAssertFalse(plan.isEmpty)
        XCTAssertLessThanOrEqual(plan.reduce(0) { $0 + $1.totalCost }, 40)
    }

    func testLargerBudgetBuysAtLeastAsMuch() {
        let small = MealPlanGenerator.generate(request: request(budget: 60), foods: foods)
            .reduce(0) { $0 + $1.totalCost }
        let large = MealPlanGenerator.generate(request: request(budget: 200), foods: foods)
            .reduce(0) { $0 + $1.totalCost }
        XCTAssertGreaterThanOrEqual(large, small)
    }

    func testVegetarianPlansNeverContainEggOrMeat() {
        let plan = MealPlanGenerator.generate(request: request(preference: .vegetarian), foods: foods)
        let used = plan.compactMap { item in foods.first { $0.nameKey == item.foodNameKey } }
        XCTAssertFalse(used.isEmpty)
        XCTAssertTrue(used.allSatisfy { $0.category == .veg })
    }

    func testEggetarianPlansNeverContainMeat() {
        let plan = MealPlanGenerator.generate(request: request(preference: .eggetarian), foods: foods)
        let used = plan.compactMap { item in foods.first { $0.nameKey == item.foodNameKey } }
        XCTAssertTrue(used.allSatisfy { $0.category != .nonVeg })
    }

    func testExcludedFoodsAreNeverSuggested() {
        let excluded: Set<String> = ["boiled egg", "egg omelette", "egg white"]
        let plan = MealPlanGenerator.generate(request: request(excluded: excluded), foods: foods)
        XCTAssertTrue(plan.allSatisfy { !$0.foodName.lowercased().contains("egg") })
    }

    func testSameDayAlwaysGeneratesSamePlan() {
        let first = MealPlanGenerator.generate(request: request(day: 20_100), foods: foods)
        let second = MealPlanGenerator.generate(request: request(day: 20_100), foods: foods)
        XCTAssertEqual(
            first.map { "\($0.foodName)|\($0.quantity)" },
            second.map { "\($0.foodName)|\($0.quantity)" }
        )
    }

    func testDifferentDaysVaryTheMenu() {
        let monday = MealPlanGenerator.generate(request: request(day: 20_100), foods: foods).map(\.foodName)
        let tuesday = MealPlanGenerator.generate(request: request(day: 20_101), foods: foods).map(\.foodName)
        XCTAssertNotEqual(monday, tuesday, "plans for consecutive days should not be identical")
    }

    func testEmptyFoodDatabaseYieldsEmptyPlan() {
        XCTAssertTrue(MealPlanGenerator.generate(request: request(), foods: []).isEmpty)
    }

    func testExcludingEverythingYieldsEmptyPlan() {
        let allKeys = Set(foods.map(\.nameKey))
        XCTAssertTrue(MealPlanGenerator.generate(request: request(excluded: allKeys), foods: foods).isEmpty)
    }

    func testEntriesCarryRealNutritionAndCost() {
        let plan = MealPlanGenerator.generate(request: request(), foods: foods)
        XCTAssertTrue(plan.allSatisfy { $0.caloriesPerServing > 0 })
        XCTAssertTrue(plan.allSatisfy { $0.quantity > 0 })
        XCTAssertTrue(plan.allSatisfy { $0.costPerServing >= 0 })
        XCTAssertTrue(plan.allSatisfy { !$0.servingLabel.isEmpty })
    }

    func testMealTimesDefaultToStandardSchedule() {
        let plan = MealPlanGenerator.generate(request: request(), foods: foods)
        XCTAssertEqual(plan.first { $0.mealType == .breakfast }?.minutesOfDay, 8 * 60)
        XCTAssertEqual(plan.first { $0.mealType == .dinner }?.minutesOfDay, 20 * 60)
    }

    func testPlansLandInSensibleCalorieBand() {
        let plan = MealPlanGenerator.generate(request: request(calories: 2_000, budget: 150), foods: foods)
        let total = plan.reduce(0) { $0 + $1.totalCalories }
        XCTAssertTrue((900...2_600).contains(total), "total calories \(total) should be a realistic day")
    }

    func testEveryMealHasAtLeastOneItemWhenBudgetAllows() {
        let plan = MealPlanGenerator.generate(request: request(budget: 120), foods: foods)
        for type in MealType.allCases {
            XCTAssertTrue(plan.contains { $0.mealType == type }, "meal \(type) should have an item")
        }
    }

    func testNegativeDaySeedDoesNotCrash() {
        let plan = MealPlanGenerator.generate(request: request(day: -5), foods: foods)
        XCTAssertFalse(plan.isEmpty)
    }

    func testSeedDatabaseContainsTheRequiredFoods() {
        let required = [
            "boiled egg", "roti (wheat)", "rice (cooked)", "toor dal (cooked)", "boiled chana",
            "soya chunks (cooked)", "paneer", "curd (dahi)", "toned milk", "banana", "apple",
            "poha", "oats (cooked in water)", "boiled potato", "tomato", "onion",
            "seasonal mixed vegetables", "peanuts (roasted)", "roasted chana"
        ]
        let keys = Set(foods.map(\.nameKey))
        for item in required {
            XCTAssertTrue(keys.contains(item), "seed database should contain \(item)")
        }
        XCTAssertEqual(keys.count, foods.count, "food names must be unique")
    }
}
