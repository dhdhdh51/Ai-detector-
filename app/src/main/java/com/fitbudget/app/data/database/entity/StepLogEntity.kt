package com.fitbudget.app.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.fitbudget.app.domain.model.StepSource

/**
 * Steps for one day. Sensor deltas and manual corrections are stored separately so a manual
 * entry never silently wipes hardware-counted steps.
 */
@Entity(tableName = "step_logs")
data class StepLogEntity(
    @PrimaryKey val epochDay: Long,
    val sensorSteps: Int = 0,
    val manualSteps: Int = 0,
    val updatedAtMillis: Long = 0L
) {
    val totalSteps: Int get() = sensorSteps + manualSteps

    val source: StepSource
        get() = when {
            sensorSteps > 0 && manualSteps > 0 -> StepSource.MIXED
            manualSteps > 0 -> StepSource.MANUAL
            else -> StepSource.SENSOR
        }
}
