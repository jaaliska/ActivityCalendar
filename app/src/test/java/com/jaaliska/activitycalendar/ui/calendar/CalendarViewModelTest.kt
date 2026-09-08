package com.jaaliska.activitycalendar.ui.calendar

import com.jaaliska.activitycalendar.domain.Activity
import com.jaaliska.activitycalendar.domain.ActivityRepository
import com.jaaliska.activitycalendar.domain.ActivitySourceType
import com.jaaliska.activitycalendar.domain.ParsedActivities
import com.jaaliska.activitycalendar.domain.ActivityType
import com.jaaliska.activitycalendar.domain.healthconnect.FakeHealthConnectSource
import com.jaaliska.activitycalendar.domain.healthconnect.FakeHealthConnectSyncState
import com.jaaliska.activitycalendar.domain.usecase.GetHealthConnectStatus
import com.jaaliska.activitycalendar.domain.usecase.LoadDemoData
import com.jaaliska.activitycalendar.domain.usecase.ObserveCalendarMonths
import com.jaaliska.activitycalendar.domain.usecase.ObserveRecentSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.InputStream
import java.time.Clock
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId

class CalendarViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    // Sunday 23 August 2026, the day the mockups are drawn on.
    private val clock: Clock =
        Clock.fixed(LocalDate.of(2026, 8, 23).atStartOfDay(ZONE).toInstant(), ZONE)

    private val august = YearMonth.of(2026, 8)

    private val neverConnected = GetHealthConnectStatus(
        source = FakeHealthConnectSource(requiredPermissionsGranted = false),
        syncState = FakeHealthConnectSyncState(),
    )

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `an empty database asks for data instead of showing a calendar`() = runTest(dispatcher) {
        assertEquals(CalendarUiState.NoData, stateOf(FakeRepository()))
    }

    @Test
    fun `a month lays its activities out over six weeks of days`() = runTest(dispatcher) {
        val repository = FakeRepository(
            activities = listOf(
                activity("2026-08-16T07:20:00", ActivityType.RUNNING),
                activity("2026-08-16T12:05:00", ActivityType.STRENGTH_TRAINING),
                activity("2026-07-30T18:00:00", ActivityType.BADMINTON),
            ),
            historyStart = HISTORY_START,
        )

        val page = calendar(repository).page(august)

        assertEquals(august, page.month)
        assertEquals(6, page.weeks.size)
        assertEquals(
            listOf(ActivityType.RUNNING, ActivityType.STRENGTH_TRAINING),
            page.dayAt(LocalDate.of(2026, 8, 16)).types,
        )
        assertTrue(page.dayAt(LocalDate.of(2026, 8, 17)).types.isEmpty())
    }

    @Test
    fun `a day of a neighbouring month keeps its own activities`() = runTest(dispatcher) {
        val repository = FakeRepository(
            activities = listOf(activity("2026-07-30T18:00:00", ActivityType.BADMINTON)),
            historyStart = HISTORY_START,
        )

        val july30 = calendar(repository).page(august).dayAt(LocalDate.of(2026, 7, 30))

        assertEquals(listOf(ActivityType.BADMINTON), july30.types)
        assertEquals(false, july30.inMonth)
    }

    @Test
    fun `today is marked, other days are not`() = runTest(dispatcher) {
        val page = calendar(FakeRepository(historyStart = HISTORY_START)).page(august)

        assertTrue(page.dayAt(LocalDate.of(2026, 8, 23)).isToday)
        assertEquals(1, page.weeks.flatten().count { it.isToday })
    }

    @Test
    fun `the months around the shown one are read too`() = runTest(dispatcher) {
        val state = calendar(FakeRepository(historyStart = HISTORY_START))

        assertEquals(
            setOf(YearMonth.of(2026, 7), august, YearMonth.of(2026, 9)),
            state.pages.keys,
        )
    }

    @Test
    fun `a month too far to have been read is drawn empty, not with a neighbour's data`() =
        runTest(dispatcher) {
            val repository = FakeRepository(
                activities = listOf(activity("2026-08-16T07:20:00", ActivityType.RUNNING)),
                historyStart = HISTORY_START,
            )

            val far = calendar(repository).page(YearMonth.of(2021, 3))

            assertEquals(YearMonth.of(2021, 3), far.month)
            assertEquals(6, far.weeks.size)
            assertTrue(far.weeks.flatten().all { it.types.isEmpty() })
        }

    @Test
    fun `paging to another month reads that month`() = runTest(dispatcher) {
        val repository = FakeRepository(
            activities = listOf(activity("2021-03-04T07:20:00", ActivityType.YOGA)),
            historyStart = LocalDate.of(2019, 1, 1),
        )
        val viewModel = viewModelOn(repository)
        backgroundScope.launch { viewModel.state.collect {} }
        advanceUntilIdle()

        viewModel.showMonth(YearMonth.of(2021, 3))
        advanceUntilIdle()

        val page = (viewModel.state.value as CalendarUiState.Calendar).page(YearMonth.of(2021, 3))
        assertEquals(listOf(ActivityType.YOGA), page.dayAt(LocalDate.of(2021, 3, 4)).types)
    }

    @Test
    fun `a flurry of paging ends on the last month asked for`() = runTest(dispatcher) {
        val repository = FakeRepository(
            activities = listOf(activity("2021-03-04T07:20:00", ActivityType.YOGA)),
            historyStart = LocalDate.of(2019, 1, 1),
        )
        val viewModel = viewModelOn(repository)
        backgroundScope.launch { viewModel.state.collect {} }

        (1..12).forEach { viewModel.showMonth(august.minusMonths(it.toLong())) }
        viewModel.showMonth(YearMonth.of(2021, 3))
        advanceUntilIdle()

        val state = viewModel.state.value as CalendarUiState.Calendar
        assertEquals(
            setOf(YearMonth.of(2021, 2), YearMonth.of(2021, 3), YearMonth.of(2021, 4)),
            state.pages.keys,
        )
    }

    @Test
    fun `a month inside the history says nothing about where it starts`() = runTest(dispatcher) {
        val state = calendar(FakeRepository(historyStart = HISTORY_START))

        assertNull(state.page(august).historyStart)
    }

    @Test
    fun `a month earlier than the history names the day it starts on`() = runTest(dispatcher) {
        val state = calendar(FakeRepository(historyStart = LocalDate.of(2026, 9, 1)))

        assertEquals(LocalDate.of(2026, 9, 1), state.page(august).historyStart)
    }

    @Test
    fun `a failed read reports itself and keeps naming the month`() = runTest(dispatcher) {
        assertEquals(CalendarUiState.Failed(august), stateOf(FailingRepository()))
    }

    @Test
    fun `retry after a failure shows the calendar`() = runTest(dispatcher) {
        val viewModel = viewModelOn(
            FailingRepository(thenReturns = FakeRepository(historyStart = HISTORY_START)),
        )
        backgroundScope.launch { viewModel.state.collect {} }
        advanceUntilIdle()
        assertTrue(viewModel.state.value is CalendarUiState.Failed)

        viewModel.retry()
        advanceUntilIdle()

        assertTrue(viewModel.state.value is CalendarUiState.Calendar)
    }

    @Test
    fun `picking a day shows it, picking it again shows the period back`() = runTest(dispatcher) {
        val viewModel = running(FakeRepository(historyStart = HISTORY_START))
        val august16 = LocalDate.of(2026, 8, 16)

        viewModel.selectDay(august16)
        advanceUntilIdle()
        assertEquals(august16, calendarOf(viewModel).selectedDay)

        viewModel.selectDay(august16)
        advanceUntilIdle()
        assertNull(calendarOf(viewModel).selectedDay)
    }

    @Test
    fun `the picked day stays picked however far the calendar is paged away`() =
        runTest(dispatcher) {
            val repository = FakeRepository(
                activities = listOf(activity("2026-08-16T07:20:00", ActivityType.RUNNING)),
                historyStart = HISTORY_START,
            )
            val viewModel = running(repository)

            viewModel.selectDay(LocalDate.of(2026, 8, 16))
            viewModel.showMonth(YearMonth.of(2027, 3))
            advanceUntilIdle()

            val state = calendarOf(viewModel)
            assertEquals(LocalDate.of(2026, 8, 16), state.selectedDay)
            assertEquals(
                listOf(ActivityType.RUNNING),
                state.selectedDayActivities.map { it.type },
            )
        }

    @Test
    fun `the picked day carries its activities, earliest first`() = runTest(dispatcher) {
        val repository = FakeRepository(
            activities = listOf(
                activity("2026-08-16T12:05:00", ActivityType.STRENGTH_TRAINING),
                activity("2026-08-16T07:20:00", ActivityType.RUNNING),
            ),
            historyStart = HISTORY_START,
        )
        val viewModel = running(repository)

        viewModel.selectDay(LocalDate.of(2026, 8, 16))
        advanceUntilIdle()

        assertEquals(
            listOf(ActivityType.RUNNING, ActivityType.STRENGTH_TRAINING),
            calendarOf(viewModel).selectedDayActivities.map { it.type },
        )
    }

    @Test
    fun `a picked day of a neighbouring month carries its activities too`() = runTest(dispatcher) {
        val repository = FakeRepository(
            activities = listOf(activity("2026-09-05T18:00:00", ActivityType.BADMINTON)),
            historyStart = HISTORY_START,
        )
        val viewModel = running(repository)

        viewModel.selectDay(LocalDate.of(2026, 9, 5))
        advanceUntilIdle()

        assertEquals(
            listOf(ActivityType.BADMINTON),
            calendarOf(viewModel).selectedDayActivities.map { it.type },
        )
    }

    @Test
    fun `the panel adds up the seven days ending today`() = runTest(dispatcher) {
        val repository = FakeRepository(
            activities = listOf(
                activity("2026-08-23T07:20:00", ActivityType.RUNNING),
                activity("2026-08-17T07:20:00", ActivityType.RUNNING),
                // One day before the period starts.
                activity("2026-08-16T07:20:00", ActivityType.YOGA),
            ),
            historyStart = HISTORY_START,
        )

        val recent = calendarOf(running(repository)).recent!!

        assertEquals(LocalDate.of(2026, 8, 17), recent.from)
        assertEquals(LocalDate.of(2026, 8, 23), recent.to)
        assertEquals(listOf(ActivityType.RUNNING), recent.byType.map { it.type })
        assertEquals(2, recent.byType.single().count)
    }

    /** The state the screen ends up with once the repository has answered. */
    private fun TestScope.stateOf(repository: ActivityRepository): CalendarUiState {
        val viewModel = viewModelOn(repository)
        backgroundScope.launch { viewModel.state.collect {} }
        advanceUntilIdle()
        return viewModel.state.value
    }

    private fun TestScope.calendar(repository: ActivityRepository): CalendarUiState.Calendar =
        stateOf(repository) as CalendarUiState.Calendar

    private fun MonthPage.dayAt(date: LocalDate): CalendarDay =
        weeks.flatten().single { it.date == date }

    /** A view model whose state is already being collected, the way the screen collects it. */
    private fun TestScope.running(repository: ActivityRepository): CalendarViewModel {
        val viewModel = viewModelOn(repository)
        backgroundScope.launch { viewModel.state.collect {} }
        advanceUntilIdle()
        return viewModel
    }

    private fun calendarOf(viewModel: CalendarViewModel) =
        viewModel.state.value as CalendarUiState.Calendar

    private fun viewModelOn(repository: ActivityRepository) = CalendarViewModel(
        observeCalendarMonths = ObserveCalendarMonths(repository),
        observeRecentSummary = ObserveRecentSummary(repository),
        repository = repository,
        getHealthConnectStatus = neverConnected,
        loadDemoData = LoadDemoData(
            repository = repository,
            parser = { ParsedActivities(emptyList(), emptyList()) },
            file = { InputStream.nullInputStream() },
        ),
        clock = clock,
    )

    /** Answers straight away, the way a local database does. */
    private class FakeRepository(
        private val activities: List<Activity> = emptyList(),
        private val historyStart: LocalDate? = null,
    ) : ActivityRepository {

        override fun observeRange(from: LocalDate, toExclusive: LocalDate): Flow<List<Activity>> =
            flow {
                emit(
                    activities
                        .filter { it.startTimeLocal.toLocalDate() in from..<toExclusive }
                        .sortedBy { it.startTimeLocal },
                )
            }

        override fun observeHistoryStart(): Flow<LocalDate?> = flow { emit(historyStart) }

        override suspend fun getAll(): List<Activity> = emptyList()

        override fun observeCountFrom(source: ActivitySourceType): Flow<Int> = flow { emit(0) }

        override suspend fun getMonth(month: YearMonth): List<Activity> = activities

        override suspend fun save(activities: List<Activity>): Int = 0

        override suspend fun deleteAllFrom(source: ActivitySourceType) = Unit
    }

    /**
     * Fails the first read of every range it is asked for, then hands over to [thenReturns]
     * if there is one.
     */
    private class FailingRepository(
        private val thenReturns: ActivityRepository? = null,
    ) : ActivityRepository {

        private val failed = mutableSetOf<Pair<LocalDate, LocalDate>>()

        override fun observeRange(from: LocalDate, toExclusive: LocalDate): Flow<List<Activity>> =
            flow {
                if (failed.add(from to toExclusive)) {
                    throw IllegalStateException("database is not readable")
                }
                thenReturns?.observeRange(from, toExclusive)?.collect { emit(it) }
            }

        override fun observeHistoryStart(): Flow<LocalDate?> =
            thenReturns?.observeHistoryStart() ?: flow { emit(null) }

        override suspend fun getAll(): List<Activity> = emptyList()

        override fun observeCountFrom(source: ActivitySourceType): Flow<Int> = flow { emit(0) }

        override suspend fun getMonth(month: YearMonth): List<Activity> = emptyList()

        override suspend fun save(activities: List<Activity>): Int = 0

        override suspend fun deleteAllFrom(source: ActivitySourceType) = Unit
    }

    private companion object {

        val ZONE: ZoneId = ZoneId.of("Europe/Warsaw")
        val HISTORY_START: LocalDate = LocalDate.of(2026, 3, 11)

        fun activity(startTime: String, type: ActivityType) = Activity(
            startTimeLocal = LocalDateTime.parse(startTime),
            type = type,
            duration = Duration.ofMinutes(40),
            distanceMeters = null,
            title = null,
            sourceId = null,
            source = ActivitySourceType.GARMIN_CSV,
        )
    }
}
