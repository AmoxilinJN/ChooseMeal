package com.choosemeal.app.ai

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChatMessage(
    val role: String,
    val content: String,
)

@Serializable
data class ChatCompletionRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val temperature: Double = 0.3,
    @SerialName("max_tokens")
    val maxTokens: Int = 1024,
    val stream: Boolean = false,
)

@Serializable
data class ChatCompletionChunk(
    val choices: List<ChunkChoice>,
)

@Serializable
data class ChunkChoice(
    val delta: Delta,
    val index: Int = 0,
    @SerialName("finish_reason")
    val finishReason: String? = null,
)

@Serializable
data class Delta(
    val content: String? = null,
    val role: String? = null,
)

@Serializable
data class ChatCompletionResponse(
    val choices: List<Choice>,
    val usage: Usage? = null,
)

@Serializable
data class Choice(
    val message: ChatMessage,
    val index: Int = 0,
)

@Serializable
data class Usage(
    @SerialName("prompt_tokens")
    val promptTokens: Int = 0,
    @SerialName("completion_tokens")
    val completionTokens: Int = 0,
)

@Serializable
data class AiRecommendation(
    val cafeteria: String?,
    val floor: String?,
    val meal: String?,
    val reason: String,
)

@Serializable
data class AiSearchResult(
    val matchedOptions: List<MatchedOption>,
    val explanation: String,
)

@Serializable
data class MatchedOption(
    val cafeteriaName: String,
    val floorName: String,
    val mealName: String,
    val matchReason: String,
)

@Serializable
data class AiAnalysis(
    val summary: String,
    val insights: List<String>,
)

// New models for redesigned SmartRecommend and AnalyzeHistory

@Serializable
data class AiSmartRecommendResult(
    val insights: String = "",
    val recommendations: List<AiRecommendItem> = emptyList(),
)

@Serializable
data class AiRecommendItem(
    val cafeteria: String = "",
    val floor: String = "",
    val meal: String = "",
    val angle: String = "",
    val reason: String = "",
)

@Serializable
data class AiDietAnalysis(
    val summary: String = "",
    val eatingPatterns: List<String> = emptyList(),
    val suggestions: List<String> = emptyList(),
)
