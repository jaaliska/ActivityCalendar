package com.jaaliska.activitycalendar.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.jaaliska.activitycalendar.data.db.AppDatabase
import com.jaaliska.activitycalendar.data.source.FixtureActivitySource
import com.jaaliska.activitycalendar.domain.ActivityRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
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
        repository = RoomActivityRepository(database.activityDao())
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
    fun `observeMonth emits the same data as getMonth`() = runTest {
        repository.save(FixtureActivitySource.ALL)

        assertEquals(repository.getMonth(august), repository.observeMonth(august).first())
    }

    @Test
    fun `a neighbouring month is a different result`() = runTest {
        repository.save(FixtureActivitySource.ALL)

        assertEquals(2, repository.getMonth(YearMonth.of(2026, 9)).size)
        assertEquals(2, repository.getMonth(YearMonth.of(2026, 7)).size)
    }

    @Test
    fun `save reports how many rows actually landed`() = runTest {
        assertEquals(FixtureActivitySource.ALL.size, repository.save(FixtureActivitySource.ALL))

        assertEquals(0, repository.save(FixtureActivitySource.ALL))
        assertEquals(10, repository.getMonth(august).size)
    }
}
