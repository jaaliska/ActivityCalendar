package com.jaaliska.activitycalendar.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.jaaliska.activitycalendar.domain.ActivityType

internal val LightActivityColors = mapOf(
    ActivityType.BADMINTON to Color(0xFFC2185B),
    ActivityType.BASKETBALL to Color(0xFF7B4DFF),
    ActivityType.BOXING to Color(0xFFE53935),
    ActivityType.CYCLING to Color(0xFF7CB342),
    ActivityType.DANCING to Color(0xFF3949AB),
    ActivityType.HIKING to Color(0xFF00897B),
    ActivityType.MARTIAL_ARTS to Color(0xFFE53935),
    ActivityType.RUNNING to Color(0xFFEF6C00),
    ActivityType.SKIING to Color(0xFF0288D1),
    ActivityType.SNOWBOARDING to Color(0xFF0288D1),
    ActivityType.SOCCER to Color(0xFF7B4DFF),
    ActivityType.STRENGTH_TRAINING to Color(0xFF6D4C41),
    ActivityType.STRETCHING to Color(0xFF3949AB),
    ActivityType.SWIMMING to Color(0xFF0288D1),
    ActivityType.TABLE_TENNIS to Color(0xFFC2185B),
    ActivityType.TENNIS to Color(0xFFC2185B),
    ActivityType.VOLLEYBALL to Color(0xFF7B4DFF),
    ActivityType.WALKING to Color(0xFF00897B),
    ActivityType.YOGA to Color(0xFF3949AB),
    ActivityType.UNKNOWN to Color(0xFF546E7A),
)

internal val DarkActivityColors = mapOf(
    ActivityType.BADMINTON to Color(0xFFDC79A0),
    ActivityType.BASKETBALL to Color(0xFFB298FF),
    ActivityType.BOXING to Color(0xFFF08C8A),
    ActivityType.CYCLING to Color(0xFFB3D391),
    ActivityType.DANCING to Color(0xFF8C95CE),
    ActivityType.HIKING to Color(0xFF6BBBB2),
    ActivityType.MARTIAL_ARTS to Color(0xFFF08C8A),
    ActivityType.RUNNING to Color(0xFFF6AA6B),
    ActivityType.SKIING to Color(0xFF6CBAE4),
    ActivityType.SNOWBOARDING to Color(0xFF6CBAE4),
    ActivityType.SOCCER to Color(0xFFB298FF),
    ActivityType.STRENGTH_TRAINING to Color(0xFFAA9791),
    ActivityType.STRETCHING to Color(0xFF8C95CE),
    ActivityType.SWIMMING to Color(0xFF6CBAE4),
    ActivityType.TABLE_TENNIS to Color(0xFFDC79A0),
    ActivityType.TENNIS to Color(0xFFDC79A0),
    ActivityType.VOLLEYBALL to Color(0xFFB298FF),
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
