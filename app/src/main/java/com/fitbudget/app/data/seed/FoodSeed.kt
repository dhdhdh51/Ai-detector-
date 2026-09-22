package com.fitbudget.app.data.seed

import com.fitbudget.app.data.database.entity.FoodEntity
import com.fitbudget.app.domain.model.FoodCategory
import com.fitbudget.app.domain.model.FoodRole

/**
 * Offline food database seed: common, low-cost Indian foods.
 *
 * Calories/protein are rounded reference values for a typical home-cooked serving and costs are
 * approximate Indian market prices. Everything here is an estimate and the user can edit or
 * add their own foods.
 */
object FoodSeed {

    fun foods(nowMillis: Long): List<FoodEntity> = raw.map { item ->
        FoodEntity(
            name = item.name,
            nameKey = item.name.trim().lowercase(),
            servingLabel = item.serving,
            calories = item.kcal,
            proteinG = item.protein,
            carbsG = item.carbs,
            fatG = item.fat,
            costRupees = item.cost,
            category = item.category,
            role = item.role,
            isCustom = false,
            createdAtMillis = nowMillis
        )
    }

    private data class Seed(
        val name: String,
        val serving: String,
        val kcal: Double,
        val protein: Double,
        val carbs: Double,
        val fat: Double,
        val cost: Double,
        val category: FoodCategory,
        val role: FoodRole
    )

