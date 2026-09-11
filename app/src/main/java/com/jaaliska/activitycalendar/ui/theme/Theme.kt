package com.jaaliska.activitycalendar.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.jaaliska.activitycalendar.domain.ColorSchemeChoice

@Composable
fun ActivityCalendarTheme(
    scheme: ColorSchemeChoice = ColorSchemeChoice.BLUE,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalActivityColors provides if (darkTheme) DarkActivityColors else LightActivityColors,
    ) {
        MaterialTheme(
            colorScheme = colorsOf(scheme, darkTheme),
            typography = Typography,
            content = content,
        )
    }
}
