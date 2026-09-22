package com.fitbudget.app.data.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Extra food spending that is not part of the planned meals (tea stall, fruit on the way home…). */
@Entity(
    tableName = "expenses",
    indices = [Index(value = ["epochDay"])]
)
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val epochDay: Long,
    val label: String,
    val amount: Double,
    val createdAtMillis: Long = 0L
)
