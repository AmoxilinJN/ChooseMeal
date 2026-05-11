package com.choosemeal.app.ui

import android.content.ClipData
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Dataset
import androidx.compose.material.icons.outlined.ImportExport
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.choosemeal.app.MainViewModel
import com.choosemeal.app.ui.screen.AiScreen
import com.choosemeal.app.ui.screen.DataManagementScreen
import com.choosemeal.app.ui.screen.FoodMapScreen
import com.choosemeal.app.ui.screen.ImportExportScreen
import com.choosemeal.app.ui.screen.RandomDecisionScreen
import com.choosemeal.app.ui.screen.SettingsScreen

enum class AppSection(val title: String) {
    FOOD_MAP("美食地图"),
    AI("AI"),
    DATA("数据"),
    SETTINGS("设置"),
    IMPORT_EXPORT("导入导出"),
}

@Composable
fun ChooseMealRoot(viewModel: MainViewModel = viewModel()) {
    var currentSection by remember { mutableStateOf(AppSection.FOOD_MAP) }
    var showFoodMap by remember { mutableStateOf(true) }
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    val message by viewModel.message.collectAsStateWithLifecycle()
    LaunchedEffect(message) {
        val text = message ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(text)
        viewModel.consumeMessage()
    }

    val sharePayload by viewModel.sharePayload.collectAsStateWithLifecycle()
    LaunchedEffect(sharePayload) {
        val payload = sharePayload ?: return@LaunchedEffect
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, payload.uri)
            clipData = ClipData.newRawUri(payload.fileName, payload.uri)
            putExtra(
                Intent.EXTRA_TEXT,
                "这是我的 ChooseMeal 配置，欢迎导入体验并提交到社区仓库：${viewModel.communityRepoUrl}",
            )
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(shareIntent, "分享配置文件")
        runCatching { context.startActivity(chooser) }
        viewModel.consumeSharePayload()
    }

    val installApkPayload by viewModel.installApkPayload.collectAsStateWithLifecycle()
    LaunchedEffect(installApkPayload) {
        val payload = installApkPayload ?: return@LaunchedEffect
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !context.packageManager.canRequestPackageInstalls()) {
            val settingsIntent = Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}"),
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            runCatching { context.startActivity(settingsIntent) }
            snackbarHostState.showSnackbar("请先允许“安装未知应用”，然后再次点击更新按钮。")
            viewModel.consumeInstallApkPayload()
            return@LaunchedEffect
        }

        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(payload.uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val startResult = runCatching { context.startActivity(installIntent) }
        if (startResult.isFailure) {
            snackbarHostState.showSnackbar("启动安装失败：${startResult.exceptionOrNull()?.message ?: "未知错误"}")
        }
        viewModel.consumeInstallApkPayload()
    }

    val aiSettings by viewModel.aiSettings.collectAsStateWithLifecycle()
    val aiSearchResult by viewModel.aiSearchResult.collectAsStateWithLifecycle()
    val aiSearchLoading by viewModel.aiSearchLoading.collectAsStateWithLifecycle()
    val aiStreamingText by viewModel.aiStreamingText.collectAsStateWithLifecycle()
    val nlSearchQuery by viewModel.nlSearchQuery.collectAsStateWithLifecycle()
    val aiSmartRecommendResult by viewModel.aiSmartRecommendResult.collectAsStateWithLifecycle()
    val aiSmartRecommendLoading by viewModel.aiRecommendLoading.collectAsStateWithLifecycle()
    val aiSmartRecommendStreamingText by viewModel.aiSmartRecommendStreamingText.collectAsStateWithLifecycle()
    val aiDietAnalysis by viewModel.aiDietAnalysis.collectAsStateWithLifecycle()
    val aiAnalysisLoading by viewModel.aiAnalysisLoading.collectAsStateWithLifecycle()
    val aiAnalysisStreamingText by viewModel.aiAnalysisStreamingText.collectAsStateWithLifecycle()

    val cafeterias by viewModel.cafeterias.collectAsStateWithLifecycle()
    val randomFloors by viewModel.randomFloors.collectAsStateWithLifecycle()
    val allFloors by viewModel.allFloors.collectAsStateWithLifecycle()
    val manageFloors by viewModel.manageFloors.collectAsStateWithLifecycle()
    val manageMeals by viewModel.manageMeals.collectAsStateWithLifecycle()
    val flavorOptions by viewModel.flavorOptions.collectAsStateWithLifecycle()
    val filteredOptions by viewModel.filteredOptions.collectAsStateWithLifecycle()
    val enabledOptions by viewModel.enabledOptions.collectAsStateWithLifecycle()
    val hierarchy by viewModel.hierarchy.collectAsStateWithLifecycle()
    val decisionResult by viewModel.decisionResult.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val appUpdateStatus by viewModel.appUpdateStatus.collectAsStateWithLifecycle()
    val isUpdatingApp by viewModel.isUpdatingApp.collectAsStateWithLifecycle()
    val appUpdateProgress by viewModel.appUpdateProgress.collectAsStateWithLifecycle()
    val isRolling by viewModel.isRolling.collectAsStateWithLifecycle()
    val animationToken by viewModel.animationToken.collectAsStateWithLifecycle()
    val randomCafeteriaFilter by viewModel.randomCafeteriaFilter.collectAsStateWithLifecycle()
    val randomFloorFilter by viewModel.randomFloorFilter.collectAsStateWithLifecycle()
    val randomPriceMinInput by viewModel.randomPriceMinInput.collectAsStateWithLifecycle()
    val randomPriceMaxInput by viewModel.randomPriceMaxInput.collectAsStateWithLifecycle()
    val randomFlavorFilter by viewModel.randomFlavorFilter.collectAsStateWithLifecycle()
    val manageCafeteria by viewModel.manageCafeteriaId.collectAsStateWithLifecycle()
    val manageFloor by viewModel.manageFloorId.collectAsStateWithLifecycle()
    val communityConfigs by viewModel.communityConfigs.collectAsStateWithLifecycle()
    val communityUpdatedAt by viewModel.communityUpdatedAt.collectAsStateWithLifecycle()
    val isCommunityLoading by viewModel.isCommunityLoading.collectAsStateWithLifecycle()
    val communityImportingId by viewModel.communityImportingId.collectAsStateWithLifecycle()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = currentSection == AppSection.FOOD_MAP,
                    onClick = { currentSection = AppSection.FOOD_MAP },
                    icon = { Icon(Icons.Outlined.AutoAwesome, contentDescription = null) },
                    label = { Text("美食地图") },
                )
                NavigationBarItem(
                    selected = currentSection == AppSection.AI,
                    onClick = { currentSection = AppSection.AI },
                    icon = { Icon(Icons.Outlined.SmartToy, contentDescription = null) },
                    label = { Text("AI") },
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
            AppSection.FOOD_MAP -> {
                Column(modifier = Modifier.padding(paddingValues)) {
                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                    ) {
                        SegmentedButton(
                            selected = showFoodMap,
                            onClick = { showFoodMap = true },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        ) { Text("美食地图") }
                        SegmentedButton(
                            selected = !showFoodMap,
                            onClick = {
                                showFoodMap = false
                                // Reset filter when switching to spin
                            },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        ) { Text("随机转盘") }
                    }

                    if (showFoodMap) {
                        FoodMapScreen(
                            modifier = Modifier,
                            hierarchy = hierarchy,
                            enabledOptions = enabledOptions,
                            onRecordMeal = viewModel::chooseDrawOption,
                        )
                    } else {
                        RandomDecisionScreen(
                            modifier = Modifier,
                            cafeterias = cafeterias,
                            floors = randomFloors,
                            options = filteredOptions,
                            selectedCafeteriaId = randomCafeteriaFilter,
                            selectedFloorId = randomFloorFilter,
                            selectedPriceMinInput = randomPriceMinInput,
                            selectedPriceMaxInput = randomPriceMaxInput,
                            flavorOptions = flavorOptions,
                            selectedFlavor = randomFlavorFilter,
                            decisionResult = decisionResult,
                            isRolling = isRolling,
                            animationToken = animationToken,
                            animationsEnabled = settings.animationsEnabled,
                            hapticsEnabled = settings.hapticsEnabled,
                            onSelectCafeteria = viewModel::setRandomCafeteriaFilter,
                            onSelectFloor = viewModel::setRandomFloorFilter,
                            onSelectPriceMinInput = viewModel::setRandomPriceMinInput,
                            onSelectPriceMaxInput = viewModel::setRandomPriceMaxInput,
                            onSelectFlavor = viewModel::setRandomFlavorFilter,
                            onSpin = viewModel::spin,
                            onDrawPick = viewModel::chooseDrawOption,
                        )
                    }
                }
            }

            AppSection.DATA -> DataManagementScreen(
                modifier = Modifier.padding(paddingValues),
                cafeterias = cafeterias,
                allFloors = allFloors,
                selectedCafeteriaId = manageCafeteria,
                selectedFloorId = manageFloor,
                floors = manageFloors,
                meals = manageMeals,
                flavorOptions = flavorOptions,
                onSelectCafeteria = viewModel::setManageCafeteria,
                onSelectFloor = viewModel::setManageFloor,
                onUpsertCafeteria = viewModel::upsertCafeteria,
                onUpsertFloor = viewModel::upsertFloor,
                onUpsertMeal = viewModel::upsertMeal,
                onDeleteCafeteria = viewModel::deleteCafeteria,
                onDeleteFloor = viewModel::deleteFloor,
                onDeleteMeal = viewModel::deleteMeal,
                history = settings.recentHistory,
                onDeleteHistoryRecord = viewModel::deleteHistoryRecord,
                onUpdateHistoryRecord = viewModel::updateHistoryRecord,
            )

            AppSection.SETTINGS -> SettingsScreen(
                modifier = Modifier.padding(paddingValues),
                settings = settings,
                appVersionName = viewModel.appVersionName,
                appUpdateStatus = appUpdateStatus,
                isUpdatingApp = isUpdatingApp,
                appUpdateProgress = appUpdateProgress,
                onCooldownEnabledChange = viewModel::setCooldownEnabled,
                onAnimationsEnabledChange = viewModel::setAnimationsEnabled,
                onHapticsEnabledChange = viewModel::setHapticsEnabled,
                onWindowSizeChange = viewModel::setRecentWindowSize,
                onCheckAppUpdate = viewModel::checkAndUpdateApp,
            )

            AppSection.AI -> AiScreen(
                modifier = Modifier.padding(paddingValues),
                aiEnabled = aiSettings.aiEnabled,
                aiSearchResult = aiSearchResult,
                aiSearchLoading = aiSearchLoading,
                aiStreamingText = aiStreamingText,
                nlSearchQuery = nlSearchQuery,
                filteredOptions = filteredOptions,
                onNlSearchQueryChange = viewModel::setNlSearchQuery,
                onNaturalLanguageSearch = viewModel::aiNaturalLanguageSearch,
                onConsumeSearchResult = viewModel::consumeAiSearchResult,
                aiSmartRecommendResult = aiSmartRecommendResult,
                aiSmartRecommendLoading = aiSmartRecommendLoading,
                aiSmartRecommendStreamingText = aiSmartRecommendStreamingText,
                onSmartRecommend = viewModel::aiSmartRecommend,
                onConsumeSmartRecommendResult = viewModel::consumeAiSmartRecommendResult,
                onRecordFromRecommend = viewModel::recordFromAiRecommend,
                aiDietAnalysis = aiDietAnalysis,
                aiAnalysisLoading = aiAnalysisLoading,
                aiAnalysisStreamingText = aiAnalysisStreamingText,
                recentHistoryCount = settings.recentHistory.size,
                onAnalyzeHistory = viewModel::aiAnalyzeHistory,
                onConsumeDietAnalysis = viewModel::consumeAiDietAnalysis,
            )

            AppSection.IMPORT_EXPORT -> ImportExportScreen(
                modifier = Modifier.padding(paddingValues),
                onImport = viewModel::importFromUri,
                onExport = viewModel::exportToUri,
                onShareCurrentConfig = viewModel::shareCurrentConfig,
                communityConfigs = communityConfigs,
                communityUpdatedAt = communityUpdatedAt,
                isCommunityLoading = isCommunityLoading,
                communityImportingId = communityImportingId,
                communityIssueUrl = viewModel.communityIssueUrl,
                communityRepoUrl = viewModel.communityRepoUrl,
                onLoadCommunityConfigs = viewModel::loadCommunityConfigs,
                onImportCommunityConfig = viewModel::importFromCommunity,
            )
        }
    }
}
