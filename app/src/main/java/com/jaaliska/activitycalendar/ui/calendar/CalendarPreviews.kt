package com.jaaliska.activitycalendar.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jaaliska.activitycalendar.domain.Activity
import com.jaaliska.activitycalendar.domain.ActivitySourceType
import com.jaaliska.activitycalendar.domain.ActivityType
import com.jaaliska.activitycalendar.domain.ColorSchemeChoice
import com.jaaliska.activitycalendar.domain.calendar.gridWeeks
import com.jaaliska.activitycalendar.domain.usecase.PeriodSummary
import com.jaaliska.activitycalendar.domain.usecase.TypeTotals
import com.jaaliska.activitycalendar.ui.theme.ActivityCalendarTheme
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

/** The calendar on the data the mockups use: September 2026, today Monday the 7th. */
@Preview(name = "Month and last 7 days", widthDp = 390, heightDp = 844)
@Composable
private fun MonthWithRecentPreview() = PreviewScreen(selectedDay = null)

@Preview(name = "Month and a picked day", widthDp = 390, heightDp = 844)
@Composable
private fun MonthWithDayPreview() = PreviewScreen(selectedDay = LocalDate.of(2026, 9, 3))

@Preview(name = "Month and an empty day", widthDp = 390, heightDp = 844)
@Composable
private fun MonthWithEmptyDayPreview() = PreviewScreen(selectedDay = LocalDate.of(2026, 9, 10))

@Preview(name = "Dark", widthDp = 390, heightDp = 844)
@Composable
private fun MonthDarkPreview() = PreviewScreen(selectedDay = null, dark = true)

@Preview(name = "Crimson", widthDp = 390, heightDp = 844)
@Composable
private fun CrimsonPreview() = PreviewScreen(null, scheme = ColorSchemeChoice.CRIMSON)

@Preview(name = "Crimson dark", widthDp = 390, heightDp = 844)
@Composable
private fun CrimsonDarkPreview() = PreviewScreen(null, ColorSchemeChoice.CRIMSON, dark = true)

@Preview(name = "Orange", widthDp = 390, heightDp = 844)
@Composable
private fun OrangePreview() = PreviewScreen(null, scheme = ColorSchemeChoice.ORANGE)

@Preview(name = "Orange dark", widthDp = 390, heightDp = 844)
@Composable
private fun OrangeDarkPreview() = PreviewScreen(null, ColorSchemeChoice.ORANGE, dark = true)

@Preview(name = "Green", widthDp = 390, heightDp = 844)
@Composable
private fun GreenPreview() = PreviewScreen(null, scheme = ColorSchemeChoice.GREEN)

@Preview(name = "Green dark", widthDp = 390, heightDp = 844)
@Composable
private fun GreenDarkPreview() = PreviewScreen(null, ColorSchemeChoice.GREEN, dark = true)

/** The whole screen in landscape, where it has to scroll to show the panel. */
@Preview(name = "Landscape", widthDp = 844, heightDp = 390)
@Composable
private fun LandscapePreview() {
    ActivityCalendarTheme {
        CalendarScreen(
            state = CalendarUiState.Calendar(
                today = TODAY,
                pages = mapOf(
                    YearMonth.of(2026, 9) to MonthPage(YearMonth.of(2026, 9), previewWeeks()),
                ),
                recent = previewRecent(),
            ),
            anchor = YearMonth.of(2026, 9),
            syncStopped = false,
            onMonthSettled = {},
            onDaySelected = {},
            onTodayClick = {},
            onSettingsClick = {},
            onImportClick = {},
            onHealthConnectClick = {},
            onDemoClick = {},
            onRetry = {},
            onScreenResumed = {},
        )
    }
}

@Composable
private fun PreviewScreen(
    selectedDay: LocalDate?,
    scheme: ColorSchemeChoice = ColorSchemeChoice.BLUE,
    dark: Boolean = false,
) {
    ActivityCalendarTheme(scheme = scheme, darkTheme = dark) {
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surface)
                .padding(top = 8.dp),
        ) {
            MonthGrid(
                weeks = previewWeeks(),
                selectedDay = selectedDay,
                onDayClick = {},
                modifier = Modifier.padding(horizontal = 6.dp),
            )
            DayPanel(
                selectedDay = selectedDay,
                activities = PREVIEW_ACTIVITIES.filter {
                    it.startTimeLocal.toLocalDate() == selectedDay
                },
                recent = previewRecent(),
            )
        }
    }
}

private fun previewWeeks(): List<List<CalendarDay>> {
    val byDay = PREVIEW_ACTIVITIES.groupBy { it.startTimeLocal.toLocalDate() }
    return YearMonth.of(2026, 9).gridWeeks().map { week ->
        week.map { date ->
            CalendarDay(
                date = date,
                inMonth = YearMonth.from(date) == YearMonth.of(2026, 9),
                isToday = date == TODAY,
                activities = byDay[date].orEmpty(),
            )
        }
    }
}

private fun previewRecent(): PeriodSummary {
    val from = TODAY.minusDays(6)
    val byType = PREVIEW_ACTIVITIES
        .filter { it.startTimeLocal.toLocalDate() >= from }
        .groupBy { it.type }
        .map { (type, activities) ->
            TypeTotals(
                type = type,
                count = activities.size,
                duration = activities.fold(Duration.ZERO) { total, it -> total + it.duration },
                distanceMeters = activities.mapNotNull { it.distanceMeters }
                    .takeIf { it.isNotEmpty() }?.sum(),
            )
        }
        .sortedByDescending { it.duration }
    return PeriodSummary(from = from, to = TODAY, byType = byType)
}

private val TODAY: LocalDate = LocalDate.of(2026, 9, 7)

private val PREVIEW_ACTIVITIES: List<Activity> = listOf(
    preview("2026-08-24T07:20", ActivityType.RUNNING, 38, 6100.0, "Morning Run"),
    preview("2026-08-25T18:30", ActivityType.BADMINTON, 80, null, null),
    preview("2026-08-27T17:10", ActivityType.CYCLING, 64, 25700.0, "Evening Ride"),
    preview("2026-08-28T07:05", ActivityType.RUNNING, 41, 6400.0, null),
    preview("2026-08-29T11:00", ActivityType.BADMINTON, 80, null, "Badminton"),
    preview("2026-08-30T10:15", ActivityType.CYCLING, 55, 18300.0, null),
    preview("2026-09-01T18:30", ActivityType.BADMINTON, 80, null, "Badminton"),
    preview("2026-09-03T07:12", ActivityType.RUNNING, 41, 6400.0, "Morning Run"),
    preview("2026-09-03T18:30", ActivityType.BADMINTON, 80, null, null),
    preview("2026-09-03T21:05", ActivityType.YOGA, 25, null, "Evening Flow"),
    preview("2026-09-05T09:40", ActivityType.BADMINTON, 80, null, null),
    preview("2026-09-06T16:00", ActivityType.CYCLING, 64, 25700.0, null),
    preview("2026-09-07T07:30", ActivityType.RUNNING, 31, 4200.0, "Morning Run"),
    preview("2026-09-07T20:00", ActivityType.YOGA, 25, null, null),
)

private fun preview(
    startTime: String,
    type: ActivityType,
    minutes: Long,
    metres: Double?,
    title: String?,
) = Activity(
    startTimeLocal = LocalDateTime.parse(startTime),
    type = type,
    duration = Duration.ofMinutes(minutes),
    distanceMeters = metres,
    title = title,
    sourceId = null,
    source = ActivitySourceType.HEALTH_CONNECT,
)
