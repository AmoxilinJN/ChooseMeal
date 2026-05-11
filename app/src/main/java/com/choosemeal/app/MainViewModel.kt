package com.choosemeal.app

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.choosemeal.app.data.importexport.CommunityConfigEntry
import com.choosemeal.app.data.importexport.ImportSummary
import com.choosemeal.app.data.local.entity.CafeteriaEntity
import com.choosemeal.app.data.local.entity.FloorEntity
import com.choosemeal.app.data.local.entity.MealEntity
import com.choosemeal.app.data.preferences.UserSettings
import com.choosemeal.app.ai.AiDietAnalysis
import com.choosemeal.app.ai.AiPrompts
import com.choosemeal.app.ai.AiRecommendItem
import com.choosemeal.app.ai.AiSearchResult
import com.choosemeal.app.ai.AiSmartRecommendResult
import com.choosemeal.app.ai.UserEatingStats
import com.choosemeal.app.data.preferences.AiSettings
import com.choosemeal.app.data.preferences.HistoryRecord
import com.choosemeal.app.data.preferences.formatTimestampForAI
import com.choosemeal.app.domain.model.DecisionMode
import com.choosemeal.app.domain.model.DecisionResult
import com.choosemeal.app.domain.model.DecisionScope
import com.choosemeal.app.domain.model.MealOption
import com.choosemeal.app.domain.model.matchesScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.ExperimentalCoroutinesApi

data class SharePayload(
    val uri: Uri,
    val fileName: String,
)

