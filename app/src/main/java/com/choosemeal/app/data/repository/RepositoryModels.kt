package com.choosemeal.app.data.repository

import com.choosemeal.app.data.local.entity.CafeteriaEntity
import com.choosemeal.app.data.local.entity.FloorEntity
import com.choosemeal.app.data.local.entity.MealEntity

data class DataSnapshot(
    val cafeterias: List<CafeteriaEntity>,
    val floors: List<FloorEntity>,
    val meals: List<MealEntity>,
)

data class ImportPayload(
    val cafeterias: List<CafeteriaEntity>,
    val floors: List<FloorEntity>,
    val meals: List<MealEntity>,
)
