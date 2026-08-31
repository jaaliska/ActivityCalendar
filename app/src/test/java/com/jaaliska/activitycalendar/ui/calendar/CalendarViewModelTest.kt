package com.jaaliska.activitycalendar.ui.calendar

import com.jaaliska.activitycalendar.domain.Activity
import com.jaaliska.activitycalendar.domain.ActivityRepository
import com.jaaliska.activitycalendar.domain.ActivitySourceType
import com.jaaliska.activitycalendar.domain.ActivityType
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

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `an empty database asks for data instead of showing a month`() = runTest(dispatcher) {
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
            historyStart = LocalDate.of(2026, 3, 11),
        )

        val state = month(repository)

        assertEquals(YearMonth.of(2026, 8), state.month)
        assertEquals(6, state.weeks.size)
        assertEquals(
            listOf(ActivityType.RUNNING, ActivityType.STRENGTH_TRAINING),
            state.day(LocalDate.of(2026, 8, 16)).types,
        )
        assertTrue(state.day(LocalDate.of(2026, 8, 17)).types.isEmpty())
    }

    @Test
    fun `a day of a neighbouring month keeps its own activities`() = runTest(dispatcher) {
        val repository = FakeRepository(
            activities = listOf(activity("2026-07-30T18:00:00", ActivityType.BADMINTON)),
            historyStart = LocalDate.of(2026, 3, 11),
        )

        val july30 = month(repository).day(LocalDate.of(2026, 7, 30))

        assertEquals(listOf(ActivityType.BADMINTON), july30.types)
        assertEquals(false, july30.inMonth)
    }

    @Test
    fun `today is marked, other days are not`() = runTest(dispatcher) {
        val state = month(FakeRepository(historyStart = LocalDate.of(2026, 3, 11)))

        assertTrue(state.day(LocalDate.of(2026, 8, 23)).isToday)
        assertEquals(1, state.weeks.flatten().count { it.isToday })
    }

    @Test
    fun `a month inside the history says nothing about where it starts`() = runTest(dispatcher) {
        val state = month(FakeRepository(historyStart = LocalDate.of(2026, 3, 11)))

        assertNull(state.historyStart)
    }

    @Test
    fun `a month earlier than the history names the day it starts on`() = runTest(dispatcher) {
        val state = month(FakeRepository(historyStart = LocalDate.of(2026, 9, 1)))

        assertEquals(LocalDate.of(2026, 9, 1), state.historyStart)
    }

    @Test
    fun `a failed read reports itself and keeps naming the month`() = runTest(dispatcher) {
        assertEquals(CalendarUiState.Failed(YearMonth.of(2026, 8)), stateOf(FailingRepository()))
    }

    @Test
    fun `retry after a failure shows the month`() = runTest(dispatcher) {
        val viewModel = CalendarViewModel(
            FailingRepository(thenReturns = FakeRepository(historyStart = HISTORY_START)),
            clock,
        )
        backgroundScope.launch { viewModel.state.collect {} }
        advanceUntilIdle()
        assertTrue(viewModel.state.value is CalendarUiState.Failed)

        viewModel.retry()
        advanceUntilIdle()

        assertTrue(viewModel.state.value is CalendarUiState.Month)
    }

    /** The state the screen ends up with once the repository has answered. */
    private fun TestScope.stateOf(repository: ActivityRepository): CalendarUiState {
        val viewModel = CalendarViewModel(repository, clock)
        backgroundScope.launch { viewModel.state.collect {} }
        advanceUntilIdle()
        return viewModel.state.value
    }

    private fun TestScope.month(repository: ActivityRepository): CalendarUiState.Month =
        stateOf(repository) as CalendarUiState.Month

    private fun CalendarUiState.Month.day(date: LocalDate): CalendarDay =
        weeks.flatten().single { it.date == date }

    /** Answers straight away, the way a local database does. */
    private class FakeRepository(
        private val activities: List<Activity> = emptyList(),
        private val historyStart: LocalDate? = null,
    ) : ActivityRepository {

        override fun observeRange(from: LocalDate, toExclusive: LocalDate): Flow<List<Activity>> =
            flow { emit(activities.filter { it.startTimeLocal.toLocalDate() in from..<toExclusive }) }

        override fun observeHistoryStart(): Flow<LocalDate?> = flow { emit(historyStart) }

        override suspend fun getMonth(month: YearMonth): List<Activity> = activities

        override suspend fun save(activities: List<Activity>): Int = 0
    }

    /** Fails the first read, then hands over to [thenReturns] if there is one. */
    private class FailingRepository(
        private val thenReturns: ActivityRepository? = null,
    ) : ActivityRepository {

        private var failed = false

        override fun observeRange(from: LocalDate, toExclusive: LocalDate): Flow<List<Activity>> =
            flow {
                if (failed) {
                    thenReturns?.observeRange(from, toExclusive)?.collect { emit(it) }
                } else {
                    failed = true
                    throw IllegalStateException("database is not readable")
                }
            }

        override fun observeHistoryStart(): Flow<LocalDate?> =
            thenReturns?.observeHistoryStart() ?: flow { emit(null) }

        override suspend fun getMonth(month: YearMonth): List<Activity> = emptyList()

        override suspend fun save(activities: List<Activity>): Int = 0
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
            source = ActivitySourceType.MANUAL,
        )
    }
}
