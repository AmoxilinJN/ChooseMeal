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
