package com.choosemeal.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "meals",
    foreignKeys = [
        ForeignKey(
            entity = FloorEntity::class,
            parentColumns = ["id"],
            childColumns = ["floorId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("floorId")],
)
data class MealEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val floorId: Long,
    val name: String,
    val tags: String = "",
    val flavor: String = "",
    val priceYuan: Int? = null,
    val enabled: Boolean = true,
)
