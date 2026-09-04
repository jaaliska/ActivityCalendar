package com.jaaliska.activitycalendar.domain.healthconnect

import kotlinx.coroutines.flow.Flow
import java.time.Instant

/** What the app remembers between Health Connect synchronisations. */
interface HealthConnectSyncState {

    /** Change-log token to ask with next time, null while nothing has been read yet. */
    val changesToken: Flow<String?>

    /** When a synchronisation last succeeded, null if none ever did. */
    val lastSync: Flow<Instant?>

    /** Whether Health Connect has ever had a session to read. */
    val seenAnySession: Flow<Boolean>

    /** Whether the system permission dialog has already been shown for this install. */
    val permissionsAsked: Flow<Boolean>

    /**
     * Remembers a successful synchronisation.
     *
     * @param sawSessions whether this synchronisation read at least one session; once true,
     * it stays true
     */
    suspend fun record(changesToken: String, syncedAt: Instant, sawSessions: Boolean)

    /** Remembers that the system has shown its permission dialog. */
    suspend fun recordPermissionsAsked()
}
