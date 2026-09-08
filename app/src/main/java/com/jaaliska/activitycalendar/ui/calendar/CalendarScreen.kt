package com.jaaliska.activitycalendar.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.RowScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.jaaliska.activitycalendar.R
import com.jaaliska.activitycalendar.ui.UI_DATE
import com.jaaliska.activitycalendar.ui.UI_MONTH
import com.jaaliska.activitycalendar.ui.components.OnResume
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    state: CalendarUiState,
    anchor: YearMonth,
    syncStopped: Boolean,
    onMonthSettled: (YearMonth) -> Unit,
    onDaySelected: (LocalDate) -> Unit,
    onTodayClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onImportClick: () -> Unit,
    onHealthConnectClick: () -> Unit,
    onRetry: () -> Unit,
    onScreenResumed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OnResume(onScreenResumed)

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            painter = painterResource(R.drawable.ic_settings),
                            contentDescription = stringResource(R.string.action_settings),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            if (syncStopped) {
                SyncStoppedBanner(onFixClick = onHealthConnectClick)
            }

            when (state) {
                // The month row is hidden, not disabled: there is no calendar to page through yet.
                CalendarUiState.NoData -> NoDataState(
                    onImportClick = onImportClick,
                    onHealthConnectClick = onHealthConnectClick,
                )

                is CalendarUiState.Failed -> {
                    MonthRow(month = state.month)
                    LoadErrorState(onRetry = onRetry)
                }

                is CalendarUiState.Calendar -> MonthPager(
                    state = state,
                    anchor = anchor,
                    onMonthSettled = onMonthSettled,
                    onDaySelected = onDaySelected,
                    onTodayClick = onTodayClick,
                    onImportClick = onImportClick,
                )
            }
        }
    }
}

/** The months, one page each, with the row that names and moves them. */
@Composable
private fun MonthPager(
    state: CalendarUiState.Calendar,
    anchor: YearMonth,
    onMonthSettled: (YearMonth) -> Unit,
    onDaySelected: (LocalDate) -> Unit,
    onTodayClick: () -> Unit,
    onImportClick: () -> Unit,
) {
    val pagerState = rememberPagerState(initialPage = ANCHOR_PAGE) { PAGE_COUNT }
    val scope = rememberCoroutineScope()
    val shownMonth by remember(anchor) {
        derivedStateOf { monthAt(pagerState.currentPage, anchor) }
    }
    var pickerShown by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(pagerState, anchor) {
        snapshotFlow { pagerState.settledPage }
            .collect { page -> onMonthSettled(monthAt(page, anchor)) }
    }

    MonthRow(
        month = shownMonth,
        reading = state.reading,
        onPrevious = { scope.launch { pagerState.animateScrollToPage(pagerState.targetPage - 1) } },
        onNext = { scope.launch { pagerState.animateScrollToPage(pagerState.targetPage + 1) } },
        onPickMonth = { pickerShown = true },
        onToday = {
            onTodayClick()
            scope.launch { pagerState.animateScrollToPage(ANCHOR_PAGE) }
        },
    )

    HorizontalPager(
        state = pagerState,
        verticalAlignment = Alignment.Top,
        beyondViewportPageCount = 1,
        modifier = Modifier.height(MONTH_GRID_HEIGHT),
    ) { page ->
        MonthGrid(
            weeks = state.page(monthAt(page, anchor)).weeks,
            selectedDay = state.selectedDay,
            onDayClick = onDaySelected,
            modifier = Modifier.padding(horizontal = 6.dp),
        )
    }

    state.page(shownMonth).historyStart?.let { HistoryStart(it, onImportClick) }

    DayPanel(
        selectedDay = state.selectedDay,
        activities = state.selectedDayActivities,
        recent = state.recent,
    )

    if (pickerShown) {
        MonthYearDialog(
            shown = shownMonth,
            onPick = { month ->
                pickerShown = false
                scope.launch { pagerState.scrollToPage(pageOf(month, anchor)) }
            },
            onDismiss = { pickerShown = false },
        )
    }
}

/** Month name with the controls that move the calendar through time. */
@Composable
private fun MonthRow(
    month: YearMonth,
    reading: Boolean = false,
    onPrevious: () -> Unit = {},
    onNext: () -> Unit = {},
    onPickMonth: () -> Unit = {},
    onToday: () -> Unit = {},
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onPrevious) {
                Icon(
                    painter = painterResource(R.drawable.ic_chevron_left),
                    contentDescription = stringResource(R.string.calendar_previous_month),
                )
            }
            TextButton(onClick = onPickMonth) {
                Box(contentAlignment = Alignment.Center) {
                    // Holds the width of the longest month name, so neither the arrow next
                    // to it nor the grid below it moves as the months go by.
                    Text(
                        text = WIDEST_MONTH,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        modifier = Modifier.alpha(0f),
                    )
                    Text(
                        text = month.format(UI_MONTH),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                    )
                }
            }
            IconButton(onClick = onNext) {
                Icon(
                    painter = painterResource(R.drawable.ic_chevron_right),
                    contentDescription = stringResource(R.string.calendar_next_month),
                )
            }
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onToday) {
                Text(stringResource(R.string.calendar_today))
            }
        }
        if (reading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(2.dp))
        }
    }
}

/** Above the month while Health Connect has stopped filling it: what happened, and where to fix it. */
@Composable
private fun SyncStoppedBanner(onFixClick: () -> Unit) {
    Banner(icon = R.drawable.ic_sync_disabled, onClick = onFixClick) {
        Text(
            text = stringResource(R.string.calendar_sync_banner),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = stringResource(R.string.calendar_sync_banner_action),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

/** Under a month earlier than anything stored: what the history is, and how to extend it. */
@Composable
private fun HistoryStart(start: LocalDate, onImportClick: () -> Unit) {
    Banner(icon = R.drawable.ic_schedule, onClick = onImportClick) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.calendar_history_start, start.format(UI_DATE)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                text = stringResource(R.string.calendar_add_older_history),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

/** The plaque the calendar says things with: an icon, the message and a way to act on it. */
@Composable
private fun Banner(
    @DrawableRes icon: Int,
    onClick: () -> Unit,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(BANNER_SHAPE)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp),
        )
        content()
    }
}

private val BANNER_SHAPE = RoundedCornerShape(12.dp)

// The longest month name the row has to hold; the digits stand in for any year.
private const val WIDEST_MONTH = "September 0000"
