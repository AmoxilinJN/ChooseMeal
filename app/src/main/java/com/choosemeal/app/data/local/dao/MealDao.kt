package com.choosemeal.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.choosemeal.app.data.local.entity.MealEntity
import com.choosemeal.app.data.local.model.MealTripleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MealDao {
    @Query("SELECT * FROM meals ORDER BY id ASC")
    fun observeAll(): Flow<List<MealEntity>>

    @Query("SELECT * FROM meals ORDER BY id ASC")
    suspend fun getAll(): List<MealEntity>

    @Query("SELECT * FROM meals WHERE floorId = :floorId ORDER BY id ASC")
    fun observeByFloor(floorId: Long): Flow<List<MealEntity>>

    @Upsert
    suspend fun upsert(item: MealEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<MealEntity>): List<Long>

    @Query("DELETE FROM meals WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM meals")
    suspend fun clearAll()

    @Query(
        """
        SELECT
            c.id AS cafeteriaId,
            c.name AS cafeteriaName,
            f.id AS floorId,
            f.name AS floorName,
            m.id AS mealId,
            m.name AS mealName,
            m.tags AS mealTags
        FROM cafeterias c
        INNER JOIN floors f ON f.cafeteriaId = c.id
        INNER JOIN meals m ON m.floorId = f.id
        WHERE c.enabled = 1 AND f.enabled = 1 AND m.enabled = 1
        ORDER BY c.sortOrder ASC, c.id ASC, f.sortOrder ASC, f.id ASC, m.id ASC
        """
    )
    fun observeEnabledTriples(): Flow<List<MealTripleEntity>>
}
