package com.choosemeal.app.data.repository

import androidx.room.withTransaction
import com.choosemeal.app.data.local.ChooseMealDatabase
import com.choosemeal.app.data.local.entity.CafeteriaEntity
import com.choosemeal.app.data.local.entity.FloorEntity
import com.choosemeal.app.data.local.entity.MealEntity
import com.choosemeal.app.domain.model.CafeteriaWithFloors
import com.choosemeal.app.domain.model.FloorWithMeals
import com.choosemeal.app.domain.model.MealOption
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class OfflineMealRepository(
    private val database: ChooseMealDatabase,
) : MealRepository {

    private val cafeteriaDao = database.cafeteriaDao()
    private val floorDao = database.floorDao()
    private val mealDao = database.mealDao()

    override fun observeHierarchy(): Flow<List<CafeteriaWithFloors>> {
        return combine(
            cafeteriaDao.observeAll(),
            floorDao.observeAll(),
            mealDao.observeAll(),
        ) { cafeterias, floors, meals ->
            cafeterias.map { cafeteria ->
                val floorModels = floors
                    .filter { it.cafeteriaId == cafeteria.id }
                    .map { floor ->
                        FloorWithMeals(
                            floor = floor,
                            meals = meals.filter { it.floorId == floor.id },
                        )
                    }
                CafeteriaWithFloors(cafeteria = cafeteria, floors = floorModels)
            }
        }
    }

    override fun observeCafeterias(): Flow<List<CafeteriaEntity>> = cafeteriaDao.observeAll()

    override fun observeFloors(cafeteriaId: Long?): Flow<List<FloorEntity>> {
        return if (cafeteriaId == null) {
            floorDao.observeAll()
        } else {
            floorDao.observeByCafeteria(cafeteriaId)
        }
    }

    override fun observeMeals(floorId: Long?): Flow<List<MealEntity>> {
        return if (floorId == null) {
            mealDao.observeAll()
        } else {
            mealDao.observeByFloor(floorId)
        }
    }

    override fun observeEnabledOptions(): Flow<List<MealOption>> {
        return mealDao.observeEnabledTriples().map { triples ->
            triples.map {
                MealOption(
                    cafeteriaId = it.cafeteriaId,
                    cafeteriaName = it.cafeteriaName,
                    floorId = it.floorId,
                    floorName = it.floorName,
                    mealId = it.mealId,
                    mealName = it.mealName,
                    mealTags = it.mealTags,
                )
            }
        }
    }

    override suspend fun upsertCafeteria(item: CafeteriaEntity): Long = cafeteriaDao.upsert(item)

    override suspend fun upsertFloor(item: FloorEntity): Long = floorDao.upsert(item)

    override suspend fun upsertMeal(item: MealEntity): Long = mealDao.upsert(item)

    override suspend fun deleteCafeteria(id: Long) = cafeteriaDao.deleteById(id)

    override suspend fun deleteFloor(id: Long) = floorDao.deleteById(id)

    override suspend fun deleteMeal(id: Long) = mealDao.deleteById(id)

    override suspend fun snapshot(): DataSnapshot {
        return DataSnapshot(
            cafeterias = cafeteriaDao.getAll(),
            floors = floorDao.getAll(),
            meals = mealDao.getAll(),
        )
    }

    override suspend fun replaceAllFromImport(payload: ImportPayload) {
        database.withTransaction {
            mealDao.clearAll()
            floorDao.clearAll()
            cafeteriaDao.clearAll()
            cafeteriaDao.insertAll(payload.cafeterias)
            floorDao.insertAll(payload.floors)
            mealDao.insertAll(payload.meals)
        }
    }

    override suspend fun seedIfEmpty() {
        if (cafeteriaDao.count() > 0) return
        database.withTransaction {
            val cafeteriaA = cafeteriaDao.upsert(CafeteriaEntity(name = "一食堂", sortOrder = 1))
            val cafeteriaB = cafeteriaDao.upsert(CafeteriaEntity(name = "二食堂", sortOrder = 2))
            val cafeteriaC = cafeteriaDao.upsert(CafeteriaEntity(name = "清真食堂", sortOrder = 3))

            val a1 = floorDao.upsert(FloorEntity(cafeteriaId = cafeteriaA, name = "1楼", sortOrder = 1))
            val a2 = floorDao.upsert(FloorEntity(cafeteriaId = cafeteriaA, name = "2楼", sortOrder = 2))
            val b1 = floorDao.upsert(FloorEntity(cafeteriaId = cafeteriaB, name = "1楼", sortOrder = 1))
            val b2 = floorDao.upsert(FloorEntity(cafeteriaId = cafeteriaB, name = "2楼", sortOrder = 2))
            val c1 = floorDao.upsert(FloorEntity(cafeteriaId = cafeteriaC, name = "1楼", sortOrder = 1))

            mealDao.upsert(MealEntity(floorId = a1, name = "兰州拉面", tags = "面食"))
            mealDao.upsert(MealEntity(floorId = a1, name = "黄焖鸡米饭", tags = "米饭"))
            mealDao.upsert(MealEntity(floorId = a2, name = "麻辣香锅", tags = "重口味"))
            mealDao.upsert(MealEntity(floorId = a2, name = "木桶饭", tags = "下饭"))
            mealDao.upsert(MealEntity(floorId = b1, name = "砂锅", tags = "热乎"))
            mealDao.upsert(MealEntity(floorId = b1, name = "自选快餐", tags = "均衡"))
            mealDao.upsert(MealEntity(floorId = b2, name = "重庆小面", tags = "辣"))
            mealDao.upsert(MealEntity(floorId = b2, name = "煲仔饭", tags = "香"))
            mealDao.upsert(MealEntity(floorId = c1, name = "牛肉面", tags = "清真"))
            mealDao.upsert(MealEntity(floorId = c1, name = "大盘鸡", tags = "清真"))
        }
    }
}
