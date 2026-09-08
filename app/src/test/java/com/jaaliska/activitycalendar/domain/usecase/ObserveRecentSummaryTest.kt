package com.jaaliska.activitycalendar.domain.usecase

import com.jaaliska.activitycalendar.domain.Activity
import com.jaaliska.activitycalendar.domain.ActivityRepository
import com.jaaliska.activitycalendar.domain.ActivitySourceType
import com.jaaliska.activitycalendar.domain.ActivityType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

class ObserveRecentSummaryTest {

    @Test
    fun `the period is today and the six days before it`() = runTest {
        val repository = RecordingRepository()

        ObserveRecentSummary(repository)(TODAY).first()

        assertEquals(LocalDate.of(2026, 9, 1), repository.from)
        assertEquals(LocalDate.of(2026, 9, 8), repository.toExclusive)
    }

    @Test
    fun `the summary names the days it covers`() = runTest {
        val summary = summaryOf()

        assertEquals(LocalDate.of(2026, 9, 1), summary.from)
        assertEquals(TODAY, summary.to)
    }

    @Test
    fun `activities of one type are counted and added up together`() = runTest {
        val summary = summaryOf(
            activity("2026-09-02T07:12:00", ActivityType.RUNNING, minutes = 41, metres = 6400.0),
            activity("2026-09-05T18:30:00", ActivityType.RUNNING, minutes = 31, metres = 4200.0),
        )

        val running = summary.byType.single()
        assertEquals(ActivityType.RUNNING, running.type)
        assertEquals(2, running.count)
        assertEquals(Duration.ofMinutes(72), running.duration)
        assertEquals(10600.0, running.distanceMeters!!, 0.01)
    }

    @Test
    fun `a type nobody measured has no distance at all`() = runTest {
        val summary = summaryOf(
            activity("2026-09-03T21:05:00", ActivityType.YOGA, minutes = 25, metres = null),
        )

        assertNull(summary.byType.single().distanceMeters)
    }

    @Test
    fun `a distance recorded by one activity of a type survives the others having none`() =
        runTest {
            val summary = summaryOf(
                activity("2026-09-03T08:00:00", ActivityType.WALKING, minutes = 30, metres = null),
                activity("2026-09-04T08:00:00", ActivityType.WALKING, minutes = 30, metres = 2500.0),
            )

            assertEquals(2500.0, summary.byType.single().distanceMeters!!, 0.01)
        }

    @Test
    fun `the type most time went into comes first`() = runTest {
        val summary = summaryOf(
            activity("2026-09-03T21:05:00", ActivityType.YOGA, minutes = 25, metres = null),
            activity("2026-09-02T18:30:00", ActivityType.BADMINTON, minutes = 80, metres = null),
            activity("2026-09-04T07:12:00", ActivityType.RUNNING, minutes = 41, metres = 6400.0),
        )

        assertEquals(
            listOf(ActivityType.BADMINTON, ActivityType.RUNNING, ActivityType.YOGA),
            summary.byType.map { it.type },
        )
    }

    @Test
    fun `a week without training summarises to nothing`() = runTest {
        val summary = summaryOf()

        assertTrue(summary.isEmpty)
        assertTrue(summary.byType.isEmpty())
    }

    private suspend fun summaryOf(vararg activities: Activity): PeriodSummary =
        ObserveRecentSummary(RecordingRepository(activities.toList()))(TODAY).first()

    /** Answers with what falls into the range it was asked for, and remembers the range. */
    private class RecordingRepository(
        private val activities: List<Activity> = emptyList(),
    ) : ActivityRepository {

        var from: LocalDate? = null
        var toExclusive: LocalDate? = null

        override fun observeRange(
            from: LocalDate,
            toExclusive: LocalDate,
        ): Flow<List<Activity>> {
            this.from = from
            this.toExclusive = toExclusive
            return flow {
                emit(activities.filter { it.startTimeLocal.toLocalDate() in from..<toExclusive })
            }
        }

        override fun observeHistoryStart(): Flow<LocalDate?> = flow { emit(null) }

        override suspend fun getAll(): List<Activity> = emptyList()

        override fun observeCountFrom(source: ActivitySourceType): Flow<Int> = flow { emit(0) }

        override suspend fun getMonth(month: YearMonth): List<Activity> = emptyList()

        override suspend fun save(activities: List<Activity>): Int = 0

        override suspend fun deleteAllFrom(source: ActivitySourceType) = Unit
    }

    private companion object {

        // Monday 7 September 2026, the day the mockups are drawn on.
        val TODAY: LocalDate = LocalDate.of(2026, 9, 7)

        fun activity(
            startTime: String,
            type: ActivityType,
            minutes: Long,
            metres: Double?,
        ) = Activity(
            startTimeLocal = LocalDateTime.parse(startTime),
            type = type,
            duration = Duration.ofMinutes(minutes),
            distanceMeters = metres,
            title = null,
            sourceId = null,
            source = ActivitySourceType.HEALTH_CONNECT,
        )
    }
}
