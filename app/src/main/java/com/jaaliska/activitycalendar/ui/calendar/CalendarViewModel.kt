package com.jaaliska.activitycalendar.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jaaliska.activitycalendar.data.healthconnect.ConnectionStatus
import com.jaaliska.activitycalendar.data.healthconnect.HealthConnectStatus
import com.jaaliska.activitycalendar.domain.Activity
import com.jaaliska.activitycalendar.domain.ActivityRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth

/**
 * Reads the months the calendar pages through. Which month is on screen belongs to the pager;
 * this only answers what is in the three months around it.
 */
class CalendarViewModel(
    private val repository: ActivityRepository,
    private val healthConnectStatus: HealthConnectStatus,
    private val clock: Clock = Clock.systemDefaultZone(),
) : ViewModel() {

    /** The month the calendar opens on, and the month `Today` returns to. */
    val anchor: YearMonth = YearMonth.now(clock)

    private val today: LocalDate = LocalDate.now(clock)

    private val requests = MutableStateFlow(Request(anchor, attempt = 0))

    val state: StateFlow<CalendarUiState> = reads()
        .scan(CalendarUiState.Calendar(today) as CalendarUiState, ::merge)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = CalendarUiState.Calendar(today),
        )

    private val _syncStopped = MutableStateFlow(false)

    /** Health Connect used to fill the calendar and no longer does; the screen says so on top. */
    val syncStopped: StateFlow<Boolean> = _syncStopped.asStateFlow()

    init {
        refreshSyncStatus()
    }

    /** Asks again whether synchronisation still works: permissions change outside the app. */
    fun refreshSyncStatus() {
        viewModelScope.launch {
            _syncStopped.value = healthConnectStatus.current() == ConnectionStatus.Stopped
        }
    }

    /** Says which month the pager has settled on, so its neighbours are read as well. */
    fun showMonth(month: YearMonth) {
        requests.value = requests.value.copy(month = month)
    }

    /** Reads the months again after a failure, without restarting the app. */
    fun retry() {
        requests.value = requests.value.copy(attempt = requests.value.attempt + 1)
    }

    /**
     * Keeps the months already on screen while the next ones are being read: a paged-to month
     * would otherwise blink through an empty grid on every swipe.
     */
    private fun merge(previous: CalendarUiState, read: Read): CalendarUiState = when {
        read.answer != null -> read.answer
        previous is CalendarUiState.Calendar -> previous.copy(reading = read.slow)
        else -> previous
    }

    // A StateFlow already drops repeats, so paging back to a month that is already read
    // does not start the read again.
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun reads(): Flow<Read> = requests
        .flatMapLatest { request ->
            combine(
                stored(request.month)
                    .catch { emit(CalendarUiState.Failed(request.month)) }
                    .onStart { emit(null) },
                slowReadTicker(),
            ) { answer, slow -> Read(answer, slow) }
        }

    private fun stored(month: YearMonth): Flow<CalendarUiState?> {
        val window = month.minusMonths(1)..month.plusMonths(1)
        return combine(
            repository.observeRange(
                window.start.gridStart(),
                window.endInclusive.gridEndExclusive(),
            ),
            repository.observeHistoryStart(),
        ) { activities, historyStart ->
            if (historyStart == null) {
                CalendarUiState.NoData
            } else {
                calendar(window, activities, historyStart)
            }
        }
    }

    private fun calendar(
        window: ClosedRange<YearMonth>,
        activities: List<Activity>,
        historyStart: LocalDate,
    ): CalendarUiState.Calendar {
        val byDay = activities.groupBy { it.startTimeLocal.toLocalDate() }
        val months = generateSequence(window.start) { it.plusMonths(1) }
            .takeWhile { it <= window.endInclusive }
        return CalendarUiState.Calendar(
            today = today,
            pages = months.associateWith { page(it, byDay, historyStart) },
        )
    }

    private fun page(
        month: YearMonth,
        byDay: Map<LocalDate, List<Activity>>,
        historyStart: LocalDate,
    ) = MonthPage(
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
        historyStart = historyStart.takeIf { month.atEndOfMonth() < it },
    )

    // A local read takes milliseconds. The progress line exists for the read that does not.
    private fun slowReadTicker(): Flow<Boolean> = flow {
        emit(false)
        delay(SLOW_READ_MILLIS)
        emit(true)
    }

    /** What the state is being read for: a month to show, and which attempt at it this is. */
    private data class Request(val month: YearMonth, val attempt: Int)

    /** One step of a read: the answer if it has arrived, and whether it is taking long. */
    private data class Read(val answer: CalendarUiState?, val slow: Boolean)

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
        const val SLOW_READ_MILLIS = 200L
    }
}
