package com.choosemeal.app.data.importexport

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ImportValidatorTest {
    @Test
    fun validContractPasses() {
        val doc = ChooseMealJsonV1(
            version = 1,
            cafeterias = listOf(JsonCafeteria(1, "一食堂", 1, true)),
            floors = listOf(JsonFloor(10, 1, "1楼", 1, true)),
            meals = listOf(JsonMeal(id = 100, floorId = 10, name = "拉面", tags = "面食", enabled = true)),
        )

        val result = ImportValidator.validate(doc)
        assertTrue(result.valid)
    }

    @Test
    fun invalidFloorReferenceFails() {
        val doc = ChooseMealJsonV1(
            version = 1,
            cafeterias = listOf(JsonCafeteria(1, "一食堂", 1, true)),
            floors = listOf(JsonFloor(10, 99, "1楼", 1, true)),
            meals = emptyList(),
        )

        val result = ImportValidator.validate(doc)
        assertFalse(result.valid)
    }

    @Test
    fun duplicateMealIdFails() {
        val doc = ChooseMealJsonV1(
            version = 1,
            cafeterias = listOf(JsonCafeteria(1, "一食堂", 1, true)),
            floors = listOf(JsonFloor(10, 1, "1楼", 1, true)),
            meals = listOf(
                JsonMeal(id = 100, floorId = 10, name = "拉面", tags = "面食", enabled = true),
                JsonMeal(id = 100, floorId = 10, name = "米饭", tags = "米饭", enabled = true),
            ),
        )

        val result = ImportValidator.validate(doc)
        assertFalse(result.valid)
    }

    @Test
    fun unsupportedVersionFails() {
        val doc = ChooseMealJsonV1(version = 2)
        val result = ImportValidator.validate(doc)
        assertFalse(result.valid)
    }

    @Test
    fun negativePriceFails() {
        val doc = ChooseMealJsonV1(
            version = 1,
            cafeterias = listOf(JsonCafeteria(1, "一食堂", 1, true)),
            floors = listOf(JsonFloor(10, 1, "1楼", 1, true)),
            meals = listOf(JsonMeal(id = 100, floorId = 10, name = "拉面", tags = "面食", flavor = "清淡", priceYuan = -1, enabled = true)),
        )

        val result = ImportValidator.validate(doc)
        assertFalse(result.valid)
    }
}
