package com.jaaliska.activitycalendar.domain

import kotlinx.coroutines.flow.Flow

/** How the user wants the app to look. */
interface AppearanceSettings {

    /** The chosen colour scheme, [ColorSchemeChoice.BLUE] until another one is picked. */
    val colorScheme: Flow<ColorSchemeChoice>

    /** Remembers the scheme the user picked. */
    suspend fun setColorScheme(choice: ColorSchemeChoice)
}
