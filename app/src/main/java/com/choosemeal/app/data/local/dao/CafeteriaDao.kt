package com.choosemeal.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.choosemeal.app.data.local.entity.CafeteriaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CafeteriaDao {
    @Query("SELECT * FROM cafeterias ORDER BY sortOrder ASC, id ASC")
    fun observeAll(): Flow<List<CafeteriaEntity>>

    @Query("SELECT * FROM cafeterias ORDER BY sortOrder ASC, id ASC")
    suspend fun getAll(): List<CafeteriaEntity>

    @Query("SELECT COUNT(*) FROM cafeterias")
    suspend fun count(): Int

    @Upsert
    suspend fun upsert(item: CafeteriaEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<CafeteriaEntity>): List<Long>

    @Query("DELETE FROM cafeterias WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM cafeterias")
    suspend fun clearAll()
}
