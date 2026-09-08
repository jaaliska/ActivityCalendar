package com.jaaliska.activitycalendar.ui.calendar

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jaaliska.activitycalendar.R
import com.jaaliska.activitycalendar.domain.Activity
import com.jaaliska.activitycalendar.domain.ActivityType
import com.jaaliska.activitycalendar.domain.usecase.PeriodSummary
import com.jaaliska.activitycalendar.domain.usecase.TypeTotals
import com.jaaliska.activitycalendar.ui.UI_DAY
import com.jaaliska.activitycalendar.ui.UI_TIME
import java.time.LocalDate

/**
 * The area under the grid: the day that was picked, or the last seven days while none is.
 *
 * @param recent the last seven days, null while they have not been read
 * @param activities what the picked day holds, empty when it holds nothing
 */
@Composable
fun DayPanel(
    selectedDay: LocalDate?,
    activities: List<Activity>,
    recent: PeriodSummary?,
    modifier: Modifier = Modifier,
) {
    if (selectedDay == null && recent == null) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp)
            .padding(top = 10.dp),
    ) {
        PanelHeading(selectedDay = selectedDay, activities = activities, recent = recent)

        AnimatedContent(
            targetState = selectedDay != null,
            transitionSpec = {
                (fadeIn(tween(TRANSITION_MILLIS)) + slideInVertically { it / SLIDE_FRACTION })
                    .togetherWith(
                        fadeOut(tween(TRANSITION_MILLIS)) +
                            slideOutVertically { -it / SLIDE_FRACTION },
                    )
            },
            label = "panel",
        ) { dayPicked ->
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                if (dayPicked) DayBody(activities) else recent?.let { RecentBody(it) }
            }
        }
    }
}

@Composable
private fun PanelHeading(
    selectedDay: LocalDate?,
    activities: List<Activity>,
    recent: PeriodSummary?,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = HEADING_HEIGHT)
            .padding(bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = selectedDay?.format(UI_DAY) ?: stringResource(R.string.panel_recent_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        val caption = if (selectedDay == null) {
            recent?.let { periodText(it.from, it.to) }
        } else {
            pluralStringResource(
                R.plurals.panel_day_activities,
                activities.size,
                activities.size,
            )
        }
        caption?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun RecentBody(recent: PeriodSummary) {
    if (recent.isEmpty) {
        EmptyBody(stringResource(R.string.panel_nothing_recent))
        return
    }
    ColumnHeader()
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant,
        modifier = Modifier.padding(top = 6.dp, bottom = 2.dp),
    )
    recent.byType.forEach { TotalsRow(it) }
}

@Composable
private fun ColumnHeader() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        HeaderCell(
            text = stringResource(R.string.panel_column_activity),
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(COLUMN_GAP))
        HeaderCell(
            text = stringResource(R.string.panel_column_distance),
            modifier = Modifier.width(DISTANCE_COLUMN),
            align = TextAlign.End,
        )
        Spacer(Modifier.width(COLUMN_GAP))
        HeaderCell(
            text = stringResource(R.string.panel_column_time),
            modifier = Modifier.width(TIME_COLUMN),
            align = TextAlign.End,
        )
    }
}

@Composable
private fun HeaderCell(text: String, modifier: Modifier = Modifier, align: TextAlign? = null) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = HEADER_TRACKING),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = align,
        modifier = modifier,
    )
}

@Composable
private fun TotalsRow(totals: TypeTotals) {
    Row(
        modifier = Modifier.height(TOTALS_ROW_HEIGHT),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RowIcon(totals.type)
        Spacer(Modifier.width(ICON_GAP))
        Text(
            text = typeCountText(totals.type, totals.count),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(COLUMN_GAP))
        NumberCell(
            text = totals.distance()?.let { distanceText(it) },
            modifier = Modifier.width(DISTANCE_COLUMN),
        )
        Spacer(Modifier.width(COLUMN_GAP))
        NumberCell(text = durationText(totals.duration), modifier = Modifier.width(TIME_COLUMN))
    }
}

@Composable
private fun NumberCell(text: String?, modifier: Modifier = Modifier) {
    Text(
        text = text.orEmpty(),
        style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = TABULAR_FIGURES),
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.End,
        modifier = modifier,
    )
}

@Composable
private fun RowIcon(type: ActivityType) {
    Box(modifier = Modifier.width(ICON_BOX), contentAlignment = Alignment.Center) {
        ActivityIcon(type = type, size = ROW_ICON)
    }
}

@Composable
private fun DayBody(activities: List<Activity>) {
    activities.forEach { ActivityRow(it) }
}

@Composable
private fun ActivityRow(activity: Activity) {
    Row(
        modifier = Modifier.padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RowIcon(activity.type)
        Spacer(Modifier.width(ACTIVITY_ICON_GAP))
        Column {
            Text(
                text = activity.title?.takeIf { it.isNotBlank() }
                    ?: stringResource(activity.type.labelRes()),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = activity.meta(),
                style = MaterialTheme.typography.bodySmall
                    .copy(fontFeatureSettings = TABULAR_FIGURES),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun Activity.meta(): String = listOfNotNull(
    startTimeLocal.format(UI_TIME),
    durationText(duration),
    distance()?.let { distanceText(it) },
).joinToString(META_SEPARATOR)

private fun Activity.distance(): Double? = distanceMeters?.takeIf { type.showsDistance }

private fun TypeTotals.distance(): Double? = distanceMeters?.takeIf { type.showsDistance }

@Composable
private fun EmptyBody(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 14.dp),
    )
}

private const val META_SEPARATOR = " · "
private const val TABULAR_FIGURES = "tnum"
private const val TRANSITION_MILLIS = 200
private const val SLIDE_FRACTION = 8
private val HEADER_TRACKING = 1.sp
private val HEADING_HEIGHT = 22.dp
private val TOTALS_ROW_HEIGHT = 44.dp
private val DISTANCE_COLUMN = 78.dp
private val TIME_COLUMN = 82.dp
private val COLUMN_GAP = 8.dp
private val ICON_BOX = 20.dp
private val ICON_GAP = 10.dp
private val ACTIVITY_ICON_GAP = 12.dp
private val ROW_ICON = 18.dp
