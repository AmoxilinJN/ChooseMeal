package com.choosemeal.app.data.local.model

data class MealTripleEntity(
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
