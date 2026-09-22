package com.fitbudget.app.data.repository

import com.fitbudget.app.data.database.dao.WeightDao
import com.fitbudget.app.data.database.entity.WeightLogEntity
import com.fitbudget.app.util.DateTimeUtils
import kotlinx.coroutines.flow.Flow

class WeightRepository(
    private val weightDao: WeightDao,
    private val profileRepository: ProfileRepository
) {

    val logs: Flow<List<WeightLogEntity>> = weightDao.observeAll()

    val latest: Flow<WeightLogEntity?> = weightDao.observeLatest()

    fun observeRange(from: Long, to: Long): Flow<List<WeightLogEntity>> =
        weightDao.observeRange(from, to)

    fun observeForDay(epochDay: Long): Flow<WeightLogEntity?> = weightDao.observeForDay(epochDay)

    suspend fun getForDay(epochDay: Long): WeightLogEntity? = weightDao.getForDay(epochDay)

    suspend fun getAll(): List<WeightLogEntity> = weightDao.getAll()

    suspend fun getRange(from: Long, to: Long): List<WeightLogEntity> = weightDao.getRange(from, to)

    /**
     * Logs a weighing. The profile's current weight is kept in sync with the most recent entry so
     * BMI and progress on the dashboard always reflect real data.
     */
    suspend fun log(
        weightKg: Double,
        epochDay: Long = DateTimeUtils.todayEpochDay(),
        note: String? = null
    ) {
        weightDao.upsert(
            WeightLogEntity(
                epochDay = epochDay,
                weightKg = weightKg,
                note = note?.trim()?.takeIf { it.isNotEmpty() },
                createdAtMillis = DateTimeUtils.nowMillis()
            )
        )
        syncProfileWithLatest()
    }

    suspend fun delete(log: WeightLogEntity) {
        weightDao.delete(log)
        syncProfileWithLatest()
    }

    private suspend fun syncProfileWithLatest() {
        val latestLog = weightDao.getLatest() ?: return
        profileRepository.setCurrentWeight(latestLog.weightKg)
    }

    suspend fun clear() = weightDao.clear()
}
