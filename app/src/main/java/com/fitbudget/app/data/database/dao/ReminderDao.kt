package com.fitbudget.app.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.fitbudget.app.data.database.entity.ReminderEntity
import com.fitbudget.app.domain.model.ReminderType
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {

    @Query("SELECT * FROM reminders")
    fun observeAll(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders")
    suspend fun getAll(): List<ReminderEntity>

    @Query("SELECT * FROM reminders WHERE type = :type LIMIT 1")
    suspend fun get(type: ReminderType): ReminderEntity?

    @Upsert
    suspend fun upsert(reminder: ReminderEntity)

    @Upsert
    suspend fun upsertAll(reminders: List<ReminderEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllIgnoringExisting(reminders: List<ReminderEntity>)

    @Query("UPDATE reminders SET enabled = :enabled WHERE type = :type")
    suspend fun setEnabled(type: ReminderType, enabled: Boolean)

    @Query("DELETE FROM reminders")
    suspend fun clear()
}
