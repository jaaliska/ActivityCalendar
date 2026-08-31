package com.jaaliska.activitycalendar.ui.calendar

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jaaliska.activitycalendar.R
import com.jaaliska.activitycalendar.ui.UI_DATE
import com.jaaliska.activitycalendar.ui.UI_MONTH
import java.time.LocalDate
import java.time.YearMonth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    state: CalendarUiState,
    onSettingsClick: () -> Unit,
    onImportClick: () -> Unit,
    onHealthConnectClick: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
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

                is CalendarUiState.Month -> {
                    MonthRow(month = state.month, reading = state.reading)
                    MonthGrid(
                        weeks = state.weeks,
                        modifier = Modifier.padding(horizontal = 6.dp),
                    )
                    state.historyStart?.let { HistoryStart(it, onImportClick) }
                }
            }
        }
    }
}

/** Month name with the paging controls; they start paging in block 6. */
@Composable
private fun MonthRow(month: YearMonth, reading: Boolean = false) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = {}) {
                Icon(
                    painter = painterResource(R.drawable.ic_chevron_left),
                    contentDescription = stringResource(R.string.calendar_previous_month),
                )
            }
            Text(
                text = month.format(UI_MONTH),
                style = MaterialTheme.typography.titleMedium,
            )
            IconButton(onClick = {}) {
                Icon(
                    painter = painterResource(R.drawable.ic_chevron_right),
                    contentDescription = stringResource(R.string.calendar_next_month),
                )
            }
            TextButton(onClick = {}, modifier = Modifier.padding(start = 8.dp)) {
                Text(stringResource(R.string.calendar_today))
            }
        }
        if (reading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(2.dp))
        }
    }
}

/** Under a month earlier than anything stored: what the history is, and how to extend it. */
@Composable
private fun HistoryStart(start: LocalDate, onImportClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp, start = 16.dp, end = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(R.string.calendar_history_start, start.format(UI_DATE)),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.calendar_add_older_history),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier.clickable(onClick = onImportClick),
        )
    }
}
