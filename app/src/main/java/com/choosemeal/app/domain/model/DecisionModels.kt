package com.choosemeal.app.domain.model

import com.choosemeal.app.data.local.entity.CafeteriaEntity
import com.choosemeal.app.data.local.entity.FloorEntity
import com.choosemeal.app.data.local.entity.MealEntity

enum class DecisionMode {
    SPIN,
    DRAW,
}

data class DecisionScope(
    val cafeteriaId: Long? = null,
    val floorId: Long? = null,
    val priceMinYuan: Int? = null,
    val priceMaxYuan: Int? = null,
    val flavor: String? = null,
)

data class MealOption(
    val cafeteriaId: Long,
    val cafeteriaName: String,
    val floorId: Long,
    val floorName: String,
    val mealId: Long,
    val mealName: String,
    val mealTags: String,
    val mealFlavor: String,
    val mealPriceYuan: Int?,
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

fun MealOption.matchesScope(scope: DecisionScope): Boolean {
    val cafeteriaMatch = scope.cafeteriaId == null || cafeteriaId == scope.cafeteriaId
    val floorMatch = scope.floorId == null || floorId == scope.floorId

    val normalizedFlavor = mealFlavor.trim()
    val requestedFlavor = scope.flavor?.trim().orEmpty()
    val flavorMatch = requestedFlavor.isBlank() || normalizedFlavor.equals(requestedFlavor, ignoreCase = true)

    val lowerRaw = scope.priceMinYuan
    val upperRaw = scope.priceMaxYuan
    val effectiveLower = when {
        lowerRaw != null && upperRaw != null -> minOf(lowerRaw, upperRaw)
        else -> lowerRaw
    }
    val effectiveUpper = when {
        lowerRaw != null && upperRaw != null -> maxOf(lowerRaw, upperRaw)
        else -> upperRaw
    }

    val priceMatch = when {
        effectiveLower == null && effectiveUpper == null -> true
        mealPriceYuan == null -> false
        effectiveLower != null && mealPriceYuan < effectiveLower -> false
        effectiveUpper != null && mealPriceYuan > effectiveUpper -> false
        else -> true
    }

    return cafeteriaMatch && floorMatch && flavorMatch && priceMatch
}
