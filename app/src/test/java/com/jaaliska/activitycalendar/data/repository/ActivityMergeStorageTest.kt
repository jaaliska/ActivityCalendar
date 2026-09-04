package com.jaaliska.activitycalendar.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.jaaliska.activitycalendar.data.db.AppDatabase
import com.jaaliska.activitycalendar.domain.Activity
import com.jaaliska.activitycalendar.domain.ActivityRepository
import com.jaaliska.activitycalendar.domain.ActivitySourceType
import com.jaaliska.activitycalendar.domain.ActivityType
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Duration
import java.time.LocalDateTime
import java.time.YearMonth

/** Epic D: two sources filling one calendar. */
@RunWith(RobolectricTestRunner::class)
class ActivityMergeStorageTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: ActivityRepository

    private val august = YearMonth.of(2026, 8)
    private val start = LocalDateTime.of(2026, 8, 12, 7, 30, 0)

    private val fromCsv = activity(
        start = start,
        source = ActivitySourceType.GARMIN_CSV,
        duration = Duration.ofMinutes(38),
        distanceMeters = 6500.0,
        title = "Warsaw Running",
    )

    private val fromHealthConnect = activity(
        start = start,
        source = ActivitySourceType.HEALTH_CONNECT,
        duration = Duration.ofSeconds(2311),
        distanceMeters = 6483.2,
        title = null,
        sourceId = "garmin-1",
    )

    @Before
    fun setUp() {
        database = Room
            .inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .build()
        repository = RoomActivityRepository(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `the same activity from both sources is one record`() = runTest {
        assertEquals(1, repository.save(listOf(fromCsv)))
        assertEquals(0, repository.save(listOf(fromHealthConnect)))

        val stored = repository.getMonth(august)

        assertEquals(1, stored.size)
        assertEquals("Warsaw Running", stored.single().title)
        assertEquals(6483.2, stored.single().distanceMeters!!, 0.001)
    }

    @Test
    fun `the other order gives the same record`() = runTest {
        repository.save(listOf(fromHealthConnect))
        repository.save(listOf(fromCsv))

        val stored = repository.getMonth(august).single()

        assertEquals("Warsaw Running", stored.title)
        assertEquals(6483.2, stored.distanceMeters!!, 0.001)
        assertEquals(Duration.ofSeconds(2311), stored.duration)
    }

    @Test
    fun `two apps writing one run into Health Connect give one record`() = runTest {
        val fromStrava = fromHealthConnect.copy(sourceId = "strava-1")

        assertEquals(1, repository.save(listOf(fromHealthConnect, fromStrava)))
        assertEquals(0, repository.save(listOf(fromHealthConnect, fromStrava)))

        assertEquals(1, repository.getMonth(august).size)
    }

    @Test
    fun `two workouts of one kind on one day stay two`() = runTest {
        val evening = fromCsv.copy(startTimeLocal = start.withHour(18).withMinute(0))
        val lateEvening = fromCsv.copy(startTimeLocal = start.withHour(18).withMinute(45))

        repository.save(listOf(evening, lateEvening))

        assertEquals(2, repository.getMonth(august).size)
    }

    @Test
    fun `different kinds starting at the same moment stay two`() = runTest {
        val strength = fromCsv.copy(type = ActivityType.STRENGTH_TRAINING)
        val yoga = fromCsv.copy(type = ActivityType.YOGA)

        repository.save(listOf(strength, yoga))

        assertEquals(2, repository.getMonth(august).size)
    }

    @Test
    fun `a second apart is not the same activity`() = runTest {
        val onTheHour = fromCsv.copy(startTimeLocal = start.withHour(17).withMinute(0))
        val secondLater = onTheHour.copy(startTimeLocal = onTheHour.startTimeLocal.plusSeconds(1))

        repository.save(listOf(onTheHour, secondLater))

        assertEquals(2, repository.getMonth(august).size)
    }

    @Test
    fun `an activity running past midnight belongs to the day it started`() = runTest {
        val beforeMidnight = fromCsv.copy(
            startTimeLocal = LocalDateTime.of(2026, 8, 12, 23, 40, 0),
            duration = Duration.ofMinutes(40),
        )

        repository.save(listOf(beforeMidnight))
        repository.save(listOf(beforeMidnight.copy(source = ActivitySourceType.HEALTH_CONNECT)))

        val stored = repository.getMonth(august)

        assertEquals(1, stored.size)
        assertEquals(12, stored.single().startTimeLocal.dayOfMonth)
    }

    @Test
    fun `merging keeps a filled field the other source does not have`() = runTest {
        repository.save(listOf(fromCsv.copy(distanceMeters = null)))
        repository.save(listOf(fromHealthConnect))

        assertEquals(6483.2, repository.getMonth(august).single().distanceMeters!!, 0.001)
    }

    @Test
    fun `a repeated sync leaves the stored record untouched`() = runTest {
        repository.save(listOf(fromCsv))
        repository.save(listOf(fromHealthConnect))
        val afterFirstMerge = repository.getMonth(august).single()

        repository.save(listOf(fromHealthConnect))

        assertEquals(afterFirstMerge, repository.getMonth(august).single())
    }

    private fun activity(
        start: LocalDateTime,
        source: ActivitySourceType,
        duration: Duration,
        distanceMeters: Double?,
        title: String?,
        sourceId: String? = null,
    ) = Activity(
        startTimeLocal = start,
        type = ActivityType.RUNNING,
        duration = duration,
        distanceMeters = distanceMeters,
        title = title,
        sourceId = sourceId,
        source = source,
    )
}
