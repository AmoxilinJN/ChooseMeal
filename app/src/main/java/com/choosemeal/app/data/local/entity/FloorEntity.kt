package com.choosemeal.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "floors",
    foreignKeys = [
        ForeignKey(
            entity = CafeteriaEntity::class,
            parentColumns = ["id"],
            childColumns = ["cafeteriaId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("cafeteriaId")],
)
data class FloorEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val cafeteriaId: Long,
    val name: String,
    val sortOrder: Int = 0,
    val enabled: Boolean = true,
)
