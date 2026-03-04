package com.choosemeal.app.domain.model

import com.choosemeal.app.data.local.entity.CafeteriaEntity
import com.choosemeal.app.data.local.entity.FloorEntity
import com.choosemeal.app.data.local.entity.MealEntity

enum class DecisionMode {
    SPIN,
    DRAW,
}

enum class PriceRangeFilter(val label: String) {
    ANY("价格不限"),
    BUDGET("¥0-15"),
    MID("¥16-25"),
    PREMIUM("¥26+"),
    UNMARKED("未标注价格"),
}

enum class FlavorFilter(val label: String) {
    ANY("口味不限"),
    LIGHT("清淡"),
    BALANCED("适中"),
    STRONG("重口/辣"),
    UNMARKED("未标注口味"),
}

data class DecisionScope(
    val cafeteriaId: Long? = null,
    val floorId: Long? = null,
    val priceRangeFilter: PriceRangeFilter = PriceRangeFilter.ANY,
    val flavorFilter: FlavorFilter = FlavorFilter.ANY,
)

data class MealOption(
    val cafeteriaId: Long,
    val cafeteriaName: String,
    val floorId: Long,
    val floorName: String,
    val mealId: Long,
    val mealName: String,
    val mealTags: String,
)

data class DecisionResult(
    val cafeteria: String,
    val floor: String,
    val meal: String,
    val timestamp: Long,
    val mode: DecisionMode,
    val historyKey: String,
)

data class CooldownPolicy(
    val recentWindowSize: Int = 3,
    val penaltyFactor: Float = 0.25f,
)

data class FloorWithMeals(
    val floor: FloorEntity,
    val meals: List<MealEntity>,
)

data class CafeteriaWithFloors(
    val cafeteria: CafeteriaEntity,
    val floors: List<FloorWithMeals>,
)

fun MealOption.estimatedPriceYuan(): Int? {
    val tags = mealTags.replace("￥", "¥")
    val rangeMatch = Regex("(\\d{1,3})\\s*[-~到]\\s*(\\d{1,3})").find(tags)
    if (rangeMatch != null) {
        val min = rangeMatch.groupValues[1].toIntOrNull()
        val max = rangeMatch.groupValues[2].toIntOrNull()
        if (min != null && max != null) {
            return (min + max) / 2
        }
    }

    val withUnit = Regex("(?<!\\d)(\\d{1,3})(?=\\s*(元|¥))").find(tags)
    if (withUnit != null) {
        return withUnit.groupValues[1].toIntOrNull()
    }

    return null
}

fun MealOption.estimatedFlavorFilter(): FlavorFilter {
    val tags = mealTags.lowercase()
    return when {
        tags.contains("重辣") || tags.contains("特辣") || tags.contains("麻辣") || tags.contains("辣") || tags.contains("重口") -> FlavorFilter.STRONG
        tags.contains("清淡") || tags.contains("少油") || tags.contains("低脂") -> FlavorFilter.LIGHT
        tags.contains("微辣") || tags.contains("咸鲜") || tags.contains("酸甜") || tags.contains("均衡") || tags.contains("适中") -> FlavorFilter.BALANCED
        else -> FlavorFilter.UNMARKED
    }
}

fun MealOption.matchesScope(scope: DecisionScope): Boolean {
    val cafeteriaMatch = scope.cafeteriaId == null || cafeteriaId == scope.cafeteriaId
    val floorMatch = scope.floorId == null || floorId == scope.floorId

    val priceMatch = when (scope.priceRangeFilter) {
        PriceRangeFilter.ANY -> true
        PriceRangeFilter.BUDGET -> estimatedPriceYuan()?.let { it <= 15 } ?: false
        PriceRangeFilter.MID -> estimatedPriceYuan()?.let { it in 16..25 } ?: false
        PriceRangeFilter.PREMIUM -> estimatedPriceYuan()?.let { it >= 26 } ?: false
        PriceRangeFilter.UNMARKED -> estimatedPriceYuan() == null
    }

    val flavor = estimatedFlavorFilter()
    val flavorMatch = when (scope.flavorFilter) {
        FlavorFilter.ANY -> true
        FlavorFilter.UNMARKED -> flavor == FlavorFilter.UNMARKED
        else -> flavor == scope.flavorFilter
    }

    return cafeteriaMatch && floorMatch && priceMatch && flavorMatch
}
