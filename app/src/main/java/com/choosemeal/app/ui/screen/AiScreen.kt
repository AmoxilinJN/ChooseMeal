package com.choosemeal.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.choosemeal.app.ai.AiDietAnalysis
import com.choosemeal.app.ai.AiRecommendItem
import com.choosemeal.app.ai.AiSearchResult
import com.choosemeal.app.ai.AiSmartRecommendResult
import com.choosemeal.app.domain.model.MealOption

@Composable
fun AiScreen(
    modifier: Modifier = Modifier,
    aiEnabled: Boolean,
    aiSearchResult: AiSearchResult?,
    aiSearchLoading: Boolean,
    aiStreamingText: String,
    nlSearchQuery: String,
    filteredOptions: List<MealOption>,
    onNlSearchQueryChange: (String) -> Unit,
    onNaturalLanguageSearch: () -> Unit,
    onConsumeSearchResult: () -> Unit,
    aiSmartRecommendResult: AiSmartRecommendResult?,
    aiSmartRecommendLoading: Boolean,
    aiSmartRecommendStreamingText: String,
    onSmartRecommend: () -> Unit,
    onConsumeSmartRecommendResult: () -> Unit,
    onRecordFromRecommend: (AiRecommendItem) -> Unit,
    aiDietAnalysis: AiDietAnalysis?,
    aiAnalysisLoading: Boolean,
    aiAnalysisStreamingText: String,
    recentHistoryCount: Int,
    onAnalyzeHistory: () -> Unit,
    onConsumeDietAnalysis: () -> Unit,
) {
    if (!aiEnabled) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text("AI 功能未启用", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            Text(
                "请在设置中启用 AI 并配置 API Key",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("AI 智能助手", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

        // 1. Smart Recommend
        SmartRecommendCard(
            result = aiSmartRecommendResult,
            isLoading = aiSmartRecommendLoading,
            streamingText = aiSmartRecommendStreamingText,
            hasOptions = filteredOptions.isNotEmpty(),
            onTrigger = {
                onConsumeSmartRecommendResult()
                onSmartRecommend()
            },
            onRecord = onRecordFromRecommend,
        )

        // 2. Natural Language Search
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Outlined.Search, contentDescription = null)
                    Text("自然语言搜索", fontWeight = FontWeight.SemiBold)
                }

                OutlinedTextField(
                    value = nlSearchQuery,
                    onValueChange = onNlSearchQueryChange,
                    label = { Text("你想吃什么？") },
                    placeholder = { Text("例：想吃清淡便宜的") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !aiSearchLoading,
                )

                OutlinedButton(
                    onClick = {
                        onConsumeSearchResult()
                        onNaturalLanguageSearch()
                    },
                    enabled = !aiSearchLoading && nlSearchQuery.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("搜索")
                }

                if (aiSearchLoading) {
                    LoadingWithStream(aiStreamingText)
                }

                aiSearchResult?.let { result ->
                    HorizontalDivider()
                    Text("搜索结果", fontWeight = FontWeight.SemiBold)
                    Text(result.explanation, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    result.matchedOptions.forEach { option ->
                        Text("  ${option.cafeteriaName} > ${option.floorName} > ${option.mealName}")
                        Text("    ${option.matchReason}", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        // 3. Diet Analysis
        AnalyzeHistoryCard(
            result = aiDietAnalysis,
            isLoading = aiAnalysisLoading,
            streamingText = aiAnalysisStreamingText,
            hasHistory = recentHistoryCount > 0,
            onTrigger = {
                onConsumeDietAnalysis()
                onAnalyzeHistory()
            },
        )
    }
}

@Composable
private fun SmartRecommendCard(
    result: AiSmartRecommendResult?,
    isLoading: Boolean,
    streamingText: String,
    hasOptions: Boolean,
    onTrigger: () -> Unit,
    onRecord: (AiRecommendItem) -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Outlined.AutoAwesome, contentDescription = null)
                Text("智能推荐", fontWeight = FontWeight.SemiBold)
            }
            Text("AI 根据你的口味和饮食记录，从多个角度为你推荐今日菜品", style = MaterialTheme.typography.bodySmall)

            Button(
                onClick = onTrigger,
                enabled = !isLoading && hasOptions,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("AI 帮我推荐")
            }

            if (!hasOptions) {
                Text("当前无可选项，请先在「数据」页面添加食堂数据",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline)
            }

            if (isLoading) {
                LoadingWithStream(streamingText)
            }

            result?.let { res ->
                HorizontalDivider()
                if (res.insights.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f),
                                RoundedCornerShape(10.dp),
                            )
                            .padding(12.dp),
                    ) {
                        Text(res.insights, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    }
                    Spacer(Modifier.height(4.dp))
                }

                res.recommendations.forEach { item ->
                    RecommendItemCard(item = item, onRecord = { onRecord(item) })
                }

                if (res.recommendations.isEmpty()) {
                    Text("暂无推荐结果", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline)
                }
            }
        }
    }
}

