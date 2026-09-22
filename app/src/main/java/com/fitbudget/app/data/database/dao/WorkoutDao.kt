package com.fitbudget.app.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.fitbudget.app.data.database.entity.WorkoutSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {

    @Query("SELECT * FROM workout_sessions ORDER BY finishedAtMillis DESC")
    fun observeAll(): Flow<List<WorkoutSessionEntity>>

    @Query("SELECT * FROM workout_sessions ORDER BY finishedAtMillis DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<WorkoutSessionEntity>>

    @Query("SELECT COUNT(*) FROM workout_sessions WHERE epochDay = :epochDay")
    fun observeCountForDay(epochDay: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM workout_sessions WHERE epochDay BETWEEN :from AND :to")
    suspend fun countInRange(from: Long, to: Long): Int

    @Query("SELECT * FROM workout_sessions ORDER BY finishedAtMillis ASC")
    suspend fun getAll(): List<WorkoutSessionEntity>

    @Query(
        """
        SELECT epochDay AS epochDay, COUNT(*) AS value
        FROM workout_sessions
        WHERE epochDay BETWEEN :from AND :to
        GROUP BY epochDay
        """
    )
    suspend fun countsForRange(from: Long, to: Long): List<DayIntAggregate>

    @Insert
    suspend fun insert(session: WorkoutSessionEntity): Long

    @Insert
    suspend fun insertAll(sessions: List<WorkoutSessionEntity>)

    @Delete
    suspend fun delete(session: WorkoutSessionEntity)

    @Query("DELETE FROM workout_sessions")
    suspend fun clear()
}
