package com.choosemeal.app.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.dataStore by preferencesDataStore(name = "choosemeal_settings")

data class UserSettings(
    val cooldownEnabled: Boolean = true,
    val recentWindowSize: Int = 3,
    val penaltyFactor: Float = 0.25f,
    val animationsEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val recentHistory: List<String> = emptyList(),
)

class SettingsStore(private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true }

    private object Keys {
        val cooldownEnabled = booleanPreferencesKey("cooldown_enabled")
        val recentWindowSize = intPreferencesKey("recent_window_size")
        val penaltyFactor = floatPreferencesKey("penalty_factor")
        val animationsEnabled = booleanPreferencesKey("animations_enabled")
        val hapticsEnabled = booleanPreferencesKey("haptics_enabled")
        val recentHistoryJson = stringPreferencesKey("recent_history_json")
    }

    val settings: Flow<UserSettings> = context.dataStore.data.map { preferences ->
        UserSettings(
            cooldownEnabled = preferences[Keys.cooldownEnabled] ?: true,
            recentWindowSize = (preferences[Keys.recentWindowSize] ?: 3).coerceIn(0, 20),
            penaltyFactor = (preferences[Keys.penaltyFactor] ?: 0.25f).coerceIn(0f, 1f),
            animationsEnabled = preferences[Keys.animationsEnabled] ?: true,
            hapticsEnabled = preferences[Keys.hapticsEnabled] ?: true,
            recentHistory = parseHistory(preferences[Keys.recentHistoryJson]),
        )
    }

    suspend fun setCooldownEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.cooldownEnabled] = enabled }
    }

    suspend fun setRecentWindowSize(size: Int) {
        val normalized = size.coerceIn(0, 20)
        context.dataStore.edit { prefs ->
            prefs[Keys.recentWindowSize] = normalized
            val existing = parseHistory(prefs[Keys.recentHistoryJson])
            prefs[Keys.recentHistoryJson] = json.encodeToString(existing.take(normalized.coerceAtLeast(1)))
        }
    }

    suspend fun setAnimationsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.animationsEnabled] = enabled }
    }

    suspend fun setHapticsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.hapticsEnabled] = enabled }
    }

    suspend fun appendHistoryKey(key: String, maxWindow: Int) {
        val keep = maxWindow.coerceAtLeast(1)
        context.dataStore.edit { prefs ->
            val old = parseHistory(prefs[Keys.recentHistoryJson])
            val newHistory = (listOf(key) + old.filterNot { it == key }).take(keep)
            prefs[Keys.recentHistoryJson] = json.encodeToString(newHistory)
        }
    }

    suspend fun clearHistory() {
        context.dataStore.edit { it[Keys.recentHistoryJson] = json.encodeToString(emptyList<String>()) }
    }

    private fun parseHistory(raw: String?): List<String> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching { json.decodeFromString<List<String>>(raw) }.getOrDefault(emptyList())
    }
}
