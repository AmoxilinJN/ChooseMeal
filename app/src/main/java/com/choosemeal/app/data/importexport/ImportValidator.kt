package com.choosemeal.app.data.importexport

object ImportValidator {
    fun validate(jsonDoc: ChooseMealJsonV1): ValidationResult {
        if (jsonDoc.version != 1) {
            return ValidationResult(valid = false, message = "仅支持 version=1 的 JSON")
        }
        if (jsonDoc.cafeterias.map { it.id }.distinct().size != jsonDoc.cafeterias.size) {
            return ValidationResult(valid = false, message = "cafeterias.id 存在重复")
        }
        if (jsonDoc.floors.map { it.id }.distinct().size != jsonDoc.floors.size) {
            return ValidationResult(valid = false, message = "floors.id 存在重复")
        }
        if (jsonDoc.meals.map { it.id }.distinct().size != jsonDoc.meals.size) {
            return ValidationResult(valid = false, message = "meals.id 存在重复")
        }

        val cafeteriaIds = jsonDoc.cafeterias.map { it.id }.toSet()
        val floorIds = jsonDoc.floors.map { it.id }.toSet()

        if (!jsonDoc.floors.all { cafeteriaIds.contains(it.cafeteriaId) }) {
            return ValidationResult(valid = false, message = "floor.cafeteriaId 存在无效引用")
        }
        if (!jsonDoc.meals.all { floorIds.contains(it.floorId) }) {
            return ValidationResult(valid = false, message = "meal.floorId 存在无效引用")
        }
        if (!jsonDoc.meals.all { (it.priceYuan ?: 0) >= 0 }) {
            return ValidationResult(valid = false, message = "meal.priceYuan 必须为非负数")
        }

        return ValidationResult(valid = true, message = "ok")
    }
}
