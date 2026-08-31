package com.jaaliska.activitycalendar.data.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

private val Context.dataStore by preferencesDataStore(name = "settings")

/** When the user last imported a CSV export. */
class ImportHistory(private val context: Context) {

    /** Date of the last successful import, or null if there has not been one. */
    val lastImport: Flow<LocalDate?> = context.dataStore.data.map { preferences ->
        preferences[LAST_IMPORT]?.let(LocalDate::parse)
    }

    /** Remembers [date] as the day of the last successful import. */
    suspend fun record(date: LocalDate) {
        context.dataStore.edit { preferences -> preferences[LAST_IMPORT] = date.toString() }
    }

    private companion object {
        val LAST_IMPORT = stringPreferencesKey("last_import")
    }
}
