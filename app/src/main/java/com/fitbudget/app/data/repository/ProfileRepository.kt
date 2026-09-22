package com.fitbudget.app.data.repository

import com.fitbudget.app.data.database.dao.ProfileDao
import com.fitbudget.app.data.database.entity.ProfileEntity
import com.fitbudget.app.util.DateTimeUtils
import kotlinx.coroutines.flow.Flow

class ProfileRepository(private val profileDao: ProfileDao) {

    val profile: Flow<ProfileEntity?> = profileDao.observe()

    suspend fun get(): ProfileEntity? = profileDao.get()

    /** Returns the stored profile, creating the pre-filled example row on first launch. */
    suspend fun ensureExists(): ProfileEntity {
        profileDao.get()?.let { return it }
        val fresh = ProfileEntity.default()
        profileDao.insertIfMissing(fresh)
        return profileDao.get() ?: fresh
    }

    suspend fun save(profile: ProfileEntity) {
        profileDao.upsert(profile.copy(updatedAtMillis = DateTimeUtils.nowMillis()))
    }

    /**
     * Persists the onboarding answers. The start weight is fixed at this moment so that progress
     * is always measured from a real starting point.
     */
    suspend fun completeOnboarding(profile: ProfileEntity) {
        val now = DateTimeUtils.nowMillis()
        profileDao.upsert(
            profile.copy(
                id = ProfileEntity.SINGLETON_ID,
                startWeightKg = profile.currentWeightKg,
                onboardingComplete = true,
                createdAtMillis = if (profile.createdAtMillis == 0L) now else profile.createdAtMillis,
                updatedAtMillis = now
            )
        )
    }

    suspend fun setCurrentWeight(weightKg: Double) {
        profileDao.updateCurrentWeight(weightKg, DateTimeUtils.nowMillis())
    }

    suspend fun setDailyBudget(budget: Double) {
        get()?.let { save(it.copy(dailyBudget = budget)) }
    }

    suspend fun setTargetWeight(targetKg: Double) {
        get()?.let { save(it.copy(targetWeightKg = targetKg)) }
    }

    suspend fun setCalorieOverride(calories: Double?) {
        get()?.let { save(it.copy(calorieTargetOverride = calories)) }
    }

    suspend fun clear() = profileDao.clear()
}
