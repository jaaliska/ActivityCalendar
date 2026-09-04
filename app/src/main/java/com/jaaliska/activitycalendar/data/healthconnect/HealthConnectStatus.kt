package com.jaaliska.activitycalendar.data.healthconnect

import com.jaaliska.activitycalendar.data.settings.HealthConnectSyncState
import kotlinx.coroutines.flow.first
import java.time.Instant

/** How the connection to Health Connect stands. */
sealed interface ConnectionStatus {

    /** Health Connect is readable, but the app has never had all the permissions. */
    data object NeverConnected : ConnectionStatus

    /** Synchronisation used to work and no longer does: the permissions are gone. */
    data object Stopped : ConnectionStatus

    /** @property lastSync when a synchronisation last succeeded */
    data class Connected(val lastSync: Instant?) : ConnectionStatus

    /** Health Connect cannot be read on this phone. */
    data object Unavailable : ConnectionStatus
}

/** Answers what the calendar banner and the settings row say about Health Connect. */
fun interface HealthConnectStatus {

    suspend fun current(): ConnectionStatus
}

class PlatformHealthConnectStatus(
    private val source: HealthConnectSource,
    private val syncState: HealthConnectSyncState,
) : HealthConnectStatus {

    override suspend fun current(): ConnectionStatus {
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
