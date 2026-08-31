package com.jaaliska.activitycalendar.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

@Composable
fun ActivityCalendarTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalActivityColors provides if (darkTheme) DarkActivityColors else LightActivityColors,
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) BlueDarkColors else BlueLightColors,
            typography = Typography,
            content = content,
        )
    }
}
