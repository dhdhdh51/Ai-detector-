package com.fitbudget.app.data.repository

import com.fitbudget.app.data.database.dao.WaterDao
import com.fitbudget.app.data.database.entity.WaterLogEntity
import com.fitbudget.app.util.DateTimeUtils
import kotlinx.coroutines.flow.Flow

class WaterRepository(
    private val waterDao: WaterDao,
    private val dayRepository: DayRepository
) {

    fun observeTotal(epochDay: Long): Flow<Int> = waterDao.observeTotalForDay(epochDay)

    fun observeLogs(epochDay: Long): Flow<List<WaterLogEntity>> = waterDao.observeForDay(epochDay)

    suspend fun total(epochDay: Long): Int = waterDao.totalForDay(epochDay)

    suspend fun add(amountMl: Int, epochDay: Long = DateTimeUtils.todayEpochDay()) {
        if (amountMl <= 0) return
        dayRepository.ensureDay(epochDay)
        waterDao.insert(
            WaterLogEntity(
                epochDay = epochDay,
                amountMl = amountMl,
                loggedAtMillis = DateTimeUtils.nowMillis()
            )
        )
    }

    /** Removes the most recent entry of the day - the "undo" for a mis-tap. */
    suspend fun undoLast(epochDay: Long = DateTimeUtils.todayEpochDay()): Boolean {
        val last = waterDao.latestForDay(epochDay) ?: return false
        waterDao.delete(last)
        return true
    }

    suspend fun delete(log: WaterLogEntity) = waterDao.delete(log)

    suspend fun clearDay(epochDay: Long) = waterDao.deleteForDay(epochDay)

    suspend fun clear() = waterDao.clear()
}
