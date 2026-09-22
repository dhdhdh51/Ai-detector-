package com.fitbudget.app.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.fitbudget.app.data.database.entity.FoodEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodDao {

    @Query("SELECT * FROM foods ORDER BY name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<FoodEntity>>

    @Query(
        """
        SELECT * FROM foods
        WHERE (:query = '' OR nameKey LIKE '%' || :query || '%')
        ORDER BY isCustom DESC, name COLLATE NOCASE ASC
        """
    )
    fun search(query: String): Flow<List<FoodEntity>>

    @Query("SELECT * FROM foods ORDER BY name COLLATE NOCASE ASC")
    suspend fun getAll(): List<FoodEntity>

    @Query("SELECT * FROM foods WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): FoodEntity?

    @Query("SELECT * FROM foods WHERE nameKey = :nameKey LIMIT 1")
    suspend fun findByNameKey(nameKey: String): FoodEntity?

    @Query("SELECT COUNT(*) FROM foods")
    suspend fun count(): Int

    /** Throws on a duplicate name so the UI can show a friendly "already exists" message. */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(food: FoodEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllIgnoringDuplicates(foods: List<FoodEntity>)

    @Update
    suspend fun update(food: FoodEntity)

    @Delete
    suspend fun delete(food: FoodEntity)

    @Query("DELETE FROM foods WHERE isCustom = 1")
    suspend fun deleteCustomFoods()

    @Query("DELETE FROM foods")
    suspend fun clear()
}
