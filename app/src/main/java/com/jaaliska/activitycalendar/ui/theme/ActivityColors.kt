package com.jaaliska.activitycalendar.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.jaaliska.activitycalendar.domain.ActivityType

// Chosen in docs/ux/mockup-plan.md; the dark values are the light ones lifted 42% towards white.
// The only fixed colours in the app: they do not belong to the colour scheme and do not follow it.
internal val LightActivityColors = mapOf(
    ActivityType.BADMINTON to Color(0xFFC2185B),
    ActivityType.CYCLING to Color(0xFF0288D1),
    ActivityType.RUNNING to Color(0xFFEF6C00),
    ActivityType.STRENGTH_TRAINING to Color(0xFF6D4C41),
    ActivityType.WALKING to Color(0xFF00897B),
    ActivityType.YOGA to Color(0xFF3949AB),
    ActivityType.UNKNOWN to Color(0xFF546E7A),
)

internal val DarkActivityColors = mapOf(
    ActivityType.BADMINTON to Color(0xFFDC79A0),
    ActivityType.CYCLING to Color(0xFF6CBAE4),
    ActivityType.RUNNING to Color(0xFFF6AA6B),
    ActivityType.STRENGTH_TRAINING to Color(0xFFAA9791),
    ActivityType.WALKING to Color(0xFF6BBBB2),
    ActivityType.YOGA to Color(0xFF8C95CE),
    ActivityType.UNKNOWN to Color(0xFF9CABB2),
)

internal val LocalActivityColors =
    staticCompositionLocalOf<Map<ActivityType, Color>> { LightActivityColors }

/** The colour that tells [type] apart in the calendar, in the theme currently in use. */
@Composable
@ReadOnlyComposable
fun activityColor(type: ActivityType): Color = LocalActivityColors.current.getValue(type)
