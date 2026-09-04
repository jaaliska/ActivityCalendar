package com.jaaliska.activitycalendar.domain.healthconnect

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
