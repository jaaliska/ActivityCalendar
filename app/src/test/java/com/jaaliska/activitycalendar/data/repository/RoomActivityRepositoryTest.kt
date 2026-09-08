package com.jaaliska.activitycalendar.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.jaaliska.activitycalendar.data.db.AppDatabase
import com.jaaliska.activitycalendar.data.source.FixtureActivitySource
import com.jaaliska.activitycalendar.domain.ActivityRepository
import com.jaaliska.activitycalendar.domain.ActivitySourceType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Month
import java.time.YearMonth

@RunWith(RobolectricTestRunner::class)
class RoomActivityRepositoryTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: ActivityRepository

    private val august = YearMonth.of(2026, 8)

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
    fun `getMonth returns the whole month and nothing else`() = runTest {
        repository.save(FixtureActivitySource.ALL)

        val stored = repository.getMonth(august)

        assertEquals(10, stored.size)
        assertEquals(setOf(Month.AUGUST), stored.map { it.startTimeLocal.month }.toSet())
    }

    @Test
    fun `observeRange emits the same data as getMonth over the same days`() = runTest {
        repository.save(FixtureActivitySource.ALL)

        val observed = repository
            .observeRange(august.atDay(1), august.plusMonths(1).atDay(1))
            .first()

        assertEquals(repository.getMonth(august), observed)
    }

    @Test
    fun `observeHistoryStart is null until something is stored`() = runTest {
        assertNull(repository.observeHistoryStart().first())

        repository.save(FixtureActivitySource.ALL)

        assertEquals(
            FixtureActivitySource.ALL.minOf { it.startTimeLocal }.toLocalDate(),
            repository.observeHistoryStart().first(),
        )
    }

    @Test
    fun `a neighbouring month is a different result`() = runTest {
        repository.save(FixtureActivitySource.ALL)

        assertEquals(2, repository.getMonth(YearMonth.of(2026, 9)).size)
        assertEquals(2, repository.getMonth(YearMonth.of(2026, 7)).size)
    }

    @Test
    fun `deleting one source leaves the rows of the others`() = runTest {
        val own = FixtureActivitySource.ALL
        val demo = own.map {
            it.copy(
                startTimeLocal = it.startTimeLocal.plusYears(1),
                source = ActivitySourceType.DEMO,
            )
        }
        repository.save(own + demo)

        repository.deleteAllFrom(ActivitySourceType.DEMO)

        assertEquals(0, repository.observeCountFrom(ActivitySourceType.DEMO).first())
        assertEquals(own.size, repository.observeCountFrom(ActivitySourceType.GARMIN_CSV).first())
    }

    @Test
    fun `save reports how many rows actually landed`() = runTest {
        assertEquals(FixtureActivitySource.ALL.size, repository.save(FixtureActivitySource.ALL))

        assertEquals(0, repository.save(FixtureActivitySource.ALL))
        assertEquals(10, repository.getMonth(august).size)
    }
}