@Composable
private fun RecommendItemCard(item: AiRecommendItem, onRecord: () -> Unit) {
    val angleColor = when (item.angle) {
        "合你口味" -> MaterialTheme.colorScheme.primaryContainer
        "换个口味" -> Color(0xFFFFF3E0)
        "高性价比" -> Color(0xFFE8F5E9)
        "营养均衡" -> Color(0xFFE3F2FD)
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val angleTextColor = when (item.angle) {
        "合你口味" -> MaterialTheme.colorScheme.onPrimaryContainer
        "换个口味" -> Color(0xFFE65100)
        "高性价比" -> Color(0xFF2E7D32)
        "营养均衡" -> Color(0xFF1565C0)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SuggestionChip(
                    onClick = {},
                    label = { Text(item.angle, style = MaterialTheme.typography.labelSmall) },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = angleColor,
                        labelColor = angleTextColor,
                    ),
                    border = null,
                    shape = RoundedCornerShape(6.dp),
                )
                Text(item.meal, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
            Text(
                "${item.cafeteria} · ${item.floor}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
            )
            Text(item.reason, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)

            OutlinedButton(
                onClick = onRecord,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
            ) {
                Icon(Icons.Outlined.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("今天就吃这个")
            }
        }
    }
}

@Composable
private fun AnalyzeHistoryCard(
    result: AiDietAnalysis?,
    isLoading: Boolean,
    streamingText: String,
    hasHistory: Boolean,
    onTrigger: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Outlined.Analytics, contentDescription = null)
                Text("饮食分析", fontWeight = FontWeight.SemiBold)
            }
            Text("AI 分析你的食堂饮食记录，给出个性化洞察和改善建议", style = MaterialTheme.typography.bodySmall)

            if (!hasHistory) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            RoundedCornerShape(10.dp),
                        )
                        .padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("暂无饮食记录，先去「美食地图」吃几顿再回来分析吧",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline)
                }
            } else {
                Button(
                    onClick = onTrigger,
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("分析我的饮食记录")
                }
            }

            if (isLoading) {
                LoadingWithStream(streamingText)
            }

            result?.let { analysis ->
                HorizontalDivider()

                // Summary
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f),
                            RoundedCornerShape(10.dp),
                        )
                        .padding(12.dp),
                ) {
                    Text(analysis.summary, style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium)
                }

                // Eating Patterns
                if (analysis.eatingPatterns.isNotEmpty()) {
                    Text("饮食模式", fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary)
                    analysis.eatingPatterns.forEach { pattern ->
                        Row(modifier = Modifier.padding(start = 4.dp)) {
                            Text("  • ", color = MaterialTheme.colorScheme.outline)
                            Text(pattern, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                // Suggestions
                if (analysis.suggestions.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text("改善建议", fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary)
                    analysis.suggestions.forEachIndexed { index, suggestion ->
                        Row(modifier = Modifier.padding(start = 4.dp)) {
                            Text("  ${index + 1}. ", color = MaterialTheme.colorScheme.outline)
                            Text(suggestion, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                if (analysis.eatingPatterns.isEmpty() && analysis.suggestions.isEmpty()) {
                    Text("分析完成，暂无特别建议", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline)
                }
            }
        }
    }
}

@Composable
private fun LoadingWithStream(streamingText: String) {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
    if (streamingText.isNotBlank()) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            ),
            shape = RoundedCornerShape(12.dp),
        ) {
            Text(
                streamingText,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(12.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
