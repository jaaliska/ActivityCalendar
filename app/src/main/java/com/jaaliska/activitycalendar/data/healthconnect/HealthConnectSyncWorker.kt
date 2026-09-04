package com.jaaliska.activitycalendar.data.healthconnect

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.jaaliska.activitycalendar.ActivityCalendarApp
import com.jaaliska.activitycalendar.domain.usecase.SyncResult
import java.time.Duration

/** Synchronises with Health Connect while the app is not open. */
class HealthConnectSyncWorker(
    context: Context,
    parameters: WorkerParameters,
) : CoroutineWorker(context, parameters) {

    override suspend fun doWork(): Result {
        val container = (applicationContext as ActivityCalendarApp).container
        // Reading from here is reading in the background. Without that permission there is
        // nothing this worker may do, and it stops rather than waking up to fail.
        if (!container.healthConnectSource.canReadInBackground()) {
            WorkManager.getInstance(applicationContext).cancelUniqueWork(REPEATING_WORK)
            return Result.success()
        }
        return when (val result = container.syncHealthConnect()) {
            is SyncResult.Synced -> {
                Log.i(TAG, "stored ${result.added} new activities")
                Result.success()
            }

            SyncResult.NotConnected -> Result.success()

            is SyncResult.Failed -> {
                Log.w(TAG, "sync failed", result.cause)
                Result.retry()
            }
        }
    }

    companion object {

        /**
         * Keeps the repeating synchronisation scheduled, so workouts recorded while the app was
         * closed are already there when it opens. The work cancels itself on its first run if
         * background reading is not allowed.
         */
        fun schedule(context: Context) {
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                REPEATING_WORK,
                ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<HealthConnectSyncWorker>(SYNC_PERIOD).build(),
            )
        }

        private const val TAG = "HealthConnectSync"
        private const val REPEATING_WORK = "health-connect-sync"
        private val SYNC_PERIOD: Duration = Duration.ofHours(6)
    }
}
