package com.fitbudget.app.data.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.fitbudget.app.domain.model.MealType

/**
 * One food line inside one day's plan. Days are independent rows keyed by [epochDay], so the
 * "daily reset" is inherent: a new day simply has no entries until the plan is generated.
 */
@Entity(
    tableName = "meal_entries",
    indices = [
        Index(value = ["epochDay"]),
        Index(value = ["epochDay", "mealType"]),
        Index(value = ["foodId"])
    ]
)
data class MealEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val epochDay: Long,
    /** Null for a user-created custom meal; the four standard meals use the enum. */
    val mealType: MealType?,
    val mealLabel: String,
    val timeMinutes: Int,
    /** Link back to the food database; null when the food row was deleted afterwards. */
    val foodId: Long? = null,
    val foodName: String,
    val servingLabel: String,
    val quantity: Double = 1.0,
    val caloriesPerServing: Double,
    val proteinPerServing: Double,
    val costPerServing: Double,
    val completed: Boolean = false,
    val completedAtMillis: Long? = null,
    val sortOrder: Int = 0
) {
    val totalCalories: Double get() = caloriesPerServing * quantity
    val totalProtein: Double get() = proteinPerServing * quantity
    val totalCost: Double get() = costPerServing * quantity

    /** Stable grouping key so custom meals sit in their own section. */
    val groupKey: String get() = mealType?.name ?: "CUSTOM:$mealLabel"
}
