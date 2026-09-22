package com.fitbudget.app.data.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** One weighing. Exactly one entry per calendar day (a re-log replaces that day). */
@Entity(
    tableName = "weight_logs",
    indices = [Index(value = ["epochDay"], unique = true)]
)
data class WeightLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val epochDay: Long,
    val weightKg: Double,
    val note: String? = null,
    val createdAtMillis: Long = 0L
)
