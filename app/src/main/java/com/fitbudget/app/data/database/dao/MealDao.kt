package com.fitbudget.app.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.fitbudget.app.data.database.entity.MealEntryEntity
import com.fitbudget.app.domain.model.MealType
import kotlinx.coroutines.flow.Flow

@Dao
interface MealDao {

    @Query("SELECT * FROM meal_entries WHERE epochDay = :epochDay ORDER BY timeMinutes ASC, sortOrder ASC, id ASC")
    fun observeForDay(epochDay: Long): Flow<List<MealEntryEntity>>

    @Query("SELECT * FROM meal_entries WHERE epochDay = :epochDay ORDER BY timeMinutes ASC, sortOrder ASC, id ASC")
    suspend fun getForDay(epochDay: Long): List<MealEntryEntity>

    @Query("SELECT * FROM meal_entries WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): MealEntryEntity?

    @Query("SELECT COUNT(*) FROM meal_entries WHERE epochDay = :epochDay")
    suspend fun countForDay(epochDay: Long): Int

    @Query("SELECT * FROM meal_entries WHERE epochDay BETWEEN :from AND :to")
    suspend fun getRange(from: Long, to: Long): List<MealEntryEntity>

    @Insert
    suspend fun insert(entry: MealEntryEntity): Long

    @Insert
    suspend fun insertAll(entries: List<MealEntryEntity>)

    @Update
    suspend fun update(entry: MealEntryEntity)

    @Delete
    suspend fun delete(entry: MealEntryEntity)

    @Query("UPDATE meal_entries SET completed = :completed, completedAtMillis = :atMillis WHERE id = :id")
    suspend fun setCompleted(id: Long, completed: Boolean, atMillis: Long?)

    @Query(
        """
        UPDATE meal_entries SET completed = :completed, completedAtMillis = :atMillis
        WHERE epochDay = :epochDay AND mealType = :mealType
        """
    )
    suspend fun setMealCompleted(
        epochDay: Long,
        mealType: MealType,
        completed: Boolean,
        atMillis: Long?
    )

    @Query("UPDATE meal_entries SET quantity = :quantity WHERE id = :id")
    suspend fun updateQuantity(id: Long, quantity: Double)

    @Query("DELETE FROM meal_entries WHERE epochDay = :epochDay")
    suspend fun deleteForDay(epochDay: Long)

    @Query("DELETE FROM meal_entries WHERE epochDay = :epochDay AND mealType IS NOT NULL")
    suspend fun deleteStandardMealsForDay(epochDay: Long)

    @Query("DELETE FROM meal_entries")
    suspend fun clear()

    @Query(
        """
        SELECT epochDay AS epochDay,
               COUNT(*) AS itemCount,
               SUM(CASE WHEN completed THEN 1 ELSE 0 END) AS completedCount,
               IFNULL(SUM(caloriesPerServing * quantity), 0) AS plannedCalories,
               IFNULL(SUM(proteinPerServing * quantity), 0) AS plannedProtein,
               IFNULL(SUM(costPerServing * quantity), 0) AS plannedCost,
               IFNULL(SUM(CASE WHEN completed THEN caloriesPerServing * quantity ELSE 0 END), 0) AS consumedCalories,
               IFNULL(SUM(CASE WHEN completed THEN proteinPerServing * quantity ELSE 0 END), 0) AS consumedProtein,
               IFNULL(SUM(CASE WHEN completed THEN costPerServing * quantity ELSE 0 END), 0) AS consumedCost
        FROM meal_entries
        WHERE epochDay BETWEEN :from AND :to
        GROUP BY epochDay
        """
    )
    suspend fun aggregateRange(from: Long, to: Long): List<DayMealAggregate>

    @Query(
        """
        SELECT epochDay AS epochDay,
               mealType AS mealType,
               COUNT(*) AS itemCount,
               SUM(CASE WHEN completed THEN 1 ELSE 0 END) AS completedCount
        FROM meal_entries
        WHERE epochDay BETWEEN :from AND :to AND mealType IS NOT NULL
        GROUP BY epochDay, mealType
        """
    )
    suspend fun aggregateMealTypes(from: Long, to: Long): List<MealTypeAggregate>
}
