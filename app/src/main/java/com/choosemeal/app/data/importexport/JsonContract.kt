package com.choosemeal.app.data.importexport

import android.net.Uri
import kotlinx.serialization.Serializable

@Serializable
data class ChooseMealJsonV1(
    val version: Int = 1,
    val cafeterias: List<JsonCafeteria> = emptyList(),
    val floors: List<JsonFloor> = emptyList(),
    val meals: List<JsonMeal> = emptyList(),
)

@Serializable
data class JsonCafeteria(
    val id: Long,
    val name: String,
    val sortOrder: Int,
    val enabled: Boolean,
)

@Serializable
data class JsonFloor(
    val id: Long,
    val cafeteriaId: Long,
    val name: String,
    val sortOrder: Int,
    val enabled: Boolean,
)

@Serializable
data class JsonMeal(
    val id: Long,
    val floorId: Long,
    val name: String,
    val tags: String = "",
    val flavor: String = "",
    val priceYuan: Int? = null,
    val enabled: Boolean,
)

data class ValidationResult(
    val valid: Boolean,
    val message: String,
)

data class ImportSummary(
    val success: Boolean,
    val message: String,
    val cafeteriaCount: Int = 0,
    val floorCount: Int = 0,
    val mealCount: Int = 0,
)

data class ExportSummary(
    val success: Boolean,
    val message: String,
    val cafeteriaCount: Int = 0,
    val floorCount: Int = 0,
    val mealCount: Int = 0,
)

data class ShareSummary(
    val success: Boolean,
    val message: String,
    val uri: Uri? = null,
    val fileName: String = "",
    val cafeteriaCount: Int = 0,
    val floorCount: Int = 0,
    val mealCount: Int = 0,
)
