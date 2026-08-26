package com.jaaliska.activitycalendar.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

/**
 * Wraps [content] in the app's colours and type.
 *
 * @param darkTheme whether to use the dark scheme; follows the system setting
 */
@Composable
fun ActivityCalendarTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) BlueDarkColors else BlueLightColors,
        typography = Typography,
        content = content,
    )
}
