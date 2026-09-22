package com.fitbudget.app.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.fitbudget.app.data.database.entity.WaterLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WaterDao {

    @Query("SELECT * FROM water_logs WHERE epochDay = :epochDay ORDER BY loggedAtMillis DESC")
    fun observeForDay(epochDay: Long): Flow<List<WaterLogEntity>>

    @Query("SELECT IFNULL(SUM(amountMl), 0) FROM water_logs WHERE epochDay = :epochDay")
    fun observeTotalForDay(epochDay: Long): Flow<Int>

    @Query("SELECT IFNULL(SUM(amountMl), 0) FROM water_logs WHERE epochDay = :epochDay")
    suspend fun totalForDay(epochDay: Long): Int

    @Query(
        """
        SELECT epochDay AS epochDay, IFNULL(SUM(amountMl), 0) AS value
        FROM water_logs
        WHERE epochDay BETWEEN :from AND :to
        GROUP BY epochDay
        """
    )
    suspend fun totalsForRange(from: Long, to: Long): List<DayIntAggregate>

    @Query("SELECT * FROM water_logs ORDER BY epochDay ASC, loggedAtMillis ASC")
    suspend fun getAll(): List<WaterLogEntity>

    @Query("SELECT * FROM water_logs WHERE epochDay = :epochDay ORDER BY loggedAtMillis DESC LIMIT 1")
    suspend fun latestForDay(epochDay: Long): WaterLogEntity?

    @Insert
    suspend fun insert(log: WaterLogEntity): Long

    @Insert
    suspend fun insertAll(logs: List<WaterLogEntity>)

    @Delete
    suspend fun delete(log: WaterLogEntity)

    @Query("DELETE FROM water_logs WHERE epochDay = :epochDay")
    suspend fun deleteForDay(epochDay: Long)

    @Query("DELETE FROM water_logs")
    suspend fun clear()
}
