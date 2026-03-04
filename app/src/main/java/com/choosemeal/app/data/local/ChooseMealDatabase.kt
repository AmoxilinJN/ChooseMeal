package com.choosemeal.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.choosemeal.app.data.local.dao.CafeteriaDao
import com.choosemeal.app.data.local.dao.FloorDao
import com.choosemeal.app.data.local.dao.MealDao
import com.choosemeal.app.data.local.entity.CafeteriaEntity
import com.choosemeal.app.data.local.entity.FloorEntity
import com.choosemeal.app.data.local.entity.MealEntity

@Database(
    entities = [CafeteriaEntity::class, FloorEntity::class, MealEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class ChooseMealDatabase : RoomDatabase() {
    abstract fun cafeteriaDao(): CafeteriaDao
    abstract fun floorDao(): FloorDao
    abstract fun mealDao(): MealDao

    companion object {
        fun create(context: Context): ChooseMealDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                ChooseMealDatabase::class.java,
                "choosemeal.db",
            ).fallbackToDestructiveMigration().build()
        }
    }
}
