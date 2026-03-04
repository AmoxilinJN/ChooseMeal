package com.choosemeal.app.data.repository

import com.choosemeal.app.data.local.entity.CafeteriaEntity
import com.choosemeal.app.data.local.entity.FloorEntity
import com.choosemeal.app.data.local.entity.MealEntity
import com.choosemeal.app.domain.model.CafeteriaWithFloors
import com.choosemeal.app.domain.model.MealOption
import kotlinx.coroutines.flow.Flow

interface MealRepository {
    fun observeHierarchy(): Flow<List<CafeteriaWithFloors>>
    fun observeCafeterias(): Flow<List<CafeteriaEntity>>
    fun observeFloors(cafeteriaId: Long?): Flow<List<FloorEntity>>
    fun observeMeals(floorId: Long?): Flow<List<MealEntity>>
    fun observeEnabledOptions(): Flow<List<MealOption>>

    suspend fun upsertCafeteria(item: CafeteriaEntity): Long
    suspend fun upsertFloor(item: FloorEntity): Long
    suspend fun upsertMeal(item: MealEntity): Long

    suspend fun deleteCafeteria(id: Long)
    suspend fun deleteFloor(id: Long)
    suspend fun deleteMeal(id: Long)

    suspend fun snapshot(): DataSnapshot
    suspend fun replaceAllFromImport(payload: ImportPayload)
    suspend fun seedIfEmpty()
}
