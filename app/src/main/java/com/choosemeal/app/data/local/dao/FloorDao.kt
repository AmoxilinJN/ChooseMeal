package com.choosemeal.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.choosemeal.app.data.local.entity.FloorEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FloorDao {
    @Query("SELECT * FROM floors ORDER BY sortOrder ASC, id ASC")
    fun observeAll(): Flow<List<FloorEntity>>

    @Query("SELECT * FROM floors ORDER BY sortOrder ASC, id ASC")
    suspend fun getAll(): List<FloorEntity>

    @Query("SELECT * FROM floors WHERE cafeteriaId = :cafeteriaId ORDER BY sortOrder ASC, id ASC")
    fun observeByCafeteria(cafeteriaId: Long): Flow<List<FloorEntity>>

    @Upsert
    suspend fun upsert(item: FloorEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<FloorEntity>): List<Long>

    @Query("DELETE FROM floors WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM floors")
    suspend fun clearAll()
}
