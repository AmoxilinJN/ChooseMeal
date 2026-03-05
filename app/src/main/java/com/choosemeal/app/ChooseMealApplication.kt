package com.choosemeal.app

import android.app.Application
import com.choosemeal.app.data.importexport.CommunityConfigService
import com.choosemeal.app.data.importexport.GithubCommunityConfigService
import com.choosemeal.app.data.importexport.ImportExportService
import com.choosemeal.app.data.importexport.LocalImportExportService
import com.choosemeal.app.data.local.ChooseMealDatabase
import com.choosemeal.app.data.preferences.SettingsStore
import com.choosemeal.app.data.repository.MealRepository
import com.choosemeal.app.data.repository.OfflineMealRepository
import com.choosemeal.app.data.update.AppUpdateService
import com.choosemeal.app.data.update.GithubAppUpdateService
import com.choosemeal.app.domain.random.DefaultRandomEngine
import com.choosemeal.app.domain.random.RandomEngine

class ChooseMealApplication : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}

class AppContainer(application: Application) {
    private val database = ChooseMealDatabase.create(application)

    val settingsStore = SettingsStore(application)
    val repository: MealRepository = OfflineMealRepository(database)
    val randomEngine: RandomEngine = DefaultRandomEngine(
        repository = repository,
        settingsStore = settingsStore,
    )
    val communityConfigService: CommunityConfigService = GithubCommunityConfigService()
    val appUpdateService: AppUpdateService = GithubAppUpdateService(application)
    val importExportService: ImportExportService = LocalImportExportService(
        context = application,
        repository = repository,
    )
}
