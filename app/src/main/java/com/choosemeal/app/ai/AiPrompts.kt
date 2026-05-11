package com.choosemeal.app.ai

object AiPrompts {

    fun systemPrompt(): String = """
你是 ChooseMeal 智能决策助手，帮助大学生选择食堂/楼层/伙食的 AI。
根据可选伙食列表和用户偏好，做出智能推荐、自然语言搜索、或饮食分析。

回答要求：
1. 简洁、友好、实用
2. 推荐必须基于提供的可选列表，不编造不存在的选项
3. 用中文回答
4. 严格按照要求的 JSON 格式输出，不用 markdown 代码块
""".trimIndent()

    fun formatEatingStats(stats: UserEatingStats): String {
        if (stats.totalRecords == 0) return "暂无足够饮食数据"
        val lines = mutableListOf<String>()
        if (stats.cafeteriaFreq.isNotEmpty()) {
            val top = stats.cafeteriaFreq.maxByOrNull { it.value }
            lines.add("- 食堂分布：${stats.cafeteriaFreq.entries.joinToString { "${it.key}(${it.value}次)" }}；最常去${top!!.key}")
        }
        if (stats.tagFreq.isNotEmpty()) {
            lines.add("- 菜品类型偏好：${stats.tagFreq.entries.joinToString { "${it.key}(${it.value}次)" }}")
        }
        if (stats.flavorFreq.isNotEmpty()) {
            lines.add("- 口味偏好：${stats.flavorFreq.entries.joinToString { "${it.key}(${it.value}次)" }}")
        }
        if (stats.avgSpendYuan != null) {
            lines.add("- 平均每餐消费：${stats.avgSpendYuan}元")
        }
        if (stats.consecutiveTags != null) {
            lines.add("- 注意：${stats.consecutiveTags}")
        }
        if (stats.timeDistribution.isNotEmpty()) {
            val periodOrder = listOf("早餐", "午餐", "晚餐", "夜宵")
            val sorted = stats.timeDistribution.entries.sortedBy { periodOrder.indexOf(it.key).let { if (it == -1) 99 else it } }
            lines.add("- 用餐时段分布：${sorted.joinToString { "${it.key}: ${it.value}次" }}")
        }
        return lines.joinToString("\n")
    }

    fun smartRecommendPrompt(
        enrichedOptions: List<String>,
        history: List<String>,
        statsText: String,
    ): String = """
## 用户饮食画像
$statsText

## 最近吃过的菜品（务必避免重复推荐）
${if (history.isNotEmpty()) history.joinToString("\n") { "  - $it" } else "暂无历史记录"}

## 今日可选菜品（含标签、口味、价格）
${enrichedOptions.joinToString("\n") { "  - $it" }}

## 任务
从今日可选菜品中推荐 2-3 个，每个推荐从不同角度出发：

角度说明：
- "合你口味"：匹配用户的口味偏好和常去食堂
- "换个口味"：与用户最近饮食模式不同，尝试新类型/口味/食堂
- "高性价比"：价格不高于用户平均消费，品质不错
- "营养均衡"：如果用户近期偏食（如连续面食/重口味），推荐互补类型

## 规则（必须遵守）
1. 绝不推荐「最近吃过」列表中的菜品
2. 每个推荐必须是今日可选列表中的真实菜品，食堂名和菜品名要完全一致
3. 推荐理由要具体到菜品特色，不要泛泛而谈
4. 至少包含 "合你口味" + 一个其他角度
5. 角度不要重复

## 输出格式（纯 JSON，无 markdown 标记）
{
  "insights": "结合用户饮食画像的一句话分析",
  "recommendations": [
    {"cafeteria": "食堂名称", "floor": "楼层名称", "meal": "菜品名称", "angle": "合你口味|换个口味|高性价比|营养均衡", "reason": "具体推荐理由（15字以内）"}
  ]
}
""".trimIndent()

    fun naturalLanguageSearchPrompt(
        query: String,
        options: List<String>,
    ): String = """
用户需求：$query

当前可选的伙食列表：
${options.joinToString("\n") { "  - $it" }}

请根据用户的需求，从可选列表中筛选出最匹配的选项（最多3个）。
以 JSON 格式返回，不要包含 markdown 代码块标记：
{
  "matchedOptions": [
    {"cafeteriaName": "食堂名", "floorName": "楼层名", "mealName": "菜品名", "matchReason": "匹配原因"}
  ],
  "explanation": "简要说明筛选结果"
}
""".trimIndent()

    fun analyzeHistoryPrompt(
        enrichedHistory: List<String>,
        enrichedOptions: List<String>,
        statsText: String,
    ): String = """
## 用户近20餐饮食记录（含标签、口味、价格）
${enrichedHistory.joinToString("\n") { "  - $it" }}

## 饮食统计摘要
$statsText

## 今日可选菜品
${enrichedOptions.joinToString("\n") { "  - $it" }}

## 任务
基于以上真实数据，给出具体、个性化、可操作的饮食分析和改善建议。

## 洞察要求
1. 饮食模式总结：用数据说话，如 "你在XX食堂消费了X%的餐次"
2. 习惯分析：指出均衡或偏食的问题，如 "最近X天连续吃了X类型"
3. 改善建议：必须基于今日可选列表，给出具体到食堂名称和菜品名称的建议
4. 禁止泛泛而谈（如只说"多吃蔬菜"而不说具体哪家食堂哪个窗口有什么菜）
5. 每个建议都要有可执行性——用户看完就知道今天去哪吃、吃什么

## 输出格式（纯 JSON，无 markdown 标记）
{
  "summary": "一句话总结用户总体饮食特征",
  "eatingPatterns": [
    "用真实数据描述的具体饮食模式1",
    "用真实数据描述的具体饮食模式2",
    "用真实数据描述的具体饮食模式3"
  ],
  "suggestions": [
    "引用今日可选列表中具体食堂和菜品名的可执行建议1",
    "引用今日可选列表中具体食堂和菜品名的可执行建议2",
    "引用今日可选列表中具体食堂和菜品名的可执行建议3"
  ]
}
""".trimIndent()
}

data class UserEatingStats(
    val totalRecords: Int = 0,
    val cafeteriaFreq: Map<String, Int> = emptyMap(),
    val tagFreq: Map<String, Int> = emptyMap(),
    val flavorFreq: Map<String, Int> = emptyMap(),
    val avgSpendYuan: Int? = null,
    val consecutiveTags: String? = null,
    val timeDistribution: Map<String, Int> = emptyMap(),
)
