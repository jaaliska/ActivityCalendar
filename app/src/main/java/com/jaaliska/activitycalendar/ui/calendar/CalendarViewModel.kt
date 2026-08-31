package com.jaaliska.activitycalendar.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jaaliska.activitycalendar.domain.Activity
import com.jaaliska.activitycalendar.domain.ActivityRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth

/** Reads the month the calendar shows; the shown month itself starts moving in block 6. */
class CalendarViewModel(
    private val repository: ActivityRepository,
    private val clock: Clock = Clock.systemDefaultZone(),
) : ViewModel() {

    private val month: YearMonth = YearMonth.now(clock)

    private val attempts = MutableStateFlow(0)

    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<CalendarUiState> = attempts
        .flatMapLatest { readMonth() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = monthState(activities = emptyList(), historyStart = null),
        )

    /** Reads the month again after a failure, without restarting the app. */
    fun retry() {
        attempts.value += 1
    }

    private fun readMonth(): Flow<CalendarUiState> =
        combine(stored(), slowReadTicker()) { state, slow ->
            state ?: monthState(emptyList(), historyStart = null, reading = slow)
        }.catch { emit(CalendarUiState.Failed(month)) }

    /** The state the database dictates; null until it has answered, so the grid can be drawn. */
    private fun stored(): Flow<CalendarUiState?> {
        val answers: Flow<CalendarUiState?> = combine(
            repository.observeRange(month.gridStart(), month.gridEndExclusive()),
            repository.observeHistoryStart(),
        ) { activities, historyStart ->
            if (historyStart == null) {
                CalendarUiState.NoData
            } else {
                monthState(activities, historyStart)
            }
        }
        return answers.onStart { emit(null) }
    }

    private fun monthState(
        activities: List<Activity>,
        historyStart: LocalDate?,
        reading: Boolean = false,
    ): CalendarUiState.Month {
        val byDay = activities.groupBy { it.startTimeLocal.toLocalDate() }
        val today = LocalDate.now(clock)
        return CalendarUiState.Month(
            month = month,
            weeks = month.gridWeeks().map { week ->
                week.map { date ->
                    CalendarDay(
                        date = date,
                        inMonth = YearMonth.from(date) == month,
                        isToday = date == today,
                        types = byDay[date].orEmpty().map { it.type },
                    )
                }
            },
            historyStart = historyStart?.takeIf { month.atEndOfMonth() < it },
            reading = reading,
        )
    }

    // A local read takes milliseconds. The progress line exists for the read that does not.
    private fun slowReadTicker(): Flow<Boolean> = flow {
        emit(false)
        delay(SLOW_READ_MILLIS)
        emit(true)
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
        const val SLOW_READ_MILLIS = 200L
    }
}
