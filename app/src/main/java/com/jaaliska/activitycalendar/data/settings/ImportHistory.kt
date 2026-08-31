package com.jaaliska.activitycalendar.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/** When the user last imported a CSV export. */
class ImportHistory(private val dataStore: DataStore<Preferences>) {

    /** Date of the last successful import, or null if there has not been one. */
    val lastImport: Flow<LocalDate?> = dataStore.data.map { preferences ->
        preferences[LAST_IMPORT]?.let(LocalDate::parse)
    }

    /** Remembers [date] as the day of the last successful import. */
    suspend fun record(date: LocalDate) {
        dataStore.edit { preferences -> preferences[LAST_IMPORT] = date.toString() }
    }

    private companion object {
        val LAST_IMPORT = stringPreferencesKey("last_import")
    }
}
