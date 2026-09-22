package com.fitbudget.app.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.fitbudget.app.data.database.entity.StepLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StepDao {

    @Query("SELECT * FROM step_logs WHERE epochDay = :epochDay LIMIT 1")
    fun observeForDay(epochDay: Long): Flow<StepLogEntity?>

    @Query("SELECT * FROM step_logs WHERE epochDay = :epochDay LIMIT 1")
    suspend fun getForDay(epochDay: Long): StepLogEntity?

    @Query("SELECT * FROM step_logs ORDER BY epochDay ASC")
    suspend fun getAll(): List<StepLogEntity>

    @Query(
        """
        SELECT epochDay AS epochDay, (sensorSteps + manualSteps) AS value
        FROM step_logs
        WHERE epochDay BETWEEN :from AND :to
        """
    )
    suspend fun totalsForRange(from: Long, to: Long): List<DayIntAggregate>

    @Upsert
    suspend fun upsert(log: StepLogEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(logs: List<StepLogEntity>)

    @Query(
        """
        UPDATE step_logs SET sensorSteps = sensorSteps + :delta, updatedAtMillis = :now
        WHERE epochDay = :epochDay
        """
    )
    suspend fun addSensorSteps(epochDay: Long, delta: Int, now: Long): Int

    @Query("UPDATE step_logs SET manualSteps = :steps, updatedAtMillis = :now WHERE epochDay = :epochDay")
    suspend fun setManualSteps(epochDay: Long, steps: Int, now: Long): Int

    @Query("DELETE FROM step_logs")
    suspend fun clear()
}
