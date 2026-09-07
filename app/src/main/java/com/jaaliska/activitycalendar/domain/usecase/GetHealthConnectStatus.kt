package com.jaaliska.activitycalendar.domain.usecase

import com.jaaliska.activitycalendar.domain.healthconnect.ConnectionStatus
import com.jaaliska.activitycalendar.domain.healthconnect.HealthConnectAvailability
import com.jaaliska.activitycalendar.domain.healthconnect.HealthConnectSource
import com.jaaliska.activitycalendar.domain.healthconnect.HealthConnectSyncState
import kotlinx.coroutines.flow.first

/** Answers what the calendar banner and the settings row say about Health Connect. */
class GetHealthConnectStatus(
    private val source: HealthConnectSource,
    private val syncState: HealthConnectSyncState,
) {

    suspend operator fun invoke(): ConnectionStatus {
        val lastSync = syncState.lastSync.first()
        return when {
            source.availability() != HealthConnectAvailability.AVAILABLE ->
                ConnectionStatus.Unavailable

            source.hasRequiredPermissions() -> ConnectionStatus.Connected(lastSync)
            lastSync != null -> ConnectionStatus.Stopped
            else -> ConnectionStatus.NeverConnected
        }
    }
}