    private val raw = listOf(
        // ---- Affordable protein ----
        Seed("Boiled Egg", "1 egg (50 g)", 78.0, 6.3, 0.6, 5.3, 7.0, FoodCategory.EGG, FoodRole.PROTEIN),
        Seed("Egg Omelette", "2 eggs + onion", 190.0, 13.0, 3.0, 14.0, 18.0, FoodCategory.EGG, FoodRole.PROTEIN),
        Seed("Egg White", "2 egg whites", 34.0, 7.2, 0.5, 0.1, 14.0, FoodCategory.EGG, FoodRole.PROTEIN),
        Seed("Toor Dal (cooked)", "1 bowl (150 g)", 150.0, 9.0, 21.0, 3.0, 12.0, FoodCategory.VEG, FoodRole.PROTEIN),
        Seed("Masoor Dal (cooked)", "1 bowl (150 g)", 140.0, 9.0, 20.0, 2.0, 11.0, FoodCategory.VEG, FoodRole.PROTEIN),
        Seed("Moong Dal (cooked)", "1 bowl (150 g)", 145.0, 9.5, 20.0, 2.0, 12.0, FoodCategory.VEG, FoodRole.PROTEIN),
        Seed("Boiled Chana", "1 bowl (100 g)", 164.0, 8.9, 27.0, 2.6, 14.0, FoodCategory.VEG, FoodRole.PROTEIN),
        Seed("Roasted Chana", "30 g", 110.0, 6.0, 18.0, 1.5, 5.0, FoodCategory.VEG, FoodRole.SNACK),
        Seed("Soya Chunks (cooked)", "50 g dry", 172.0, 26.0, 16.0, 0.5, 10.0, FoodCategory.VEG, FoodRole.PROTEIN),
        Seed("Sprouted Moong", "1 bowl (100 g)", 120.0, 8.0, 19.0, 0.6, 10.0, FoodCategory.VEG, FoodRole.PROTEIN),
        Seed("Rajma (cooked)", "1 bowl (150 g)", 180.0, 10.0, 30.0, 1.0, 16.0, FoodCategory.VEG, FoodRole.PROTEIN),
        Seed("Paneer", "50 g", 145.0, 9.0, 2.0, 11.0, 25.0, FoodCategory.VEG, FoodRole.PROTEIN),
        Seed("Peanuts (roasted)", "30 g", 170.0, 7.6, 6.0, 14.0, 6.0, FoodCategory.VEG, FoodRole.PROTEIN),
        Seed("Sattu Drink", "30 g sattu", 120.0, 6.0, 20.0, 1.5, 8.0, FoodCategory.VEG, FoodRole.BEVERAGE),
        Seed("Chicken Breast", "100 g", 165.0, 31.0, 0.0, 3.6, 40.0, FoodCategory.NON_VEG, FoodRole.PROTEIN),
        Seed("Chicken Curry", "1 bowl (150 g)", 240.0, 22.0, 6.0, 14.0, 60.0, FoodCategory.NON_VEG, FoodRole.PROTEIN),
        Seed("Rohu Fish (curry cut)", "100 g", 140.0, 20.0, 0.0, 6.0, 40.0, FoodCategory.NON_VEG, FoodRole.PROTEIN),

        // ---- Staples ----
        Seed("Roti (wheat)", "1 roti (40 g atta)", 110.0, 3.2, 22.0, 1.0, 3.0, FoodCategory.VEG, FoodRole.STAPLE),
        Seed("Rice (cooked)", "1 cup (150 g)", 200.0, 4.0, 44.0, 0.4, 6.0, FoodCategory.VEG, FoodRole.STAPLE),
        Seed("Poha", "1 plate (150 g)", 250.0, 5.0, 45.0, 6.0, 12.0, FoodCategory.VEG, FoodRole.STAPLE),
        Seed("Oats (cooked in water)", "40 g dry", 150.0, 5.0, 27.0, 3.0, 10.0, FoodCategory.VEG, FoodRole.STAPLE),
        Seed("Upma", "1 plate (200 g)", 230.0, 6.0, 38.0, 6.0, 12.0, FoodCategory.VEG, FoodRole.STAPLE),
        Seed("Idli", "2 pieces", 140.0, 4.0, 28.0, 0.6, 12.0, FoodCategory.VEG, FoodRole.STAPLE),
        Seed("Plain Dosa", "1 dosa", 160.0, 4.0, 26.0, 4.0, 15.0, FoodCategory.VEG, FoodRole.STAPLE),
        Seed("Whole Wheat Bread", "2 slices", 140.0, 5.0, 26.0, 2.0, 10.0, FoodCategory.VEG, FoodRole.STAPLE),
        Seed("Daliya (broken wheat)", "50 g dry", 170.0, 6.0, 34.0, 1.0, 8.0, FoodCategory.VEG, FoodRole.STAPLE),

        // ---- Dairy ----
        Seed("Toned Milk", "1 glass (200 ml)", 120.0, 6.4, 10.0, 5.0, 12.0, FoodCategory.VEG, FoodRole.DAIRY),
        Seed("Curd (dahi)", "1 bowl (150 g)", 90.0, 5.0, 7.0, 4.5, 12.0, FoodCategory.VEG, FoodRole.DAIRY),
        Seed("Buttermilk (chaas)", "1 glass (250 ml)", 40.0, 2.0, 4.0, 1.0, 8.0, FoodCategory.VEG, FoodRole.BEVERAGE),
        Seed("Ghee", "1 tsp (5 g)", 45.0, 0.0, 0.0, 5.0, 6.0, FoodCategory.VEG, FoodRole.SNACK),

        // ---- Vegetables ----
        Seed("Seasonal Mixed Vegetables", "1 bowl (150 g)", 90.0, 3.0, 14.0, 2.0, 12.0, FoodCategory.VEG, FoodRole.VEGETABLE),
        Seed("Boiled Potato", "1 medium (150 g)", 130.0, 3.0, 30.0, 0.2, 6.0, FoodCategory.VEG, FoodRole.VEGETABLE),
        Seed("Sweet Potato", "1 medium (150 g)", 130.0, 2.0, 30.0, 0.1, 12.0, FoodCategory.VEG, FoodRole.VEGETABLE),
        Seed("Tomato", "1 medium (100 g)", 22.0, 1.0, 4.0, 0.2, 4.0, FoodCategory.VEG, FoodRole.VEGETABLE),
        Seed("Onion", "1 medium (100 g)", 44.0, 1.2, 10.0, 0.1, 4.0, FoodCategory.VEG, FoodRole.VEGETABLE),
        Seed("Palak (spinach, cooked)", "1 bowl (150 g)", 45.0, 3.4, 5.0, 0.5, 8.0, FoodCategory.VEG, FoodRole.VEGETABLE),
        Seed("Cabbage Sabzi", "1 bowl (150 g)", 70.0, 2.0, 10.0, 2.5, 8.0, FoodCategory.VEG, FoodRole.VEGETABLE),
        Seed("Lauki (bottle gourd) Sabzi", "1 bowl (150 g)", 65.0, 1.5, 9.0, 2.5, 8.0, FoodCategory.VEG, FoodRole.VEGETABLE),
        Seed("Carrot", "1 medium (80 g)", 25.0, 0.6, 6.0, 0.1, 4.0, FoodCategory.VEG, FoodRole.VEGETABLE),
        Seed("Cucumber", "1 medium (150 g)", 30.0, 1.0, 5.0, 0.2, 5.0, FoodCategory.VEG, FoodRole.VEGETABLE),
        Seed("Green Salad", "1 plate", 50.0, 2.0, 8.0, 0.5, 10.0, FoodCategory.VEG, FoodRole.VEGETABLE),

        // ---- Fruit ----
        Seed("Banana", "1 medium (118 g)", 105.0, 1.3, 27.0, 0.4, 5.0, FoodCategory.VEG, FoodRole.FRUIT),
        Seed("Apple", "1 medium (180 g)", 95.0, 0.5, 25.0, 0.3, 18.0, FoodCategory.VEG, FoodRole.FRUIT),
        Seed("Guava", "1 medium (120 g)", 65.0, 2.6, 14.0, 0.9, 10.0, FoodCategory.VEG, FoodRole.FRUIT),
        Seed("Papaya", "1 bowl (150 g)", 60.0, 0.9, 15.0, 0.2, 12.0, FoodCategory.VEG, FoodRole.FRUIT),
        Seed("Orange", "1 medium (150 g)", 60.0, 1.2, 15.0, 0.2, 12.0, FoodCategory.VEG, FoodRole.FRUIT),
        Seed("Watermelon", "1 bowl (200 g)", 60.0, 1.2, 15.0, 0.3, 10.0, FoodCategory.VEG, FoodRole.FRUIT),

        // ---- Beverages / extras ----
        Seed("Green Tea", "1 cup", 2.0, 0.0, 0.0, 0.0, 4.0, FoodCategory.VEG, FoodRole.BEVERAGE),
        Seed("Tea with Milk", "1 cup (150 ml)", 90.0, 2.0, 12.0, 3.0, 8.0, FoodCategory.VEG, FoodRole.BEVERAGE),
        Seed("Black Coffee", "1 cup", 5.0, 0.3, 0.0, 0.0, 6.0, FoodCategory.VEG, FoodRole.BEVERAGE),
        Seed("Jaggery (gud)", "10 g", 38.0, 0.1, 9.5, 0.0, 2.0, FoodCategory.VEG, FoodRole.SNACK),
        Seed("Mustard Oil", "1 tsp (5 ml)", 45.0, 0.0, 0.0, 5.0, 2.0, FoodCategory.VEG, FoodRole.SNACK)
    )
}
