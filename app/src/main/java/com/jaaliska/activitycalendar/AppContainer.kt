package com.jaaliska.activitycalendar

import android.content.Context
import android.util.Log
import androidx.room.Room
import com.jaaliska.activitycalendar.data.csv.CsvImporter
import com.jaaliska.activitycalendar.data.db.AppDatabase
import com.jaaliska.activitycalendar.data.file.ContentFileSource
import com.jaaliska.activitycalendar.data.file.FileSource
import com.jaaliska.activitycalendar.data.healthconnect.HealthConnectSource
import com.jaaliska.activitycalendar.data.healthconnect.HealthConnectStatus
import com.jaaliska.activitycalendar.data.healthconnect.HealthConnectSyncer
import com.jaaliska.activitycalendar.data.healthconnect.PlatformHealthConnectSource
import com.jaaliska.activitycalendar.data.healthconnect.PlatformHealthConnectStatus
import com.jaaliska.activitycalendar.data.healthconnect.SyncResult
import com.jaaliska.activitycalendar.data.repository.RoomActivityRepository
import com.jaaliska.activitycalendar.data.settings.HealthConnectSyncState
import com.jaaliska.activitycalendar.data.settings.ImportHistory
import com.jaaliska.activitycalendar.data.settings.settingsDataStore
import com.jaaliska.activitycalendar.domain.ActivityRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Holds the objects that live as long as the app does. */
class AppContainer(context: Context) {

    private val database: AppDatabase = Room
        .databaseBuilder(context, AppDatabase::class.java, DATABASE_NAME)
        .build()

    val activityRepository: ActivityRepository = RoomActivityRepository(database.activityDao())

    val csvImporter: CsvImporter = CsvImporter(activityRepository)

    val importHistory: ImportHistory = ImportHistory(context.settingsDataStore)

    val fileSource: FileSource = ContentFileSource(context.contentResolver)

    val healthConnectSource: HealthConnectSource = PlatformHealthConnectSource(context)

    val healthConnectSyncState = HealthConnectSyncState(context.settingsDataStore)

    val healthConnectStatus: HealthConnectStatus =
        PlatformHealthConnectStatus(healthConnectSource, healthConnectSyncState)

    val healthConnectSyncer = HealthConnectSyncer(
        source = healthConnectSource,
        repository = activityRepository,
        syncState = healthConnectSyncState,
    )

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Reads Health Connect once, right after the app has been opened. It runs here and not in
     * a worker on purpose: the app is on screen, so this read is allowed even to someone who
     * refused background reading.
     */
    fun syncHealthConnectOnOpen() {
        scope.launch {
            val result = healthConnectSyncer.sync()
            if (result is SyncResult.Failed) Log.w(TAG, "sync on open failed", result.cause)
        }
    }

    private companion object {
        const val DATABASE_NAME = "activity-calendar.db"
        const val TAG = "AppContainer"
    }
}
