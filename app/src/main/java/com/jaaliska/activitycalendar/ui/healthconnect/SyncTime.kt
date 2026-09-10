package com.jaaliska.activitycalendar.ui.healthconnect

import android.text.format.DateUtils
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.jaaliska.activitycalendar.R
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val CLOCK_TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH)
private val DAY_AND_MONTH: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM", Locale.ENGLISH)

/** How long ago [instant] was, the way the phone words it: "5 minutes ago", "just now". */
@Composable
fun timeAgo(instant: Instant, now: Instant = Instant.now()): String {
    val elapsed = now.toEpochMilli() - instant.toEpochMilli()
    if (elapsed < DateUtils.MINUTE_IN_MILLIS) {
        return stringResource(R.string.health_connect_just_now)
    }
    return DateUtils.getRelativeTimeSpanString(
        instant.toEpochMilli(),
        now.toEpochMilli(),
        DateUtils.MINUTE_IN_MILLIS,
    ).toString()
}

/** The clock time of [instant]: 08:12. */
fun clockTime(instant: Instant, zone: ZoneId = ZoneId.systemDefault()): String =
    LocalDateTime.ofInstant(instant, zone).format(CLOCK_TIME)

/** The day and clock time of [instant]: "Today, 08:12" or "29 August, 08:12". */
@Composable
fun dayAndTime(instant: Instant, zone: ZoneId = ZoneId.systemDefault()): String {
    val local = LocalDateTime.ofInstant(instant, zone)
    val time = local.format(CLOCK_TIME)
    return if (local.toLocalDate() == LocalDate.now(zone)) {
        stringResource(R.string.health_connect_time_today, time)
    } else {
        stringResource(R.string.health_connect_time_date, local.format(DAY_AND_MONTH), time)
    }
}
