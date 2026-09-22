package com.fitbudget.app.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.fitbudget.app.data.database.entity.DayMetaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DayMetaDao {

    @Query("SELECT * FROM day_meta WHERE epochDay = :epochDay LIMIT 1")
    fun observeForDay(epochDay: Long): Flow<DayMetaEntity?>

    @Query("SELECT * FROM day_meta WHERE epochDay = :epochDay LIMIT 1")
    suspend fun getForDay(epochDay: Long): DayMetaEntity?

    @Query("SELECT * FROM day_meta WHERE epochDay BETWEEN :from AND :to")
    suspend fun getRange(from: Long, to: Long): List<DayMetaEntity>

    @Query("SELECT * FROM day_meta ORDER BY epochDay ASC")
    suspend fun getAll(): List<DayMetaEntity>

    @Upsert
    suspend fun upsert(meta: DayMetaEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfMissing(meta: DayMetaEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(metas: List<DayMetaEntity>)

    @Query("UPDATE day_meta SET planGenerated = :generated WHERE epochDay = :epochDay")
    suspend fun setPlanGenerated(epochDay: Long, generated: Boolean)

    @Query("UPDATE day_meta SET budget = :budget WHERE epochDay = :epochDay")
    suspend fun setBudget(epochDay: Long, budget: Double)

    @Query("UPDATE day_meta SET waterTargetMl = :targetMl WHERE epochDay = :epochDay")
    suspend fun setWaterTarget(epochDay: Long, targetMl: Int)

    @Query("UPDATE day_meta SET stepGoal = :goal WHERE epochDay = :epochDay")
    suspend fun setStepGoal(epochDay: Long, goal: Int)

    @Query("DELETE FROM day_meta")
    suspend fun clear()
}
