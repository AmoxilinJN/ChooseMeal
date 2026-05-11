package com.choosemeal.app.ai

interface AiService {
    suspend fun smartRecommend(
        options: List<String>,
        history: List<String> = emptyList(),
        statsText: String = "",
        onChunk: ((String) -> Unit)? = null,
    ): Result<AiSmartRecommendResult>

    suspend fun naturalLanguageSearch(
        query: String,
        options: List<String>,
        onChunk: ((String) -> Unit)? = null,
    ): Result<AiSearchResult>

    suspend fun analyzeHistory(
        history: List<String>,
        options: List<String>,
        statsText: String = "",
        onChunk: ((String) -> Unit)? = null,
    ): Result<AiDietAnalysis>
}
