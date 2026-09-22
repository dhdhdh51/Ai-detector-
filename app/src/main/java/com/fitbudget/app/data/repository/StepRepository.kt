package com.fitbudget.app.data.repository

import com.fitbudget.app.data.database.dao.StepDao
import com.fitbudget.app.data.database.entity.StepLogEntity
import com.fitbudget.app.data.settings.SettingsRepository
import com.fitbudget.app.util.DateTimeUtils
import kotlinx.coroutines.flow.Flow

class StepRepository(
    private val stepDao: StepDao,
    private val settingsRepository: SettingsRepository,
    private val dayRepository: DayRepository
) {

    fun observeForDay(epochDay: Long): Flow<StepLogEntity?> = stepDao.observeForDay(epochDay)

    suspend fun getForDay(epochDay: Long): StepLogEntity? = stepDao.getForDay(epochDay)

    private suspend fun ensureRow(epochDay: Long): StepLogEntity {
        stepDao.getForDay(epochDay)?.let { return it }
        dayRepository.ensureDay(epochDay)
        val row = StepLogEntity(epochDay = epochDay, updatedAtMillis = DateTimeUtils.nowMillis())
        stepDao.upsert(row)
        return stepDao.getForDay(epochDay) ?: row
    }

    /**
     * Records a hardware step-counter reading.
     *
     * `TYPE_STEP_COUNTER` is cumulative since boot, so we persist the previous reading and add the
     * difference. A smaller value than last time means the device rebooted, in which case we just
     * re-anchor instead of inventing steps.
     */
    suspend fun recordSensorReading(
        counterValue: Long,
        epochDay: Long = DateTimeUtils.todayEpochDay()
    ) {
        val settings = settingsRepository.current()
        val previous = settings.lastStepCounterValue
        val previousDay = settings.lastStepCounterEpochDay

        if (previous < 0L || counterValue < previous || previousDay == 0L) {
            settingsRepository.setStepCounterState(counterValue, epochDay)
            return
        }

        val delta = (counterValue - previous).toInt()
        settingsRepository.setStepCounterState(counterValue, epochDay)
        if (delta <= 0) return

        // Steps accumulated since the last reading are credited to the day they are observed.
        ensureRow(epochDay)
        if (stepDao.addSensorSteps(epochDay, delta, DateTimeUtils.nowMillis()) == 0) {
            stepDao.upsert(
                StepLogEntity(
                    epochDay = epochDay,
                    sensorSteps = delta,
                    updatedAtMillis = DateTimeUtils.nowMillis()
                )
            )
        }
    }

    /** Manual entry used when the device has no step sensor or the user walked without the phone. */
    suspend fun setManualSteps(steps: Int, epochDay: Long = DateTimeUtils.todayEpochDay()) {
        val safe = steps.coerceIn(0, 100_000)
        ensureRow(epochDay)
        if (stepDao.setManualSteps(epochDay, safe, DateTimeUtils.nowMillis()) == 0) {
            stepDao.upsert(
                StepLogEntity(
                    epochDay = epochDay,
                    manualSteps = safe,
                    updatedAtMillis = DateTimeUtils.nowMillis()
                )
            )
        }
    }

    suspend fun addManualSteps(steps: Int, epochDay: Long = DateTimeUtils.todayEpochDay()) {
        val existing = stepDao.getForDay(epochDay)?.manualSteps ?: 0
        setManualSteps(existing + steps, epochDay)
    }

    suspend fun getAll(): List<StepLogEntity> = stepDao.getAll()

    suspend fun clear() = stepDao.clear()
}
