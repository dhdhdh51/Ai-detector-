package com.fitbudget.app.data.repository

import android.database.sqlite.SQLiteConstraintException
import android.util.Log
import com.fitbudget.app.data.database.dao.FoodDao
import com.fitbudget.app.data.database.entity.FoodEntity
import com.fitbudget.app.data.seed.FoodSeed
import com.fitbudget.app.util.DateTimeUtils
import kotlinx.coroutines.flow.Flow

class FoodRepository(private val foodDao: FoodDao) {

    val foods: Flow<List<FoodEntity>> = foodDao.observeAll()

    fun search(query: String): Flow<List<FoodEntity>> =
        foodDao.search(query.trim().lowercase())

    suspend fun getAll(): List<FoodEntity> = foodDao.getAll()

    suspend fun getById(id: Long): FoodEntity? = foodDao.getById(id)

    /** Installs the bundled Indian food database the first time the app runs. */
    suspend fun seedIfEmpty() {
        if (foodDao.count() > 0) return
        foodDao.insertAllIgnoringDuplicates(FoodSeed.foods(DateTimeUtils.nowMillis()))
    }

    /**
     * Adds a user food. Duplicate names are reported as a friendly failure rather than crashing
     * on the unique index.
     */
    suspend fun addCustom(
        name: String,
        servingLabel: String,
        calories: Double,
        proteinG: Double,
        costRupees: Double,
        category: com.fitbudget.app.domain.model.FoodCategory,
        role: com.fitbudget.app.domain.model.FoodRole,
        carbsG: Double = 0.0,
        fatG: Double = 0.0
    ): Result<Long> {
        val trimmed = name.trim()
        val key = trimmed.lowercase()
        foodDao.findByNameKey(key)?.let {
            return Result.failure(DuplicateFoodException(trimmed))
        }
        return try {
            val id = foodDao.insert(
                FoodEntity(
                    name = trimmed,
                    nameKey = key,
                    servingLabel = servingLabel.trim(),
                    calories = calories,
                    proteinG = proteinG,
                    carbsG = carbsG,
                    fatG = fatG,
                    costRupees = costRupees,
                    category = category,
                    role = role,
                    isCustom = true,
                    createdAtMillis = DateTimeUtils.nowMillis()
                )
            )
            Result.success(id)
        } catch (constraint: SQLiteConstraintException) {
            Log.w(TAG, "Duplicate food rejected: $trimmed", constraint)
            Result.failure(DuplicateFoodException(trimmed))
        }
    }

    suspend fun update(food: FoodEntity): Result<Unit> = try {
        foodDao.update(food.copy(nameKey = food.name.trim().lowercase()))
        Result.success(Unit)
    } catch (constraint: SQLiteConstraintException) {
        Result.failure(DuplicateFoodException(food.name))
    }

    suspend fun delete(food: FoodEntity) = foodDao.delete(food)

    suspend fun clear() = foodDao.clear()

    suspend fun resetToSeed() {
        foodDao.clear()
        foodDao.insertAllIgnoringDuplicates(FoodSeed.foods(DateTimeUtils.nowMillis()))
    }

    private companion object {
        const val TAG = "FoodRepository"
    }
}

class DuplicateFoodException(val foodName: String) :
    Exception("\"$foodName\" is already in your food list.")
