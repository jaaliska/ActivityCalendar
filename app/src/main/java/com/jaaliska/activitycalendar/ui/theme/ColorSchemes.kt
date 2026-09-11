package com.jaaliska.activitycalendar.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import com.jaaliska.activitycalendar.domain.ColorSchemeChoice

fun colorsOf(choice: ColorSchemeChoice, darkTheme: Boolean): ColorScheme = when (choice) {
    ColorSchemeChoice.CRIMSON -> if (darkTheme) CrimsonDarkColors else CrimsonLightColors
    ColorSchemeChoice.BLUE -> if (darkTheme) BlueDarkColors else BlueLightColors
    ColorSchemeChoice.ORANGE -> if (darkTheme) OrangeDarkColors else OrangeLightColors
    ColorSchemeChoice.GREEN -> if (darkTheme) GreenDarkColors else GreenLightColors
}

val ColorSchemeChoice.swatch: Color
    get() = when (this) {
        ColorSchemeChoice.CRIMSON -> Color(0xFFBB576C)
        ColorSchemeChoice.BLUE -> Color(0xFF067EB5)
        ColorSchemeChoice.ORANGE -> Color(0xFFA46947)
        ColorSchemeChoice.GREEN -> Color(0xFF488446)
    }
