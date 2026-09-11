package com.jaaliska.activitycalendar.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.jaaliska.activitycalendar.domain.AppearanceSettings
import com.jaaliska.activitycalendar.domain.ColorSchemeChoice
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DataStoreAppearanceSettings(
    private val dataStore: DataStore<Preferences>,
) : AppearanceSettings {

    override val colorScheme: Flow<ColorSchemeChoice> = dataStore.data.map { preferences ->
        val stored = preferences[COLOR_SCHEME]
        ColorSchemeChoice.entries.firstOrNull { it.name == stored } ?: DEFAULT_SCHEME
    }

    override suspend fun setColorScheme(choice: ColorSchemeChoice) {
        dataStore.edit { preferences -> preferences[COLOR_SCHEME] = choice.name }
    }

    private companion object {
        val COLOR_SCHEME = stringPreferencesKey("color_scheme")
        val DEFAULT_SCHEME = ColorSchemeChoice.BLUE
    }
}
