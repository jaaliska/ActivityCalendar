package com.jaaliska.activitycalendar.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jaaliska.activitycalendar.domain.Activity
import com.jaaliska.activitycalendar.domain.ActivityRepository
import com.jaaliska.activitycalendar.domain.healthconnect.ConnectionStatus
import com.jaaliska.activitycalendar.domain.usecase.CalendarWindow
import com.jaaliska.activitycalendar.domain.usecase.DayActivities
import com.jaaliska.activitycalendar.domain.usecase.GetHealthConnectStatus
import com.jaaliska.activitycalendar.domain.usecase.LoadDemoData
import com.jaaliska.activitycalendar.domain.usecase.MonthActivities
import com.jaaliska.activitycalendar.domain.usecase.ObserveCalendarMonths
import com.jaaliska.activitycalendar.domain.usecase.ObserveRecentSummary
import com.jaaliska.activitycalendar.domain.usecase.PeriodSummary
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
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
    private val observeCalendarMonths: ObserveCalendarMonths,
    private val observeRecentSummary: ObserveRecentSummary,
    private val repository: ActivityRepository,
    private val getHealthConnectStatus: GetHealthConnectStatus,
    private val loadDemoData: LoadDemoData,
    private val clock: Clock = Clock.systemDefaultZone(),
) : ViewModel() {

    /** The month the calendar opens on, and the month `Today` returns to. */
    val anchor: YearMonth = YearMonth.now(clock)

    private val today: LocalDate = LocalDate.now(clock)

    private val requests = MutableStateFlow(Request(anchor, attempt = 0))

    private val selectedDay = MutableStateFlow<LocalDate?>(null)

    val state: StateFlow<CalendarUiState> = combine(
        reads().scan(CalendarUiState.Calendar(today) as CalendarUiState, ::merge),
        selections(),
        recentSummaries(),
        ::withPanel,
    )
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = CalendarUiState.Calendar(today),
        )

    private val _demoFailed = MutableStateFlow(false)

    /** The sample activities could not be stored; the screen says so and forgets it. */
    val demoFailed: StateFlow<Boolean> = _demoFailed.asStateFlow()

    private val _syncStopped = MutableStateFlow(false)

    /** Health Connect used to fill the calendar and no longer does; the screen says so on top. */
    val syncStopped: StateFlow<Boolean> = _syncStopped.asStateFlow()

    init {
        refreshSyncStatus()
    }

    /** Asks again whether synchronisation still works: permissions change outside the app. */
    fun refreshSyncStatus() {
        viewModelScope.launch {
            _syncStopped.value = getHealthConnectStatus() == ConnectionStatus.Stopped
        }
    }

    /** Says which month the pager has settled on, so its neighbours are read as well. */
    fun showMonth(month: YearMonth) {
        requests.value = requests.value.copy(month = month)
    }

    /** Picks the day the panel describes; picking the day already picked shows the period again. */
    fun selectDay(date: LocalDate) {
        selectedDay.value = date.takeIf { it != selectedDay.value }
    }

    /** Drops the picked day, so the panel goes back to the last seven days. */
    fun clearDaySelection() {
        selectedDay.value = null
    }

    fun loadDemo() {
        viewModelScope.launch {
            runCatching { loadDemoData() }.onFailure { _demoFailed.value = true }
        }
    }

    /** The screen has told the user that the sample activities did not load. */
    fun demoFailureShown() {
        _demoFailed.value = false
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

    private fun withPanel(
        state: CalendarUiState,
        selection: Selection,
        recent: PeriodSummary?,
    ): CalendarUiState =
        if (state is CalendarUiState.Calendar) {
            state.copy(
                selectedDay = selection.day,
                selectedDayActivities = selection.activities,
                recent = recent,
            )
        } else {
            state
        }

    // The picked day is read on its own, not taken from the months on screen: it stays picked
    // however far the calendar is paged away from it.
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun selections(): Flow<Selection> = selectedDay.flatMapLatest { day ->
        if (day == null) {
            flowOf(Selection())
        } else {
            repository.observeRange(day, day.plusDays(1))
                .map { activities -> Selection(day, activities) }
                .catch { emit(Selection(day)) }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun recentSummaries(): Flow<PeriodSummary?> = requests
        .map { it.attempt }
        .distinctUntilChanged()
        .flatMapLatest {
            observeRecentSummary(today)
                .map<PeriodSummary, PeriodSummary?> { summary -> summary }
                .onStart { emit(null) }
                .catch { emit(null) }
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

    private fun stored(month: YearMonth): Flow<CalendarUiState?> =
        observeCalendarMonths(month).map { window ->
            if (window.historyStart == null) CalendarUiState.NoData else window.calendar()
        }

    private fun CalendarWindow.calendar() = CalendarUiState.Calendar(
        today = today,
        pages = months.associate { it.month to it.page() },
    )

    private fun MonthActivities.page() = MonthPage(
        month = month,
        weeks = weeks.map { week -> week.map { it.day() } },
        historyStart = historyStart,
    )

    private fun DayActivities.day() = CalendarDay(
        date = date,
        inMonth = inMonth,
        isToday = date == today,
        activities = activities,
    )

    // A local read takes milliseconds. The progress line exists for the read that does not.
    private fun slowReadTicker(): Flow<Boolean> = flow {
        emit(false)
        delay(SLOW_READ_MILLIS)
        emit(true)
    }

    /** What the state is being read for: a month to show, and which attempt at it this is. */
    private data class Request(val month: YearMonth, val attempt: Int)

    /** The picked day together with what it holds, so the panel never shows one without the other. */
    private data class Selection(
        val day: LocalDate? = null,
        val activities: List<Activity> = emptyList(),
    )

    /** One step of a read: the answer if it has arrived, and whether it is taking long. */
    private data class Read(val answer: CalendarUiState?, val slow: Boolean)

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
        const val SLOW_READ_MILLIS = 200L
    }
}
