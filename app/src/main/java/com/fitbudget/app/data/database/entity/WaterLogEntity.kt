package com.fitbudget.app.data.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "water_logs",
    indices = [Index(value = ["epochDay"])]
)
data class WaterLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val epochDay: Long,
    val amountMl: Int,
    val loggedAtMillis: Long = 0L
)
