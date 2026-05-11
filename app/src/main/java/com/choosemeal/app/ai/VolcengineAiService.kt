package com.choosemeal.app.ai

import com.choosemeal.app.data.preferences.AiSettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

class VolcengineAiService(
    private val aiSettingsStore: AiSettingsStore,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build(),
) : AiService {

    private val json = Json { ignoreUnknownKeys = true }
    private val mediaType = "application/json".toMediaType()

    companion object {
        const val BASE_URL = "https://ark.cn-beijing.volces.com/api/v3"
        const val CHAT_ENDPOINT = "$BASE_URL/chat/completions"
    }

    private fun getApiKey(): String {
        return aiSettingsStore.settings.value.apiKey
    }

    private fun getModel(): String {
        return aiSettingsStore.settings.value.modelName
    }

    override suspend fun smartRecommend(
        options: List<String>,
        history: List<String>,
        statsText: String,
        onChunk: ((String) -> Unit)?,
    ): Result<AiSmartRecommendResult> = withContext(Dispatchers.IO) {
        runCatching {
            val prompt = AiPrompts.smartRecommendPrompt(options, history, statsText)
            val responseText = callChatApi(prompt, onChunk)
            json.decodeFromString<AiSmartRecommendResult>(responseText)
        }
    }

    override suspend fun naturalLanguageSearch(
        query: String,
        options: List<String>,
        onChunk: ((String) -> Unit)?,
    ): Result<AiSearchResult> = withContext(Dispatchers.IO) {
        runCatching {
            val prompt = AiPrompts.naturalLanguageSearchPrompt(query, options)
            val responseText = callChatApi(prompt, onChunk)
            json.decodeFromString<AiSearchResult>(responseText)
        }
    }

    override suspend fun analyzeHistory(
        history: List<String>,
        options: List<String>,
        statsText: String,
        onChunk: ((String) -> Unit)?,
    ): Result<AiDietAnalysis> = withContext(Dispatchers.IO) {
        runCatching {
            val prompt = AiPrompts.analyzeHistoryPrompt(history, options, statsText)
            val responseText = callChatApi(prompt, onChunk)
            json.decodeFromString<AiDietAnalysis>(responseText)
        }
    }

    private fun callChatApi(
        userPrompt: String,
        onChunk: ((String) -> Unit)? = null,
    ): String {
        val apiKey = getApiKey()
        if (apiKey.isBlank()) throw IllegalStateException("API Key 未配置")

        val requestBody = ChatCompletionRequest(
            model = getModel(),
            messages = listOf(
                ChatMessage(role = "system", content = AiPrompts.systemPrompt()),
                ChatMessage(role = "user", content = userPrompt),
            ),
            stream = onChunk != null,
        )
        val bodyJson = json.encodeToString(ChatCompletionRequest.serializer(), requestBody)

        val request = Request.Builder()
            .url(CHAT_ENDPOINT)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(bodyJson.toRequestBody(mediaType))
            .build()

        val response = client.newCall(request).execute()

        if (!response.isSuccessful) {
            val errorBody = response.body?.string() ?: ""
            throw IllegalStateException("API 请求失败 (${response.code}): ${errorBody.take(200)}")
        }

        return if (onChunk != null) {
            handleStreamingResponse(response, onChunk)
        } else {
            handleNormalResponse(response)
        }
    }

    private fun handleNormalResponse(response: okhttp3.Response): String {
        val responseBody = response.body?.string() ?: throw IllegalStateException("响应为空")
        val chatResponse = json.decodeFromString<ChatCompletionResponse>(responseBody)
        val content = chatResponse.choices.firstOrNull()?.message?.content
            ?: throw IllegalStateException("API 返回内容为空")
        return extractJson(content)
    }

    private fun handleStreamingResponse(
        response: okhttp3.Response,
        onChunk: (String) -> Unit,
    ): String {
        val body = response.body ?: throw IllegalStateException("响应为空")
        val reader = BufferedReader(InputStreamReader(body.byteStream(), Charsets.UTF_8))
        val fullContent = StringBuilder()

        reader.use { r ->
            var line: String?
            while (r.readLine().also { line = it } != null) {
                val l = line ?: continue
                if (!l.startsWith("data: ")) continue
                val data = l.removePrefix("data: ").trim()
                if (data == "[DONE]") break

                val chunk = runCatching {
                    json.decodeFromString<ChatCompletionChunk>(data)
                }.getOrNull() ?: continue

                val delta = chunk.choices.firstOrNull()?.delta?.content ?: ""
                if (delta.isNotEmpty()) {
                    fullContent.append(delta)
                    onChunk(delta)
                }
            }
        }

        return extractJson(fullContent.toString())
    }

    private fun extractJson(text: String): String {
        val trimmed = text.trim()
        val withoutCodeBlock = trimmed
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
        return withoutCodeBlock
    }
}
