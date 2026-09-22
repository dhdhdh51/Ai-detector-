package com.fitbudget.app.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.fitbudget.app.data.database.entity.ProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {

    @Query("SELECT * FROM profile WHERE id = :id LIMIT 1")
    fun observe(id: Int = ProfileEntity.SINGLETON_ID): Flow<ProfileEntity?>

    @Query("SELECT * FROM profile WHERE id = :id LIMIT 1")
    suspend fun get(id: Int = ProfileEntity.SINGLETON_ID): ProfileEntity?

    @Upsert
    suspend fun upsert(profile: ProfileEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfMissing(profile: ProfileEntity): Long

    @Update
    suspend fun update(profile: ProfileEntity)

    @Query("UPDATE profile SET currentWeightKg = :weightKg, updatedAtMillis = :now WHERE id = :id")
    suspend fun updateCurrentWeight(
        weightKg: Double,
        now: Long,
        id: Int = ProfileEntity.SINGLETON_ID
    )

    @Delete
    suspend fun delete(profile: ProfileEntity)

    @Query("DELETE FROM profile")
    suspend fun clear()
}