data class InstallApkPayload(
    val uri: Uri,
    val version: String,
)

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val container = (application as ChooseMealApplication).container
    private val repository = container.repository
    private val randomEngine = container.randomEngine
    private val settingsStore = container.settingsStore
    private val aiSettingsStore = container.aiSettingsStore
    private val aiService = container.aiService
    private val importExportService = container.importExportService
    private val communityConfigService = container.communityConfigService
    private val appUpdateService = container.appUpdateService

    val settings: StateFlow<UserSettings> = settingsStore.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = UserSettings(),
    )

    val aiSettings: StateFlow<AiSettings> = aiSettingsStore.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AiSettings(),
    )

    val cafeterias = repository.observeCafeterias().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    private val _randomCafeteriaFilter = MutableStateFlow<Long?>(null)
    val randomCafeteriaFilter = _randomCafeteriaFilter.asStateFlow()

    private val _randomFloorFilter = MutableStateFlow<Long?>(null)
    val randomFloorFilter = _randomFloorFilter.asStateFlow()

    private val _randomPriceMinInput = MutableStateFlow("")
    val randomPriceMinInput = _randomPriceMinInput.asStateFlow()

    private val _randomPriceMaxInput = MutableStateFlow("")
    val randomPriceMaxInput = _randomPriceMaxInput.asStateFlow()

    private val _randomFlavorFilter = MutableStateFlow<String?>(null)
    val randomFlavorFilter = _randomFlavorFilter.asStateFlow()

    val randomFloors = _randomCafeteriaFilter.flatMapLatest { repository.observeFloors(it) }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    val hierarchy = repository.observeHierarchy().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    val allFloors = repository.observeFloors(null).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    val flavorOptions = repository.observeMeals(null)
        .map { meals ->
            meals.map { it.flavor.trim() }
                .filter { it.isNotBlank() }
                .distinct()
                .sorted()
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    private val _manageCafeteriaId = MutableStateFlow<Long?>(null)
    val manageCafeteriaId = _manageCafeteriaId.asStateFlow()

    private val _manageFloorId = MutableStateFlow<Long?>(null)
    val manageFloorId = _manageFloorId.asStateFlow()

    val manageFloors = _manageCafeteriaId.flatMapLatest { repository.observeFloors(it) }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    val manageMeals = _manageFloorId.flatMapLatest { repository.observeMeals(it) }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    val enabledOptions = repository.observeEnabledOptions().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    private val randomPriceRangeInputs = combine(
        _randomPriceMinInput,
        _randomPriceMaxInput,
    ) { minInput, maxInput ->
        minInput to maxInput
    }

    val filteredOptions: StateFlow<List<MealOption>> = combine(
        enabledOptions,
        _randomCafeteriaFilter,
        _randomFloorFilter,
        randomPriceRangeInputs,
        _randomFlavorFilter,
    ) { options, cafeteriaFilter, floorFilter, priceRangeInputs, flavorFilter ->
        val (priceMinInput, priceMaxInput) = priceRangeInputs
        val scope = DecisionScope(
            cafeteriaId = cafeteriaFilter,
            floorId = floorFilter,
            priceMinYuan = parsePriceInput(priceMinInput),
            priceMaxYuan = parsePriceInput(priceMaxInput),
            flavor = flavorFilter,
        )
        options.filter { option -> option.matchesScope(scope) }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    private val _decisionResult = MutableStateFlow<DecisionResult?>(null)
    val decisionResult = _decisionResult.asStateFlow()

    private val _isRolling = MutableStateFlow(false)
    val isRolling = _isRolling.asStateFlow()

    private val _animationToken = MutableStateFlow(0L)
    val animationToken = _animationToken.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message = _message.asStateFlow()

    private val _sharePayload = MutableStateFlow<SharePayload?>(null)
    val sharePayload = _sharePayload.asStateFlow()

    private val _installApkPayload = MutableStateFlow<InstallApkPayload?>(null)
    val installApkPayload = _installApkPayload.asStateFlow()

    private val _isUpdatingApp = MutableStateFlow(false)
    val isUpdatingApp = _isUpdatingApp.asStateFlow()

    private val _appUpdateProgress = MutableStateFlow<Int?>(null)
    val appUpdateProgress = _appUpdateProgress.asStateFlow()

    private val _appUpdateStatus = MutableStateFlow("点击“检查更新”可下载最新版")
    val appUpdateStatus = _appUpdateStatus.asStateFlow()

    private val _communityConfigs = MutableStateFlow<List<CommunityConfigEntry>>(emptyList())
    val communityConfigs = _communityConfigs.asStateFlow()

    private val _communityUpdatedAt = MutableStateFlow("")
    val communityUpdatedAt = _communityUpdatedAt.asStateFlow()

    private val _isCommunityLoading = MutableStateFlow(false)
    val isCommunityLoading = _isCommunityLoading.asStateFlow()

    private val _communityImportingId = MutableStateFlow<String?>(null)
    val communityImportingId = _communityImportingId.asStateFlow()

    private val _aiSmartRecommendResult = MutableStateFlow<AiSmartRecommendResult?>(null)
    val aiSmartRecommendResult = _aiSmartRecommendResult.asStateFlow()

    private val _aiSearchResult = MutableStateFlow<AiSearchResult?>(null)
    val aiSearchResult = _aiSearchResult.asStateFlow()

    private val _aiDietAnalysis = MutableStateFlow<AiDietAnalysis?>(null)
    val aiDietAnalysis = _aiDietAnalysis.asStateFlow()

    private val _aiRecommendLoading = MutableStateFlow(false)
    val aiRecommendLoading = _aiRecommendLoading.asStateFlow()

    private val _aiSearchLoading = MutableStateFlow(false)
    val aiSearchLoading = _aiSearchLoading.asStateFlow()

    private val _aiAnalysisLoading = MutableStateFlow(false)
    val aiAnalysisLoading = _aiAnalysisLoading.asStateFlow()

    private val _nlSearchQuery = MutableStateFlow("")
    val nlSearchQuery = _nlSearchQuery.asStateFlow()

    private val _aiStreamingText = MutableStateFlow("")
    val aiStreamingText: StateFlow<String> = _aiStreamingText.asStateFlow()

    private val _aiSmartRecommendStreamingText = MutableStateFlow("")
    val aiSmartRecommendStreamingText = _aiSmartRecommendStreamingText.asStateFlow()

    private val _aiAnalysisStreamingText = MutableStateFlow("")
    val aiAnalysisStreamingText = _aiAnalysisStreamingText.asStateFlow()

    val communityIssueUrl: String = communityConfigService.issueTemplateUrl()
    val communityRepoUrl: String = communityConfigService.repositoryUrl()
    val appVersionName: String = BuildConfig.VERSION_NAME.substringBefore('-')

    init {
        viewModelScope.launch {
            repository.seedIfEmpty()
        }

        viewModelScope.launch {
            cafeterias.collect { list ->
                if (_manageCafeteriaId.value == null && list.isNotEmpty()) {
                    _manageCafeteriaId.value = list.first().id
                }
                val current = _manageCafeteriaId.value
                if (current != null && list.none { it.id == current }) {
                    _manageCafeteriaId.value = list.firstOrNull()?.id
                }

                val randomCurrent = _randomCafeteriaFilter.value
                if (randomCurrent != null && list.none { it.id == randomCurrent }) {
                    _randomCafeteriaFilter.value = null
                    _randomFloorFilter.value = null
                }
            }
        }

        viewModelScope.launch {
            manageFloors.collect { list ->
                if (_manageFloorId.value == null && list.isNotEmpty()) {
                    _manageFloorId.value = list.first().id
                }
                val current = _manageFloorId.value
                if (current != null && list.none { it.id == current }) {
                    _manageFloorId.value = list.firstOrNull()?.id
                }
            }
        }

        viewModelScope.launch {
            randomFloors.collect { list ->
                val current = _randomFloorFilter.value
                if (current != null && list.none { it.id == current }) {
                    _randomFloorFilter.value = null
                }
            }
        }
    }

    fun setRandomCafeteriaFilter(id: Long?) {
        _randomCafeteriaFilter.value = id
        _randomFloorFilter.value = null
    }

    fun setRandomFloorFilter(id: Long?) {
        _randomFloorFilter.value = id
    }

    fun setRandomPriceMinInput(input: String) {
        _randomPriceMinInput.value = input.filter(Char::isDigit).take(4)
    }

    fun setRandomPriceMaxInput(input: String) {
        _randomPriceMaxInput.value = input.filter(Char::isDigit).take(4)
    }

    fun setRandomFlavorFilter(filter: String?) {
        _randomFlavorFilter.value = filter?.trim().takeUnless { it.isNullOrBlank() }
    }

    fun setManageCafeteria(id: Long?) {
        _manageCafeteriaId.value = id
        _manageFloorId.value = null
    }

    fun setManageFloor(id: Long?) {
        _manageFloorId.value = id
    }

    fun consumeMessage() {
        _message.value = null
    }

    fun consumeSharePayload() {
        _sharePayload.value = null
    }

    fun consumeInstallApkPayload() {
        _installApkPayload.value = null
        _appUpdateProgress.value = null
    }

    fun spin() = decide(DecisionMode.SPIN)

    fun draw() = decide(DecisionMode.DRAW)

    fun chooseDrawOption(option: MealOption) {
        viewModelScope.launch {
            val result = DecisionResult(
                cafeteria = option.cafeteriaName,
                floor = option.floorName,
                meal = option.mealName,
                timestamp = System.currentTimeMillis(),
                mode = DecisionMode.DRAW,
                historyKey = "${option.cafeteriaName} > ${option.floorName} > ${option.mealName}",
            )
            _decisionResult.value = result

            val maxWindow = settings.first().recentWindowSize.coerceAtLeast(1)
            settingsStore.appendHistoryKey(result.historyKey, maxWindow)
        }
    }

    private fun decide(mode: DecisionMode) {
        if (_isRolling.value) return
        viewModelScope.launch {
            _isRolling.value = true

            val scope = DecisionScope(
                cafeteriaId = _randomCafeteriaFilter.value,
                floorId = _randomFloorFilter.value,
                priceMinYuan = parsePriceInput(_randomPriceMinInput.value),
                priceMaxYuan = parsePriceInput(_randomPriceMaxInput.value),
                flavor = _randomFlavorFilter.value,
            )

            val result = when (mode) {
                DecisionMode.SPIN -> randomEngine.spinDecision(scope, mode)
                DecisionMode.DRAW -> randomEngine.drawDecision(scope, mode)
            }
            if (result == null) {
                _message.value = "当前筛选条件无可选项，请先补充数据或调整筛选"
                _isRolling.value = false
            } else {
                _decisionResult.value = result
                val currentSettings = settings.first()
                if (currentSettings.animationsEnabled && mode == DecisionMode.SPIN) {
                    _animationToken.value = maxOf(System.currentTimeMillis(), _animationToken.value + 1L)
                    delay(1650)
                }
                _isRolling.value = false
            }
        }
    }

    fun upsertCafeteria(item: CafeteriaEntity) {
        viewModelScope.launch {
            repository.upsertCafeteria(item)
        }
    }

    fun upsertFloor(item: FloorEntity) {
        viewModelScope.launch {
            repository.upsertFloor(item)
        }
    }

    fun upsertMeal(item: MealEntity) {
        viewModelScope.launch {
            repository.upsertMeal(item)
        }
    }

    fun deleteCafeteria(id: Long) {
        viewModelScope.launch {
            repository.deleteCafeteria(id)
        }
    }

    fun deleteFloor(id: Long) {
        viewModelScope.launch {
            repository.deleteFloor(id)
        }
    }

    fun deleteMeal(id: Long) {
        viewModelScope.launch {
            repository.deleteMeal(id)
        }
    }

    fun setCooldownEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsStore.setCooldownEnabled(enabled)
        }
    }

    fun setAnimationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsStore.setAnimationsEnabled(enabled)
        }
    }

    fun setHapticsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsStore.setHapticsEnabled(enabled)
        }
    }

    fun setRecentWindowSize(value: Int) {
        viewModelScope.launch {
            settingsStore.setRecentWindowSize(value)
        }
    }

    fun importFromUri(uri: Uri) {
        viewModelScope.launch {
            val summary: ImportSummary = importExportService.importFromJson(uri)
            if (summary.success) {
                settingsStore.clearHistory()
            }
            _message.value = buildString {
                append(summary.message)
                if (summary.success) {
                    append("（食堂${summary.cafeteriaCount}，楼层${summary.floorCount}，伙食${summary.mealCount}）")
                }
            }
        }
    }

    fun exportToUri(uri: Uri) {
        viewModelScope.launch {
            val summary = importExportService.exportToJson(uri)
            _message.value = buildString {
                append(summary.message)
                if (summary.success) {
                    append("（食堂${summary.cafeteriaCount}，楼层${summary.floorCount}，伙食${summary.mealCount}）")
                }
            }
        }
    }

    fun shareCurrentConfig() {
        viewModelScope.launch {
            val summary = importExportService.exportToShareFile()
            if (summary.success && summary.uri != null) {
                _sharePayload.value = SharePayload(
                    uri = summary.uri,
                    fileName = summary.fileName,
                )
                _message.value = "已生成分享文件（食堂${summary.cafeteriaCount}，楼层${summary.floorCount}，伙食${summary.mealCount}）"
            } else {
                _message.value = summary.message
            }
        }
    }

    fun checkAndUpdateApp() {
        if (_isUpdatingApp.value) return

        viewModelScope.launch {
            _isUpdatingApp.value = true
            _appUpdateProgress.value = null
            _appUpdateStatus.value = "正在检查更新..."
            val check = appUpdateService.checkForUpdate(appVersionName)
            if (!check.success) {
                _appUpdateStatus.value = check.message
                _message.value = check.message
                _isUpdatingApp.value = false
                return@launch
            }
            if (!check.hasUpdate) {
                _appUpdateStatus.value = "当前版本已是最新：${check.latestVersion}"
                _appUpdateProgress.value = null
                _message.value = _appUpdateStatus.value
                _isUpdatingApp.value = false
                return@launch
            }

            _appUpdateStatus.value = "发现新版本 ${check.latestVersion}，开始下载..."
            _appUpdateProgress.value = 0
            val download = appUpdateService.downloadApk(
                downloadUrl = check.downloadUrl,
                latestVersion = check.latestVersion,
                onProgress = { progress ->
                    _appUpdateProgress.value = progress
                    if (progress in 0..99) {
                        _appUpdateStatus.value = "下载中... $progress%"
                    } else if (progress == -1) {
                        _appUpdateStatus.value = "下载中..."
                    }
                },
            )
            if (!download.success || download.uri == null) {
                _appUpdateStatus.value = download.message
                _appUpdateProgress.value = null
                _message.value = download.message
                _isUpdatingApp.value = false
                return@launch
            }

            _installApkPayload.value = InstallApkPayload(
                uri = download.uri,
                version = download.version,
            )
            _appUpdateStatus.value = "下载完成，准备安装 ${download.version}"
            _appUpdateProgress.value = 100
            _message.value = "新版本 ${download.version} 已下载，正在调起安装。"
            _isUpdatingApp.value = false
        }
    }

    fun loadCommunityConfigs(force: Boolean = false) {
        if (_isCommunityLoading.value) return
        if (!force && _communityConfigs.value.isNotEmpty()) return

        viewModelScope.launch {
            _isCommunityLoading.value = true
            val result = communityConfigService.fetchIndex()
            if (result.success) {
                _communityConfigs.value = result.entries
                _communityUpdatedAt.value = result.updatedAt
            } else {
                _message.value = result.message
            }
            _isCommunityLoading.value = false
        }
    }

    fun importFromCommunity(entry: CommunityConfigEntry) {
        if (_communityImportingId.value != null) return

        viewModelScope.launch {
            _communityImportingId.value = entry.id
            val download = communityConfigService.downloadConfig(entry)
            if (!download.success) {
                _communityImportingId.value = null
                _message.value = download.message
                return@launch
            }

            val summary: ImportSummary = importExportService.importFromRawJson(download.rawJson)
            if (summary.success) {
                settingsStore.clearHistory()
            }
            _message.value = buildString {
                append(summary.message)
                if (summary.success) {
                    append("（来自${entry.schoolName}，食堂${summary.cafeteriaCount}，楼层${summary.floorCount}，伙食${summary.mealCount}）")
                }
            }
            _communityImportingId.value = null
        }
    }

    private fun parsePriceInput(input: String): Int? {
        if (input.isBlank()) return null
        return input.filter(Char::isDigit).toIntOrNull()
    }

    fun setNlSearchQuery(query: String) {
        _nlSearchQuery.value = query
    }

    fun aiSmartRecommend() {
        if (_aiRecommendLoading.value) return
        viewModelScope.launch {
            _aiRecommendLoading.value = true
            _aiSmartRecommendStreamingText.value = ""
            val options = filteredOptions.value
            if (options.isEmpty()) {
                _message.value = "当前无可选项，请先补充食堂数据"
                _aiRecommendLoading.value = false
                return@launch
            }

            val historySet = settings.value.recentHistory.map { it.key }.toSet()
            val freshOptions = options.filterNot {
                "${it.cafeteriaName} > ${it.floorName} > ${it.mealName}" in historySet
            }.ifEmpty { options }

            val enrichedOptions = freshOptions.map { enrichOptionString(it) }
            val historyStrings = settings.value.recentHistory.takeLast(10).map {
                "[${formatTimestampForAI(it.timestamp)}] ${it.key}"
            }
            val allOptions = enabledOptions.value
            val stats = computeEatingStats(settings.value.recentHistory, allOptions)
            val statsText = AiPrompts.formatEatingStats(stats)

            val result = aiService.smartRecommend(
                options = enrichedOptions,
                history = historyStrings,
                statsText = statsText,
            ) { chunk ->
                _aiSmartRecommendStreamingText.value += chunk
            }
            result.fold(
                onSuccess = {
                    _aiSmartRecommendResult.value = it
                    _aiSmartRecommendStreamingText.value = ""
                },
                onFailure = { _message.value = "AI 推荐失败：${it.message ?: "未知错误"}" },
            )
            _aiRecommendLoading.value = false
        }
    }

    fun aiNaturalLanguageSearch() {
        val query = _nlSearchQuery.value
        if (query.isBlank() || _aiSearchLoading.value) return
        viewModelScope.launch {
            _aiSearchLoading.value = true
            _aiStreamingText.value = ""
            val options = filteredOptions.value
            if (options.isEmpty()) {
                _message.value = "当前无可选项，请先补充食堂数据"
                _aiSearchLoading.value = false
                return@launch
            }

            val optionStrings = options.map { enrichOptionString(it) }
            val result = aiService.naturalLanguageSearch(query, optionStrings) { chunk ->
                _aiStreamingText.value += chunk
            }
            result.fold(
                onSuccess = {
                    _aiSearchResult.value = it
                    _aiStreamingText.value = ""
                },
                onFailure = { _message.value = "AI 搜索失败：${it.message ?: "未知错误"}" },
            )
            _aiSearchLoading.value = false
        }
    }

    fun aiAnalyzeHistory() {
        if (_aiAnalysisLoading.value) return
        viewModelScope.launch {
            _aiAnalysisLoading.value = true
            _aiAnalysisStreamingText.value = ""
            val history = settings.value.recentHistory
            if (history.isEmpty()) {
                _message.value = "暂无饮食记录，先去吃几顿吧"
                _aiAnalysisLoading.value = false
                return@launch
            }

            val allOptions = enabledOptions.value
            val recentHistory = history.takeLast(30)
            val enrichedHistory = recentHistory.map { record ->
                val base = findMealOptionByHistoryKey(record.key, allOptions)
                    ?.let { enrichOptionString(it) } ?: record.key
                "[${formatTimestampForAI(record.timestamp)}] $base"
            }
            val enrichedOptions = allOptions.map { enrichOptionString(it) }
            val stats = computeEatingStats(recentHistory, allOptions)
            val statsText = AiPrompts.formatEatingStats(stats)

            val result = aiService.analyzeHistory(
                history = enrichedHistory,
                options = enrichedOptions,
                statsText = statsText,
            ) { chunk ->
                _aiAnalysisStreamingText.value += chunk
            }
            result.fold(
                onSuccess = {
                    _aiDietAnalysis.value = it
                    _aiAnalysisStreamingText.value = ""
                },
                onFailure = { _message.value = "AI 分析失败：${it.message ?: "未知错误"}" },
            )
            _aiAnalysisLoading.value = false
        }
    }

    fun recordFromAiRecommend(item: AiRecommendItem) {
        val option = findMealOptionByName(
            item.cafeteria, item.floor, item.meal,
        )
        if (option != null) {
            chooseDrawOption(option)
            _message.value = "已记录：${item.cafeteria} · ${item.meal}"
        } else {
            viewModelScope.launch {
                val historyKey = "${item.cafeteria} > ${item.floor} > ${item.meal}"
                val result = DecisionResult(
                    cafeteria = item.cafeteria,
                    floor = item.floor,
                    meal = item.meal,
                    timestamp = System.currentTimeMillis(),
                    mode = DecisionMode.DRAW,
                    historyKey = historyKey,
                )
                _decisionResult.value = result
                val maxWindow = settings.first().recentWindowSize.coerceAtLeast(1)
                settingsStore.appendHistoryKey(historyKey, maxWindow)
                _message.value = "已记录：${item.cafeteria} · ${item.meal}"
            }
        }
    }

    private fun enrichOptionString(option: MealOption): String {
        val parts = mutableListOf<String>()
        if (option.mealTags.isNotBlank()) parts.add(option.mealTags.trim())
        if (option.mealFlavor.isNotBlank()) parts.add(option.mealFlavor.trim())
        if (option.mealPriceYuan != null) parts.add("¥${option.mealPriceYuan}")
        val meta = if (parts.isNotEmpty()) " | ${parts.joinToString(" | ")}" else ""
        return "${option.cafeteriaName} > ${option.floorName} > ${option.mealName}$meta"
    }

    private fun findMealOptionByHistoryKey(
        key: String,
        allOptions: List<MealOption>,
    ): MealOption? = allOptions.firstOrNull {
        "${it.cafeteriaName} > ${it.floorName} > ${it.mealName}" == key
    }

    private fun findMealOptionByName(
        cafeteria: String,
        floor: String,
        meal: String,
    ): MealOption? = enabledOptions.value.firstOrNull {
        it.cafeteriaName == cafeteria && it.floorName == floor && it.mealName == meal
    }

    private fun computeEatingStats(
        history: List<HistoryRecord>,
        allOptions: List<MealOption>,
    ): UserEatingStats {
        val enriched = history.mapNotNull { record ->
            findMealOptionByHistoryKey(record.key, allOptions)?.let { record.key to it }
        }
        if (enriched.isEmpty()) return UserEatingStats()

        val cafeteriaFreq = enriched.groupBy { it.second.cafeteriaName }
            .mapValues { it.value.size }
        val tagFreq = enriched.map { it.second.mealTags.trim() }
            .filter { it.isNotBlank() }
            .groupBy { it }.mapValues { it.value.size }
        val flavorFreq = enriched.map { it.second.mealFlavor.trim() }
            .filter { it.isNotBlank() }
            .groupBy { it }.mapValues { it.value.size }
        val prices = enriched.mapNotNull { it.second.mealPriceYuan }
        val avgSpend = if (prices.isNotEmpty()) prices.sum() / prices.size else null

        val recentTags = enriched.take(5).map { it.second.mealTags.trim() }.filter { it.isNotBlank() }
        val consecutiveTags = if (recentTags.size >= 3 && recentTags.take(3).distinct().size == 1) {
            "最近${recentTags.takeWhile { it == recentTags.first() }.size}天连续吃${recentTags.first()}"
        } else null

        val timeDistribution = computeTimeDistribution(history)

        return UserEatingStats(
            totalRecords = enriched.size,
            cafeteriaFreq = cafeteriaFreq,
            tagFreq = tagFreq,
            flavorFreq = flavorFreq,
            avgSpendYuan = avgSpend,
            consecutiveTags = consecutiveTags,
            timeDistribution = timeDistribution,
        )
    }

    private fun computeTimeDistribution(history: List<HistoryRecord>): Map<String, Int> {
        val result = mutableMapOf<String, Int>()
        history.forEach { record ->
            if (record.timestamp == 0L) return@forEach
            val cal = java.util.Calendar.getInstance().apply { timeInMillis = record.timestamp }
            val hour = cal.get(java.util.Calendar.HOUR_OF_DAY)
            val period = when (hour) {
                in 6..10 -> "早餐"
                in 11..14 -> "午餐"
                in 17..21 -> "晚餐"
                in 22..23, in 0..5 -> "夜宵"
                else -> "其他"
            }
            result[period] = (result[period] ?: 0) + 1
        }
        return result
    }

    fun deleteHistoryRecord(index: Int) {
        viewModelScope.launch {
            settingsStore.deleteHistoryRecord(index)
        }
    }

    fun updateHistoryRecord(index: Int, newKey: String, newTimestamp: Long) {
        viewModelScope.launch {
            settingsStore.updateHistoryRecord(index, newKey, newTimestamp)
        }
    }

    fun consumeAiSmartRecommendResult() {
        _aiSmartRecommendResult.value = null
    }

    fun consumeAiSearchResult() {
        _aiSearchResult.value = null
    }

    fun consumeAiDietAnalysis() {
        _aiDietAnalysis.value = null
    }
}
