package com.jaaliska.activitycalendar.domain

import kotlinx.coroutines.flow.MutableStateFlow

/** Appearance settings that live only as long as the test. */
class FakeAppearanceSettings(
    initial: ColorSchemeChoice = ColorSchemeChoice.BLUE,
) : AppearanceSettings {

    override val colorScheme: MutableStateFlow<ColorSchemeChoice> = MutableStateFlow(initial)

    override suspend fun setColorScheme(choice: ColorSchemeChoice) {
        colorScheme.value = choice
    }
}
