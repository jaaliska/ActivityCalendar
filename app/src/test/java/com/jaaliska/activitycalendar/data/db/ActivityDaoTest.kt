package com.jaaliska.activitycalendar.data.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.jaaliska.activitycalendar.data.source.FixtureActivitySource
import com.jaaliska.activitycalendar.domain.Activity
import com.jaaliska.activitycalendar.domain.ActivitySourceType
import com.jaaliska.activitycalendar.domain.ActivityType
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Duration
import java.time.LocalDateTime
import java.time.Month

@RunWith(RobolectricTestRunner::class)
class ActivityDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: ActivityDao

    private val augustStart = LocalDateTime.of(2026, 8, 1, 0, 0, 0).toDbString()
    private val septemberStart = LocalDateTime.of(2026, 9, 1, 0, 0, 0).toDbString()

    @Before
    fun setUp() {
        database = Room
            .inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .build()
        dao = database.activityDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `august 2026 returns only august activities`() = runTest {
        dao.insertAll(FixtureActivitySource.ALL.map { it.toEntity() })

        val august = dao.getInRange(augustStart, septemberStart).map { it.toDomain() }

        assertEquals(10, august.size)
        assertTrue(august.all { it.startTimeLocal.year == 2026 && it.startTimeLocal.month == Month.AUGUST })
    }

    @Test
    fun `month boundaries are half-open`() = runTest {
        dao.insertAll(FixtureActivitySource.ALL.map { it.toEntity() })

        val august = dao.getInRange(augustStart, septemberStart).map { it.toDomain() }

        assertEquals(LocalDateTime.of(2026, 8, 1, 0, 0, 0), august.first().startTimeLocal)
        assertEquals(LocalDateTime.of(2026, 8, 31, 23, 59, 59), august.last().startTimeLocal)
    }

    @Test
    fun `activities come back in chronological order`() = runTest {
        dao.insertAll(FixtureActivitySource.ALL.shuffled().map { it.toEntity() })

        val august = dao.getInRange(augustStart, septemberStart).map { it.toDomain() }

        assertEquals(august.sortedBy { it.startTimeLocal }, august)
    }

    @Test
    fun `same start and type is not inserted twice`() = runTest {
        val first = sample(type = ActivityType.RUNNING, title = "Первая")
        val duplicate = sample(type = ActivityType.RUNNING, title = "Импортирована повторно")

        dao.insertAll(listOf(first.toEntity()))
        val rowIds = dao.insertAll(listOf(duplicate.toEntity()))

        assertEquals(listOf(-1L), rowIds)
        val stored = dao.getInRange(augustStart, septemberStart)
        assertEquals(1, stored.size)
        assertEquals("Первая", stored.single().title)
    }

    @Test
    fun `same start with a different type is a different activity`() = runTest {
        dao.insertAll(
            listOf(
                sample(type = ActivityType.RUNNING).toEntity(),
                sample(type = ActivityType.STRENGTH_TRAINING).toEntity(),
            )
        )

        assertEquals(2, dao.getInRange(augustStart, septemberStart).size)
    }

    @Test
    fun `nullable fields survive the database`() = runTest {
        dao.insertAll(listOf(sample(type = ActivityType.YOGA).toEntity()))

        val stored = dao.getInRange(augustStart, septemberStart).single().toDomain()

        assertNull(stored.distanceMeters)
        assertNull(stored.sourceId)
        assertEquals(ActivityType.YOGA, stored.type)
        assertEquals(ActivitySourceType.GARMIN_CSV, stored.source)
    }

    @Test
    fun `clear empties the table`() = runTest {
        dao.insertAll(FixtureActivitySource.ALL.map { it.toEntity() })

        dao.clear()

        assertTrue(dao.getInRange(augustStart, septemberStart).isEmpty())
    }

    private fun sample(
        type: ActivityType,
        title: String? = null,
    ) = Activity(
        startTimeLocal = LocalDateTime.of(2026, 8, 18, 18, 30),
        type = type,
        duration = Duration.ofMinutes(45),
        distanceMeters = null,
        title = title,
        sourceId = null,
        source = ActivitySourceType.GARMIN_CSV,
    )
}
