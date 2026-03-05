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
    private val importExportService = container.importExportService
    private val communityConfigService = container.communityConfigService
    private val appUpdateService = container.appUpdateService

    val settings: StateFlow<UserSettings> = settingsStore.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = UserSettings(),
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

    val communityIssueUrl: String = communityConfigService.issueTemplateUrl()
    val communityRepoUrl: String = communityConfigService.repositoryUrl()
    val appVersionName: String = BuildConfig.VERSION_NAME

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
                historyKey = "${option.cafeteriaId}:${option.floorId}:${option.mealId}",
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
}
