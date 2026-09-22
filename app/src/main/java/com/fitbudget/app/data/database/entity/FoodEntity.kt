package com.fitbudget.app.data.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.fitbudget.app.domain.model.FoodCategory
import com.fitbudget.app.domain.model.FoodRole

/**
 * Offline food database. Seeded with common low-cost Indian foods and extended by the user.
 * The unique name index is what makes duplicate entries a handled error instead of a crash.
 */
@Entity(
    tableName = "foods",
    indices = [
        Index(value = ["nameKey"], unique = true),
        Index(value = ["category"]),
        Index(value = ["role"])
    ]
)
data class FoodEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    /** Lower-cased trimmed name used purely for duplicate detection. */
    val nameKey: String = name.trim().lowercase(),
    val servingLabel: String,
    val calories: Double,
    val proteinG: Double,
    val carbsG: Double = 0.0,
    val fatG: Double = 0.0,
    /** Approximate street-market cost of one serving, in rupees. */
    val costRupees: Double,
    val category: FoodCategory = FoodCategory.VEG,
    val role: FoodRole = FoodRole.SNACK,
    val isCustom: Boolean = false,
    val createdAtMillis: Long = 0L
) {
    /** Grams of protein per rupee - the app's affordability ranking for protein sources. */
    val proteinPerRupee: Double get() = if (costRupees <= 0.0) proteinG else proteinG / costRupees
}
