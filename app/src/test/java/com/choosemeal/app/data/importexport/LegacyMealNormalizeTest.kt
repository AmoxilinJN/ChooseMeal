package com.choosemeal.app.data.importexport

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LegacyMealNormalizeTest {
    @Test
    fun normalizeLegacyTagsExtractsFlavorAndPrice() {
        val meal = JsonMeal(
            id = 1,
            floorId = 11,
            name = "黄焖鸡",
            tags = "快餐,适中,￥18",
            enabled = true,
        )

        val normalized = normalizeLegacyMealFields(meal)
        assertEquals("快餐", normalized.tags)
        assertEquals("适中", normalized.flavor)
        assertEquals(18, normalized.priceYuan)
    }

    @Test
    fun normalizeKeepsExplicitFlavorAndPrice() {
        val meal = JsonMeal(
            id = 1,
            floorId = 11,
            name = "牛肉面",
            tags = "面食,清淡,￥16",
            flavor = "清淡",
            priceYuan = 16,
            enabled = true,
        )

        val normalized = normalizeLegacyMealFields(meal)
        assertEquals("面食", normalized.tags)
        assertEquals("清淡", normalized.flavor)
        assertEquals(16, normalized.priceYuan)
    }

    @Test
    fun normalizeWithoutLegacyTokensKeepsTags() {
        val meal = JsonMeal(
            id = 1,
            floorId = 11,
            name = "轻食沙拉",
            tags = "轻食,沙拉",
            enabled = true,
        )

        val normalized = normalizeLegacyMealFields(meal)
        assertEquals("轻食,沙拉", normalized.tags)
        assertEquals("", normalized.flavor)
        assertNull(normalized.priceYuan)
    }
}
