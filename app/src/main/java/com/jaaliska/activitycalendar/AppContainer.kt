package com.jaaliska.activitycalendar

import android.content.Context
import android.util.Log
import androidx.room.Room
import com.jaaliska.activitycalendar.data.csv.GarminCsvParser
import com.jaaliska.activitycalendar.data.db.AppDatabase
import com.jaaliska.activitycalendar.data.file.AssetDemoDataFile
import com.jaaliska.activitycalendar.data.file.ContentFileSource
import com.jaaliska.activitycalendar.data.healthconnect.PlatformHealthConnectSource
import com.jaaliska.activitycalendar.data.repository.RoomActivityRepository
import com.jaaliska.activitycalendar.data.settings.DataStoreHealthConnectSyncState
import com.jaaliska.activitycalendar.data.settings.DataStoreImportHistory
import com.jaaliska.activitycalendar.data.settings.settingsDataStore
import com.jaaliska.activitycalendar.data.settings.DataStoreAppearanceSettings
import com.jaaliska.activitycalendar.domain.ActivityRepository
import com.jaaliska.activitycalendar.domain.AppearanceSettings
import com.jaaliska.activitycalendar.domain.ColorSchemeChoice
import com.jaaliska.activitycalendar.domain.DemoDataFile
import com.jaaliska.activitycalendar.domain.ImportHistory
import com.jaaliska.activitycalendar.domain.healthconnect.HealthConnectSource
import com.jaaliska.activitycalendar.domain.healthconnect.HealthConnectSyncState
import com.jaaliska.activitycalendar.domain.usecase.GetHealthConnectStatus
import com.jaaliska.activitycalendar.domain.usecase.ImportActivities
import com.jaaliska.activitycalendar.domain.usecase.LoadDemoData
import com.jaaliska.activitycalendar.domain.usecase.ObserveCalendarMonths
import com.jaaliska.activitycalendar.domain.usecase.ObserveRecentSummary
import com.jaaliska.activitycalendar.domain.usecase.SyncHealthConnect
import com.jaaliska.activitycalendar.domain.usecase.SyncResult
import com.jaaliska.activitycalendar.ui.file.FileSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Holds the objects that live as long as the app does. */
class AppContainer(context: Context) {

    private val database: AppDatabase = Room
        .databaseBuilder(context, AppDatabase::class.java, DATABASE_NAME)
        .addMigrations(AppDatabase.MIGRATION_1_2)
        .build()

    val activityRepository: ActivityRepository = RoomActivityRepository(database)

    val observeCalendarMonths = ObserveCalendarMonths(activityRepository)

    val observeRecentSummary = ObserveRecentSummary(activityRepository)

    val importHistory: ImportHistory = DataStoreImportHistory(context.settingsDataStore)

    private val csvParser = GarminCsvParser()

    val importActivities = ImportActivities(
        repository = activityRepository,
        parser = csvParser,
        importHistory = importHistory,
    )

    val demoDataFile: DemoDataFile = AssetDemoDataFile(context.assets)

    val loadDemoData = LoadDemoData(
        repository = activityRepository,
        parser = csvParser,
        file = demoDataFile,
    )

    val fileSource: FileSource = ContentFileSource(context.contentResolver)

    val healthConnectSource: HealthConnectSource = PlatformHealthConnectSource(context)

    val healthConnectSyncState: HealthConnectSyncState =
        DataStoreHealthConnectSyncState(context.settingsDataStore)

    val getHealthConnectStatus = GetHealthConnectStatus(healthConnectSource, healthConnectSyncState)

    val syncHealthConnect = SyncHealthConnect(
        source = healthConnectSource,
        repository = activityRepository,
        syncState = healthConnectSyncState,
    )

    val appearanceSettings: AppearanceSettings =
        DataStoreAppearanceSettings(context.settingsDataStore)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val colorScheme: StateFlow<ColorSchemeChoice> = appearanceSettings.colorScheme
        .stateIn(scope, SharingStarted.Eagerly, ColorSchemeChoice.BLUE)

    /**
     * Reads Health Connect once, right after the app has been opened. It runs here and not in
     * a worker on purpose: the app is on screen, so this read is allowed even to someone who
     * refused background reading.
     */
    fun syncHealthConnectOnOpen() {
        scope.launch {
            val result = syncHealthConnect()
            if (result is SyncResult.Failed) Log.w(TAG, "sync on open failed", result.cause)
        }
    }

    private companion object {
        const val DATABASE_NAME = "activity-calendar.db"
        const val TAG = "AppContainer"
    }
}
