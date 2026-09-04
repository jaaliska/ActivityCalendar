package com.jaaliska.activitycalendar.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant

/** What the app remembers between Health Connect synchronisations. */
class HealthConnectSyncState(private val dataStore: DataStore<Preferences>) {

    /** Change-log token to ask with next time, null while nothing has been read yet. */
    val changesToken: Flow<String?> = dataStore.data.map { it[CHANGES_TOKEN] }

    /** When a synchronisation last succeeded, null if none ever did. */
    val lastSync: Flow<Instant?> = dataStore.data.map { preferences ->
        preferences[LAST_SYNC]?.let(Instant::ofEpochMilli)
    }

    /** Whether Health Connect has ever had a session to read. */
    val seenAnySession: Flow<Boolean> = dataStore.data.map { it[SEEN_ANY_SESSION] == true }

    /** Whether the system permission dialog has already been shown for this install. */
    val permissionsAsked: Flow<Boolean> = dataStore.data.map { it[PERMISSIONS_ASKED] == true }

    /**
     * Remembers a successful synchronisation.
     *
     * @param sawSessions whether this synchronisation read at least one session; once true,
     * it stays true
     */
    suspend fun record(changesToken: String, syncedAt: Instant, sawSessions: Boolean) {
        dataStore.edit { preferences ->
            preferences[CHANGES_TOKEN] = changesToken
            preferences[LAST_SYNC] = syncedAt.toEpochMilli()
            preferences[SEEN_ANY_SESSION] = sawSessions || preferences[SEEN_ANY_SESSION] == true
        }
    }

    /** Remembers that the system has shown its permission dialog. */
    suspend fun recordPermissionsAsked() {
        dataStore.edit { preferences -> preferences[PERMISSIONS_ASKED] = true }
    }

    private companion object {
        val CHANGES_TOKEN = stringPreferencesKey("health_connect_changes_token")
        val LAST_SYNC = longPreferencesKey("health_connect_last_sync")
        val SEEN_ANY_SESSION = booleanPreferencesKey("health_connect_seen_any_session")
        val PERMISSIONS_ASKED = booleanPreferencesKey("health_connect_permissions_asked")
    }
}
