package com.jaaliska.activitycalendar.ui.healthconnect

import java.time.Instant

/**
 * What the Health Connect screen shows.
 *
 * Missing a permission the app cannot work without is the same state as never having connected,
 * whether it was refused or revoked later. Background reading is the one permission the app
 * survives without: it then reads only while it is open.
 */
sealed interface HealthConnectUiState {

    /**
     * Health Connect can be read, but the app may not: what is read and why, before the dialog.
     *
     * @property canAsk whether the system will still show its permission dialog. It shows it
     * once; after that the permissions are changed in Health Connect settings and nowhere else
     */
    data class NotConnected(val canAsk: Boolean) : HealthConnectUiState

    /** @property canInstall Health Connect is missing rather than impossible on this phone */
    data class NotAvailable(val canInstall: Boolean) : HealthConnectUiState

    data object Syncing : HealthConnectUiState

    /**
     * @property lastSync when a synchronisation last succeeded
     * @property backgroundSync whether workouts also arrive while the app is closed
     */
    data class Connected(
        val lastSync: Instant?,
        val backgroundSync: Boolean,
    ) : HealthConnectUiState

    /** Connected, but Health Connect has never had a session to read. */
    data object NoWorkouts : HealthConnectUiState

    /**
     * @property failedAt when the failed synchronisation was attempted
     * @property lastSync when a synchronisation last succeeded; a failure does not move it
     */
    data class SyncFailed(val failedAt: Instant, val lastSync: Instant?) : HealthConnectUiState
}
