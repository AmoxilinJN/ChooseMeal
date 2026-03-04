package com.choosemeal.app.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Dataset
import androidx.compose.material.icons.outlined.ImportExport
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.choosemeal.app.MainViewModel
import com.choosemeal.app.ui.screen.DataManagementScreen
import com.choosemeal.app.ui.screen.ImportExportScreen
import com.choosemeal.app.ui.screen.RandomDecisionScreen
import com.choosemeal.app.ui.screen.SettingsScreen

enum class AppSection(val title: String) {
    RANDOM("决策"),
    DATA("数据"),
    SETTINGS("设置"),
    IMPORT_EXPORT("导入导出"),
}

@Composable
fun ChooseMealRoot(viewModel: MainViewModel = viewModel()) {
    var currentSection by remember { mutableStateOf(AppSection.RANDOM) }
    val snackbarHostState = remember { SnackbarHostState() }

    val message by viewModel.message.collectAsStateWithLifecycle()
    LaunchedEffect(message) {
        val text = message ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(text)
        viewModel.consumeMessage()
    }

    val cafeterias by viewModel.cafeterias.collectAsStateWithLifecycle()
    val randomFloors by viewModel.randomFloors.collectAsStateWithLifecycle()
    val allFloors by viewModel.allFloors.collectAsStateWithLifecycle()
    val manageFloors by viewModel.manageFloors.collectAsStateWithLifecycle()
    val manageMeals by viewModel.manageMeals.collectAsStateWithLifecycle()
    val filteredOptions by viewModel.filteredOptions.collectAsStateWithLifecycle()
    val decisionResult by viewModel.decisionResult.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val isRolling by viewModel.isRolling.collectAsStateWithLifecycle()
    val animationToken by viewModel.animationToken.collectAsStateWithLifecycle()
    val randomCafeteriaFilter by viewModel.randomCafeteriaFilter.collectAsStateWithLifecycle()
    val randomFloorFilter by viewModel.randomFloorFilter.collectAsStateWithLifecycle()
    val manageCafeteria by viewModel.manageCafeteriaId.collectAsStateWithLifecycle()
    val manageFloor by viewModel.manageFloorId.collectAsStateWithLifecycle()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = currentSection == AppSection.RANDOM,
                    onClick = { currentSection = AppSection.RANDOM },
                    icon = { Icon(Icons.Outlined.AutoAwesome, contentDescription = null) },
                    label = { Text("决策") },
                )
                NavigationBarItem(
                    selected = currentSection == AppSection.DATA,
                    onClick = { currentSection = AppSection.DATA },
                    icon = { Icon(Icons.Outlined.Dataset, contentDescription = null) },
                    label = { Text("数据") },
                )
                NavigationBarItem(
                    selected = currentSection == AppSection.SETTINGS,
                    onClick = { currentSection = AppSection.SETTINGS },
                    icon = { Icon(Icons.Outlined.Settings, contentDescription = null) },
                    label = { Text("设置") },
                )
                NavigationBarItem(
                    selected = currentSection == AppSection.IMPORT_EXPORT,
                    onClick = { currentSection = AppSection.IMPORT_EXPORT },
                    icon = { Icon(Icons.Outlined.ImportExport, contentDescription = null) },
                    label = { Text("导入导出") },
                )
            }
        },
    ) { paddingValues ->
        when (currentSection) {
            AppSection.RANDOM -> RandomDecisionScreen(
                modifier = Modifier.padding(paddingValues),
                cafeterias = cafeterias,
                floors = randomFloors,
                options = filteredOptions,
                selectedCafeteriaId = randomCafeteriaFilter,
                selectedFloorId = randomFloorFilter,
                decisionResult = decisionResult,
                isRolling = isRolling,
                animationToken = animationToken,
                animationsEnabled = settings.animationsEnabled,
                hapticsEnabled = settings.hapticsEnabled,
                onSelectCafeteria = viewModel::setRandomCafeteriaFilter,
                onSelectFloor = viewModel::setRandomFloorFilter,
                onSpin = viewModel::spin,
                onDrawPick = viewModel::chooseDrawOption,
            )

            AppSection.DATA -> DataManagementScreen(
                modifier = Modifier.padding(paddingValues),
                cafeterias = cafeterias,
                allFloors = allFloors,
                selectedCafeteriaId = manageCafeteria,
                selectedFloorId = manageFloor,
                floors = manageFloors,
                meals = manageMeals,
                onSelectCafeteria = viewModel::setManageCafeteria,
                onSelectFloor = viewModel::setManageFloor,
                onUpsertCafeteria = viewModel::upsertCafeteria,
                onUpsertFloor = viewModel::upsertFloor,
                onUpsertMeal = viewModel::upsertMeal,
                onDeleteCafeteria = viewModel::deleteCafeteria,
                onDeleteFloor = viewModel::deleteFloor,
                onDeleteMeal = viewModel::deleteMeal,
            )

            AppSection.SETTINGS -> SettingsScreen(
                modifier = Modifier.padding(paddingValues),
                settings = settings,
                onCooldownEnabledChange = viewModel::setCooldownEnabled,
                onAnimationsEnabledChange = viewModel::setAnimationsEnabled,
                onHapticsEnabledChange = viewModel::setHapticsEnabled,
                onWindowSizeChange = viewModel::setRecentWindowSize,
            )

            AppSection.IMPORT_EXPORT -> ImportExportScreen(
                modifier = Modifier.padding(paddingValues),
                onImport = viewModel::importFromUri,
                onExport = viewModel::exportToUri,
            )
        }
    }
}
