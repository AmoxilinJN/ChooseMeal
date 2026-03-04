package com.choosemeal.app

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.choosemeal.app.data.importexport.ImportSummary
import com.choosemeal.app.data.local.entity.CafeteriaEntity
import com.choosemeal.app.data.local.entity.FloorEntity
import com.choosemeal.app.data.local.entity.MealEntity
import com.choosemeal.app.data.preferences.UserSettings
import com.choosemeal.app.domain.model.DecisionMode
import com.choosemeal.app.domain.model.DecisionResult
import com.choosemeal.app.domain.model.DecisionScope
import com.choosemeal.app.domain.model.MealOption
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val container = (application as ChooseMealApplication).container
    private val repository = container.repository
    private val randomEngine = container.randomEngine
    private val settingsStore = container.settingsStore
    private val importExportService = container.importExportService

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

    val filteredOptions: StateFlow<List<MealOption>> = combine(
        enabledOptions,
        _randomCafeteriaFilter,
        _randomFloorFilter,
    ) { options, cafeteriaFilter, floorFilter ->
        options.filter { option ->
            val cafeteriaMatch = cafeteriaFilter == null || option.cafeteriaId == cafeteriaFilter
            val floorMatch = floorFilter == null || option.floorId == floorFilter
            cafeteriaMatch && floorMatch
        }
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
            _animationToken.value = System.currentTimeMillis()

            val scope = DecisionScope(
                cafeteriaId = _randomCafeteriaFilter.value,
                floorId = _randomFloorFilter.value,
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
}
