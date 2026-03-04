package com.choosemeal.app.data.importexport

import android.content.Context
import android.net.Uri
import com.choosemeal.app.data.local.entity.CafeteriaEntity
import com.choosemeal.app.data.local.entity.FloorEntity
import com.choosemeal.app.data.local.entity.MealEntity
import com.choosemeal.app.data.repository.ImportPayload
import com.choosemeal.app.data.repository.MealRepository
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

interface ImportExportService {
    suspend fun exportToJson(uri: Uri): ExportSummary
    suspend fun importFromJson(uri: Uri): ImportSummary
    fun validate(jsonDoc: ChooseMealJsonV1): ValidationResult
}

class LocalImportExportService(
    private val context: Context,
    private val repository: MealRepository,
) : ImportExportService {

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        prettyPrintIndent = "  "
    }

    override suspend fun exportToJson(uri: Uri): ExportSummary {
        return runCatching {
            val snapshot = repository.snapshot()
            val contract = ChooseMealJsonV1(
                version = 1,
                cafeterias = snapshot.cafeterias.map {
                    JsonCafeteria(
                        id = it.id,
                        name = it.name,
                        sortOrder = it.sortOrder,
                        enabled = it.enabled,
                    )
                },
                floors = snapshot.floors.map {
                    JsonFloor(
                        id = it.id,
                        cafeteriaId = it.cafeteriaId,
                        name = it.name,
                        sortOrder = it.sortOrder,
                        enabled = it.enabled,
                    )
                },
                meals = snapshot.meals.map {
                    JsonMeal(
                        id = it.id,
                        floorId = it.floorId,
                        name = it.name,
                        tags = it.tags,
                        enabled = it.enabled,
                    )
                },
            )
            context.contentResolver.openOutputStream(uri)?.use { stream ->
                stream.write(json.encodeToString(contract).toByteArray(Charsets.UTF_8))
            } ?: error("无法写入目标文件")
            ExportSummary(
                success = true,
                message = "导出成功",
                cafeteriaCount = contract.cafeterias.size,
                floorCount = contract.floors.size,
                mealCount = contract.meals.size,
            )
        }.getOrElse {
            ExportSummary(success = false, message = "导出失败: ${it.message}")
        }
    }

    override suspend fun importFromJson(uri: Uri): ImportSummary {
        return runCatching {
            val raw = context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                ?: error("无法读取文件")
            val doc = json.decodeFromString<ChooseMealJsonV1>(raw)
            val validation = validate(doc)
            if (!validation.valid) {
                return ImportSummary(success = false, message = validation.message)
            }

            repository.replaceAllFromImport(
                ImportPayload(
                    cafeterias = doc.cafeterias.map {
                        CafeteriaEntity(
                            id = it.id,
                            name = it.name,
                            sortOrder = it.sortOrder,
                            enabled = it.enabled,
                        )
                    },
                    floors = doc.floors.map {
                        FloorEntity(
                            id = it.id,
                            cafeteriaId = it.cafeteriaId,
                            name = it.name,
                            sortOrder = it.sortOrder,
                            enabled = it.enabled,
                        )
                    },
                    meals = doc.meals.map {
                        MealEntity(
                            id = it.id,
                            floorId = it.floorId,
                            name = it.name,
                            tags = it.tags,
                            enabled = it.enabled,
                        )
                    },
                ),
            )
            ImportSummary(
                success = true,
                message = "导入成功",
                cafeteriaCount = doc.cafeterias.size,
                floorCount = doc.floors.size,
                mealCount = doc.meals.size,
            )
        }.getOrElse {
            ImportSummary(success = false, message = "导入失败: ${it.message}")
        }
    }

    override fun validate(jsonDoc: ChooseMealJsonV1): ValidationResult {
        return ImportValidator.validate(jsonDoc)
    }
}
