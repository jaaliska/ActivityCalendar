package com.jaaliska.activitycalendar.ui.activities

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jaaliska.activitycalendar.domain.Activity
import com.jaaliska.activitycalendar.domain.ActivityRepository
import java.time.Duration
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Flat list of one month's activities, grouped by day. */
@Composable
fun ActivityListScreen(
    repository: ActivityRepository,
    month: YearMonth,
    modifier: Modifier = Modifier,
) {
    val activitiesFlow = remember(repository, month) { repository.observeMonth(month) }
    val activities by activitiesFlow.collectAsState(initial = emptyList())

    Column(modifier = modifier.fillMaxSize()) {
        Text(
            text = month.format(MONTH_FORMAT).replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(16.dp),
        )
        HorizontalDivider()

        if (activities.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "За этот месяц активностей нет",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        } else {
            ActivitiesByDay(activities)
        }
    }
}

@Composable
private fun ActivitiesByDay(activities: List<Activity>) {
    val byDay = remember(activities) { activities.groupBy { it.startTimeLocal.toLocalDate() } }

    LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
        byDay.forEach { (day, ofDay) ->
            item(key = day) { DayHeader(day) }
            items(ofDay, key = { it.id }) { ActivityRow(it) }
        }
    }
}

@Composable
private fun DayHeader(day: LocalDate) {
    Text(
        text = day.format(DAY_FORMAT),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
    )
}

@Composable
private fun ActivityRow(activity: Activity) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text(
                text = activity.title ?: activity.type.name,
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = activity.startTimeLocal.format(TIME_FORMAT),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = formatDuration(activity.duration),
                style = MaterialTheme.typography.bodyMedium,
            )
            activity.distanceMeters?.let { meters ->
                Text(
                    text = String.format(Locale.getDefault(), "%.1f км", meters / 1000),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun formatDuration(duration: Duration): String {
    val hours = duration.toHours()
    val minutes = duration.toMinutes() % 60
    return when {
        hours > 0 && minutes > 0 -> "$hours ч $minutes мин"
        hours > 0 -> "$hours ч"
        else -> "$minutes мин"
    }
}

private val MONTH_FORMAT = DateTimeFormatter.ofPattern("LLLL yyyy", Locale.getDefault())
private val DAY_FORMAT = DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.getDefault())
private val TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())
