package com.choosemeal.app.domain.random

import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.random.Random

class WeightedSamplerTest {
    @Test
    fun uniformDistributionIsWithinThreshold() {
        val random = Random(42)
        val weights = listOf(1.0, 1.0, 1.0, 1.0)
        val counts = IntArray(weights.size)

        repeat(1000) {
            val index = WeightedSampler.pickIndex(weights, random.nextDouble())
            counts[index]++
        }

        val average = 1000.0 / weights.size
        counts.forEach { count ->
            val deviation = abs(count - average) / average
            assertTrue("deviation=$deviation should be <= 0.15", deviation <= 0.15)
        }
    }

    @Test
    fun cooldownPenaltyReducesRepeatChance() {
        val random = Random(7)
        val weights = listOf(0.25, 1.0, 1.0, 1.0)
        val counts = IntArray(weights.size)

        repeat(5000) {
            counts[WeightedSampler.pickIndex(weights, random.nextDouble())]++
        }

        val repeated = counts[0].toDouble()
        val othersAverage = counts.drop(1).average()
        assertTrue("repeated=$repeated should be less than half of others average=$othersAverage", repeated < othersAverage * 0.5)
    }

    @Test
    fun zeroWeightFallsBackToUniformByIndex() {
        val index = WeightedSampler.pickIndex(listOf(0.0, 0.0, 0.0), 0.9)
        assertTrue(index in 0..2)
    }
}
