package com.fitbudget.app.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.fitbudget.app.data.database.entity.WeightLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WeightDao {

    @Query("SELECT * FROM weight_logs ORDER BY epochDay DESC")
    fun observeAll(): Flow<List<WeightLogEntity>>

    @Query("SELECT * FROM weight_logs WHERE epochDay BETWEEN :from AND :to ORDER BY epochDay ASC")
    fun observeRange(from: Long, to: Long): Flow<List<WeightLogEntity>>

    @Query("SELECT * FROM weight_logs ORDER BY epochDay ASC")
    suspend fun getAll(): List<WeightLogEntity>

    @Query("SELECT * FROM weight_logs WHERE epochDay BETWEEN :from AND :to ORDER BY epochDay ASC")
    suspend fun getRange(from: Long, to: Long): List<WeightLogEntity>

    @Query("SELECT * FROM weight_logs WHERE epochDay = :epochDay LIMIT 1")
    suspend fun getForDay(epochDay: Long): WeightLogEntity?

    @Query("SELECT * FROM weight_logs WHERE epochDay = :epochDay LIMIT 1")
    fun observeForDay(epochDay: Long): Flow<WeightLogEntity?>

    @Query("SELECT * FROM weight_logs ORDER BY epochDay DESC LIMIT 1")
    fun observeLatest(): Flow<WeightLogEntity?>

    @Query("SELECT * FROM weight_logs ORDER BY epochDay DESC LIMIT 1")
    suspend fun getLatest(): WeightLogEntity?

    @Query("SELECT * FROM weight_logs ORDER BY epochDay ASC LIMIT 1")
    suspend fun getEarliest(): WeightLogEntity?

    /** REPLACE keeps the one-entry-per-day guarantee. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(log: WeightLogEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(logs: List<WeightLogEntity>)

    @Delete
    suspend fun delete(log: WeightLogEntity)

    @Query("DELETE FROM weight_logs")
    suspend fun clear()
}
