package com.fitbudget.app.data.repository

import com.fitbudget.app.data.database.dao.DayMetaDao
import com.fitbudget.app.data.database.entity.DayMetaEntity
import com.fitbudget.app.data.settings.SettingsRepository
import com.fitbudget.app.util.DateTimeUtils
import kotlinx.coroutines.flow.Flow

/**
 * Owns the per-day snapshot of goals. Calling [ensureDay] is cheap and idempotent, and it is what
 * makes the midnight roll-over work: a new calendar day gets its own budget/water/step row while
 * previous days keep the goals that were actually in force.
 */
class DayRepository(
    private val dayMetaDao: DayMetaDao,
    private val profileRepository: ProfileRepository,
    private val settingsRepository: SettingsRepository
) {

    fun observe(epochDay: Long): Flow<DayMetaEntity?> = dayMetaDao.observeForDay(epochDay)

    suspend fun ensureDay(epochDay: Long = DateTimeUtils.todayEpochDay()): DayMetaEntity {
        dayMetaDao.getForDay(epochDay)?.let { return it }
        val profile = profileRepository.ensureExists()
        val settings = settingsRepository.current()
        val meta = DayMetaEntity(
            epochDay = epochDay,
            budget = profile.dailyBudget,
            waterTargetMl = settings.waterTargetMl,
            stepGoal = settings.stepGoal,
            calorieTarget = profile.estimatedCalorieTarget,
            planGenerated = false,
            createdAtMillis = DateTimeUtils.nowMillis()
        )
        dayMetaDao.insertIfMissing(meta)
        return dayMetaDao.getForDay(epochDay) ?: meta
    }

    suspend fun get(epochDay: Long): DayMetaEntity? = dayMetaDao.getForDay(epochDay)

    suspend fun getRange(from: Long, to: Long): List<DayMetaEntity> = dayMetaDao.getRange(from, to)

    /** Applies a changed budget to today (past days keep their historical budget). */
    suspend fun applyBudgetToToday(budget: Double) {
        val today = DateTimeUtils.todayEpochDay()
        ensureDay(today)
        dayMetaDao.setBudget(today, budget)
    }

    suspend fun applyWaterTargetToToday(targetMl: Int) {
        val today = DateTimeUtils.todayEpochDay()
        ensureDay(today)
        dayMetaDao.setWaterTarget(today, targetMl)
    }

    suspend fun applyStepGoalToToday(goal: Int) {
        val today = DateTimeUtils.todayEpochDay()
        ensureDay(today)
        dayMetaDao.setStepGoal(today, goal)
    }

    suspend fun markPlanGenerated(epochDay: Long, generated: Boolean = true) =
        dayMetaDao.setPlanGenerated(epochDay, generated)

    suspend fun clear() = dayMetaDao.clear()
}
