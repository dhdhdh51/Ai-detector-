package com.fitbudget.app.data.repository

import com.fitbudget.app.data.database.dao.MealDao
import com.fitbudget.app.data.database.entity.FoodEntity
import com.fitbudget.app.data.database.entity.MealEntryEntity
import com.fitbudget.app.data.settings.SettingsRepository
import com.fitbudget.app.domain.MealPlanGenerator
import com.fitbudget.app.domain.model.MealType
import com.fitbudget.app.util.DateTimeUtils
import kotlinx.coroutines.flow.Flow

class DietRepository(
    private val mealDao: MealDao,
    private val foodRepository: FoodRepository,
    private val profileRepository: ProfileRepository,
    private val dayRepository: DayRepository,
    private val settingsRepository: SettingsRepository
) {

    fun observeDay(epochDay: Long): Flow<List<MealEntryEntity>> = mealDao.observeForDay(epochDay)

    suspend fun getDay(epochDay: Long): List<MealEntryEntity> = mealDao.getForDay(epochDay)

    /**
     * Generates the day's plan the first time a day is opened. Does nothing if the user already
     * has entries for that day, or if they deliberately cleared the generated plan.
     */
    suspend fun ensurePlanForDay(epochDay: Long = DateTimeUtils.todayEpochDay()) {
        val meta = dayRepository.ensureDay(epochDay)
        if (meta.planGenerated) return
        if (mealDao.countForDay(epochDay) > 0) {
            dayRepository.markPlanGenerated(epochDay)
            return
        }
        generateAndStore(epochDay)
    }

    /** Explicit user action: throw away the standard meals and build a fresh plan. */
    suspend fun regeneratePlan(epochDay: Long) {
        mealDao.deleteStandardMealsForDay(epochDay)
        generateAndStore(epochDay)
    }

    private suspend fun generateAndStore(epochDay: Long) {
        val profile = profileRepository.ensureExists()
        val meta = dayRepository.ensureDay(epochDay)
        val settings = settingsRepository.current()
        val foods = foodRepository.getAll()

        val entries = MealPlanGenerator.generate(
            request = MealPlanGenerator.Request(
                epochDay = epochDay,
                calorieTarget = meta.calorieTarget.takeIf { it > 0 } ?: profile.estimatedCalorieTarget,
                dailyBudget = meta.budget.takeIf { it > 0 } ?: profile.dailyBudget,
                preference = profile.dietPreference,
                excludedKeys = settings.excludedFoodKeys
            ),
            foods = foods
        )
        if (entries.isNotEmpty()) {
            mealDao.insertAll(entries)
        }
        dayRepository.markPlanGenerated(epochDay)
    }

    suspend fun setItemCompleted(id: Long, completed: Boolean) {
        mealDao.setCompleted(id, completed, if (completed) DateTimeUtils.nowMillis() else null)
    }

    suspend fun setMealCompleted(epochDay: Long, mealType: MealType, completed: Boolean) {
        mealDao.setMealCompleted(
            epochDay = epochDay,
            mealType = mealType,
            completed = completed,
            atMillis = if (completed) DateTimeUtils.nowMillis() else null
        )
    }

    suspend fun updateQuantity(id: Long, quantity: Double) = mealDao.updateQuantity(id, quantity)

    suspend fun delete(entry: MealEntryEntity) = mealDao.delete(entry)

    suspend fun deleteDay(epochDay: Long) = mealDao.deleteForDay(epochDay)

    /** Swaps the food on an existing line, keeping the meal, time and position. */
    suspend fun replaceFood(entryId: Long, food: FoodEntity, quantity: Double = 1.0) {
        val existing = mealDao.getById(entryId) ?: return
        mealDao.update(
            existing.copy(
                foodId = food.id,
                foodName = food.name,
                servingLabel = food.servingLabel,
                quantity = quantity,
                caloriesPerServing = food.calories,
                proteinPerServing = food.proteinG,
                costPerServing = food.costRupees,
                completed = false,
                completedAtMillis = null
            )
        )
    }

    suspend fun addFoodToMeal(
        epochDay: Long,
        mealType: MealType?,
        mealLabel: String,
        timeMinutes: Int,
        food: FoodEntity,
        quantity: Double = 1.0
    ): Long {
        dayRepository.ensureDay(epochDay)
        val sortOrder = (mealDao.getForDay(epochDay).maxOfOrNull { it.sortOrder } ?: 0) + 1
        return mealDao.insert(
            MealEntryEntity(
                epochDay = epochDay,
                mealType = mealType,
                mealLabel = mealLabel,
                timeMinutes = timeMinutes,
                foodId = food.id,
                foodName = food.name,
                servingLabel = food.servingLabel,
                quantity = quantity,
                caloriesPerServing = food.calories,
                proteinPerServing = food.proteinG,
                costPerServing = food.costRupees,
                sortOrder = sortOrder
            )
        )
    }

    /** A custom meal is just a group of entries with no [MealType] and a user-chosen label. */
    suspend fun addCustomMeal(
        epochDay: Long,
        label: String,
        timeMinutes: Int,
        food: FoodEntity,
        quantity: Double = 1.0
    ): Long = addFoodToMeal(
        epochDay = epochDay,
        mealType = null,
        mealLabel = label.trim().ifEmpty { "Custom meal" },
        timeMinutes = timeMinutes,
        food = food,
        quantity = quantity
    )

    suspend fun updateMealTime(epochDay: Long, groupKey: String, timeMinutes: Int) {
        mealDao.getForDay(epochDay)
            .filter { it.groupKey == groupKey }
            .forEach { mealDao.update(it.copy(timeMinutes = timeMinutes)) }
    }

    suspend fun clear() = mealDao.clear()
}
