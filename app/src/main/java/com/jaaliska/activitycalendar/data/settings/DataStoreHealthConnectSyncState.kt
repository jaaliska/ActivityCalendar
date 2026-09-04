package com.jaaliska.activitycalendar.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.jaaliska.activitycalendar.domain.healthconnect.HealthConnectSyncState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant

class DataStoreHealthConnectSyncState(
    private val dataStore: DataStore<Preferences>,
) : HealthConnectSyncState {

    override val changesToken: Flow<String?> = dataStore.data.map { it[CHANGES_TOKEN] }

    override val lastSync: Flow<Instant?> = dataStore.data.map { preferences ->
        preferences[LAST_SYNC]?.let(Instant::ofEpochMilli)
    }

    override val seenAnySession: Flow<Boolean> = dataStore.data.map { it[SEEN_ANY_SESSION] == true }

    override val permissionsAsked: Flow<Boolean> = dataStore.data.map { it[PERMISSIONS_ASKED] == true }

    override suspend fun record(changesToken: String, syncedAt: Instant, sawSessions: Boolean) {
        dataStore.edit { preferences ->
            preferences[CHANGES_TOKEN] = changesToken
            preferences[LAST_SYNC] = syncedAt.toEpochMilli()
            preferences[SEEN_ANY_SESSION] = sawSessions || preferences[SEEN_ANY_SESSION] == true
        }
    }

    override suspend fun recordPermissionsAsked() {
        dataStore.edit { preferences -> preferences[PERMISSIONS_ASKED] = true }
    }

    private companion object {
        val CHANGES_TOKEN = stringPreferencesKey("health_connect_changes_token")
        val LAST_SYNC = longPreferencesKey("health_connect_last_sync")
        val SEEN_ANY_SESSION = booleanPreferencesKey("health_connect_seen_any_session")
        val PERMISSIONS_ASKED = booleanPreferencesKey("health_connect_permissions_asked")
    }
}
