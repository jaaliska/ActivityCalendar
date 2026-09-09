package com.jaaliska.activitycalendar.ui.calendar

import androidx.annotation.PluralsRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.jaaliska.activitycalendar.R
import com.jaaliska.activitycalendar.domain.ActivityType
import com.jaaliska.activitycalendar.ui.UI_PERIOD_DAY
import com.jaaliska.activitycalendar.ui.UI_PERIOD_DAY_OF_MONTH
import java.time.Duration
import java.time.LocalDate
import java.util.Locale

/** How many activities of a type happened: `2 Runs`, `1 Ride`, `2 Badminton`. */
@Composable
fun typeCountText(type: ActivityType, count: Int): String =
    pluralStringResource(type.countRes(), count, count)

/** A length of time the way the panel writes it: `41 min`, `1 h 04 min`. */
@Composable
fun durationText(duration: Duration): String {
    val minutes = duration.toMinutes()
    return if (minutes < MINUTES_IN_HOUR) {
        stringResource(R.string.panel_duration_minutes, minutes)
    } else {
        stringResource(
            R.string.panel_duration_hours,
            minutes / MINUTES_IN_HOUR,
            minutes % MINUTES_IN_HOUR,
        )
    }
}

/** A distance the way the panel writes it: `10.6 km`. */
@Composable
fun distanceText(metres: Double): String = stringResource(
    R.string.panel_distance_km,
    String.format(Locale.ENGLISH, "%.1f", metres / METRES_IN_KM),
)

/** The days a period covers: `Sep 1 – 7`, or `Sep 30 – Oct 6` when it crosses a month. */
@Composable
fun periodText(from: LocalDate, to: LocalDate): String {
    val end = if (from.month == to.month) UI_PERIOD_DAY_OF_MONTH else UI_PERIOD_DAY
    return stringResource(
        R.string.panel_recent_period,
        from.format(UI_PERIOD_DAY),
        to.format(end),
    )
}

@PluralsRes
private fun ActivityType.countRes(): Int = when (this) {
    ActivityType.BADMINTON -> R.plurals.panel_count_badminton
    ActivityType.BASKETBALL -> R.plurals.panel_count_basketball
    ActivityType.BOXING -> R.plurals.panel_count_boxing
    ActivityType.CYCLING -> R.plurals.panel_count_cycling
    ActivityType.DANCING -> R.plurals.panel_count_dancing
    ActivityType.HIKING -> R.plurals.panel_count_hiking
    ActivityType.MARTIAL_ARTS -> R.plurals.panel_count_martial_arts
    ActivityType.RUNNING -> R.plurals.panel_count_running
    ActivityType.SKIING -> R.plurals.panel_count_skiing
    ActivityType.SNOWBOARDING -> R.plurals.panel_count_snowboarding
    ActivityType.SOCCER -> R.plurals.panel_count_soccer
    ActivityType.STRENGTH_TRAINING -> R.plurals.panel_count_strength_training
    ActivityType.STRETCHING -> R.plurals.panel_count_stretching
    ActivityType.SWIMMING -> R.plurals.panel_count_swimming
    ActivityType.TABLE_TENNIS -> R.plurals.panel_count_table_tennis
    ActivityType.TENNIS -> R.plurals.panel_count_tennis
    ActivityType.VOLLEYBALL -> R.plurals.panel_count_volleyball
    ActivityType.WALKING -> R.plurals.panel_count_walking
    ActivityType.YOGA -> R.plurals.panel_count_yoga
    ActivityType.UNKNOWN -> R.plurals.panel_count_other
}

private const val MINUTES_IN_HOUR = 60
private const val METRES_IN_KM = 1000
