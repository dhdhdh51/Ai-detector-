package com.fitbudget.app.data.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.fitbudget.app.domain.model.WorkoutCategory

/** A finished (or abandoned-but-saved) workout session. */
@Entity(
    tableName = "workout_sessions",
    indices = [Index(value = ["epochDay"])]
)
data class WorkoutSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val epochDay: Long,
    val templateId: String,
    val templateName: String,
    val category: WorkoutCategory,
    val startedAtMillis: Long,
    val finishedAtMillis: Long,
    val durationSeconds: Int,
    val exercisesCompleted: Int,
    val exercisesSkipped: Int,
    val exercisesTotal: Int,
    /** Rough energy estimate, clearly labelled as an estimate in the UI. */
    val estimatedCalories: Int,
    val note: String? = null
) {
    val completionFraction: Double
        get() = if (exercisesTotal <= 0) 0.0
        else (exercisesCompleted.toDouble() / exercisesTotal).coerceIn(0.0, 1.0)
}
