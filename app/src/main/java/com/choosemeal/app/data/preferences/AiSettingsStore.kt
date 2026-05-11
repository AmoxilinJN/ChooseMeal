package com.choosemeal.app.data.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AiSettings(
    val aiEnabled: Boolean = true,
    val apiKey: String = "ark-37771994-5701-46cf-9d2b-af2f42c14b14-6aa65",
    val modelName: String = "doubao-1-5-lite-32k-250115",
)

class AiSettingsStore(context: Context) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "choosemeal_ai_settings",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    private val _settings = MutableStateFlow(loadSettings())

    val settings: StateFlow<AiSettings> = _settings.asStateFlow()

    private fun loadSettings(): AiSettings = AiSettings(
        aiEnabled = prefs.getBoolean(Keys.AI_ENABLED, true),
        apiKey = prefs.getString(Keys.API_KEY, "ark-37771994-5701-46cf-9d2b-af2f42c14b14-6aa65") ?: "ark-37771994-5701-46cf-9d2b-af2f42c14b14-6aa65",
        modelName = prefs.getString(Keys.MODEL_NAME, "doubao-1-5-lite-32k-250115") ?: "doubao-1-5-lite-32k-250115",
    )

    fun setAiEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(Keys.AI_ENABLED, enabled).apply()
        _settings.value = _settings.value.copy(aiEnabled = enabled)
    }

    fun setApiKey(key: String) {
        prefs.edit().putString(Keys.API_KEY, key).apply()
        _settings.value = _settings.value.copy(apiKey = key)
    }

    fun setModelName(model: String) {
        prefs.edit().putString(Keys.MODEL_NAME, model).apply()
        _settings.value = _settings.value.copy(modelName = model)
    }

    private object Keys {
        const val AI_ENABLED = "ai_enabled"
        const val API_KEY = "api_key"
        const val MODEL_NAME = "model_name"
    }
}
