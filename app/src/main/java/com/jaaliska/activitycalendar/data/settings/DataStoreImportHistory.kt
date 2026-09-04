package com.jaaliska.activitycalendar.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.jaaliska.activitycalendar.domain.ImportHistory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

class DataStoreImportHistory(private val dataStore: DataStore<Preferences>) : ImportHistory {

    override val lastImport: Flow<LocalDate?> = dataStore.data.map { preferences ->
        preferences[LAST_IMPORT]?.let(LocalDate::parse)
    }

    override suspend fun record(date: LocalDate) {
        dataStore.edit { preferences -> preferences[LAST_IMPORT] = date.toString() }
    }

    private companion object {
        val LAST_IMPORT = stringPreferencesKey("last_import")
    }
}
