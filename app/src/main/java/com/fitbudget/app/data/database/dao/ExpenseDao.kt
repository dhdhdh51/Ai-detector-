package com.fitbudget.app.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.fitbudget.app.data.database.entity.ExpenseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {

    @Query("SELECT * FROM expenses WHERE epochDay = :epochDay ORDER BY createdAtMillis DESC")
    fun observeForDay(epochDay: Long): Flow<List<ExpenseEntity>>

    @Query("SELECT IFNULL(SUM(amount), 0) FROM expenses WHERE epochDay = :epochDay")
    fun observeTotalForDay(epochDay: Long): Flow<Double>

    @Query("SELECT IFNULL(SUM(amount), 0) FROM expenses WHERE epochDay = :epochDay")
    suspend fun totalForDay(epochDay: Long): Double

    @Query(
        """
        SELECT epochDay AS epochDay, IFNULL(SUM(amount), 0) AS value
        FROM expenses
        WHERE epochDay BETWEEN :from AND :to
        GROUP BY epochDay
        """
    )
    suspend fun totalsForRange(from: Long, to: Long): List<DayDoubleAggregate>

    @Query("SELECT * FROM expenses ORDER BY epochDay ASC")
    suspend fun getAll(): List<ExpenseEntity>

    @Insert
    suspend fun insert(expense: ExpenseEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(expenses: List<ExpenseEntity>)

    @Delete
    suspend fun delete(expense: ExpenseEntity)

    @Query("DELETE FROM expenses")
    suspend fun clear()
}
