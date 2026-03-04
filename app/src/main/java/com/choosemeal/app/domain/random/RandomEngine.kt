package com.choosemeal.app.domain.random

import com.choosemeal.app.data.preferences.SettingsStore
import com.choosemeal.app.data.repository.MealRepository
import com.choosemeal.app.domain.model.CooldownPolicy
import com.choosemeal.app.domain.model.DecisionMode
import com.choosemeal.app.domain.model.DecisionResult
import com.choosemeal.app.domain.model.DecisionScope
import com.choosemeal.app.domain.model.MealOption
import com.choosemeal.app.domain.model.matchesScope
import kotlinx.coroutines.flow.first
import kotlin.random.Random

interface RandomEngine {
    suspend fun spinDecision(scope: DecisionScope, mode: DecisionMode = DecisionMode.SPIN): DecisionResult?
    suspend fun drawDecision(scope: DecisionScope, mode: DecisionMode = DecisionMode.DRAW): DecisionResult?
}

object WeightedSampler {
    fun pickIndex(weights: List<Double>, randomValue: Double): Int {
        val total = weights.sum()
        if (weights.isEmpty()) return -1
        if (total <= 0.0) return (randomValue * weights.size).toInt().coerceIn(0, weights.lastIndex)
        var cursor = randomValue * total
        for ((index, weight) in weights.withIndex()) {
            cursor -= weight
            if (cursor <= 0.0) return index
        }
        return weights.lastIndex
    }
}

class DefaultRandomEngine(
    private val repository: MealRepository,
    private val settingsStore: SettingsStore,
    private val random: Random = Random(System.currentTimeMillis()),
) : RandomEngine {

    override suspend fun spinDecision(scope: DecisionScope, mode: DecisionMode): DecisionResult? {
        return pick(scope = scope, mode = mode)
    }

    override suspend fun drawDecision(scope: DecisionScope, mode: DecisionMode): DecisionResult? {
        return pick(scope = scope, mode = mode)
    }

    private suspend fun pick(scope: DecisionScope, mode: DecisionMode): DecisionResult? {
        val options = repository.observeEnabledOptions().first()
            .filter { it.matchesScope(scope) }

        if (options.isEmpty()) return null

        val settings = settingsStore.settings.first()
        val policy = CooldownPolicy(
            recentWindowSize = settings.recentWindowSize,
            penaltyFactor = settings.penaltyFactor,
        )

        val history = settings.recentHistory.take(policy.recentWindowSize.coerceAtLeast(0)).toSet()
        val weights = options.map { option ->
            if (settings.cooldownEnabled && history.contains(option.historyKey())) {
                policy.penaltyFactor.toDouble().coerceIn(0.01, 1.0)
            } else {
                1.0
            }
        }

        val index = WeightedSampler.pickIndex(weights, random.nextDouble())
        if (index !in options.indices) return null

        val selected = options[index]
        val result = DecisionResult(
            cafeteria = selected.cafeteriaName,
            floor = selected.floorName,
            meal = selected.mealName,
            timestamp = System.currentTimeMillis(),
            mode = mode,
            historyKey = selected.historyKey(),
        )
        settingsStore.appendHistoryKey(result.historyKey, policy.recentWindowSize.coerceAtLeast(1))
        return result
    }

    private fun MealOption.historyKey(): String = "${cafeteriaId}:${floorId}:${mealId}"
}
