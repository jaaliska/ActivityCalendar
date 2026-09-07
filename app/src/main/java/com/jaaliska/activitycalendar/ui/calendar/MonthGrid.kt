package com.jaaliska.activitycalendar.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jaaliska.activitycalendar.R
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/** The six-week grid of a month: weekday row on top, a cell per day below. */
@Composable
fun MonthGrid(
    weeks: List<List<CalendarDay>>,
    selectedDay: LocalDate?,
    onDayClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(CELL_GAP),
    ) {
        WeekdayRow()
        weeks.forEach { week ->
            Row(horizontalArrangement = Arrangement.spacedBy(CELL_GAP)) {
                week.forEach { day ->
                    DayCell(
                        day = day,
                        selected = day.date == selectedDay,
                        onClick = { onDayClick(day.date) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun WeekdayRow() {
    Row(
        modifier = Modifier.padding(bottom = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(CELL_GAP),
    ) {
        DayOfWeek.entries.forEach { day ->
            Text(
                text = day.getDisplayName(TextStyle.SHORT, Locale.ENGLISH),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun DayCell(
    day: CalendarDay,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val outline = MaterialTheme.colorScheme.onSurface
    Box(
        modifier = modifier
            .height(CELL_HEIGHT)
            .alpha(if (day.inMonth) 1f else ADJACENT_MONTH_ALPHA)
            .clip(CELL_SHAPE)
            .background(cellColor(day, selected))
            .then(if (day.isToday) Modifier.border(2.dp, outline, CELL_SHAPE) else Modifier)
            .clickable(onClick = onClick)
            .padding(top = 3.dp, start = 4.dp, end = 4.dp, bottom = 2.dp),
    ) {
        Text(
            text = day.date.dayOfMonth.toString(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ActivityMarks(
            day = day,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 2.dp),
        )
    }
}

@Composable
private fun cellColor(day: CalendarDay, selected: Boolean): Color = when {
    selected -> MaterialTheme.colorScheme.secondaryContainer
    day.hasActivity -> MaterialTheme.colorScheme.surfaceContainerHighest
    else -> MaterialTheme.colorScheme.surfaceContainerLow
}

/** Up to two icons side by side, the rest of the day's activities counted as `+N`. */
@Composable
private fun ActivityMarks(day: CalendarDay, modifier: Modifier = Modifier) {
    val types = day.types
    if (types.isEmpty()) return

    val shown = types.take(MAX_ICONS)
    val hidden = types.size - shown.size
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(1.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        shown.forEach { type ->
            ActivityIcon(type = type, size = if (shown.size == 1) SINGLE_ICON else STACKED_ICON)
        }
        if (hidden > 0) {
            Text(
                text = stringResource(R.string.calendar_more_activities, hidden),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                softWrap = false,
            )
        }
    }
}

private val CELL_HEIGHT = 56.dp
private val CELL_GAP = 1.dp
private val CELL_SHAPE = RoundedCornerShape(8.dp)
private val SINGLE_ICON = 18.dp
private val STACKED_ICON = 16.dp
private const val MAX_ICONS = 2
private const val ADJACENT_MONTH_ALPHA = 0.45f
