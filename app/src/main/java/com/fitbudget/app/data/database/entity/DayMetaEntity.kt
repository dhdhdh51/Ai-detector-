package com.fitbudget.app.data.database.entity

/**
 * Snapshot of the goals that applied on a given day. Written once when a day is first opened
 * so that changing the budget or a goal later never rewrites history.
 */
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "day_meta")
data class DayMetaEntity(
    @PrimaryKey val epochDay: Long,
    val budget: Double,
    val waterTargetMl: Int,
    val stepGoal: Int,
    val calorieTarget: Double,
    val planGenerated: Boolean = false,
    val createdAtMillis: Long = 0L
)
