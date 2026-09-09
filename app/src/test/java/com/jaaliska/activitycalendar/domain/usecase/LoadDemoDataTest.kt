package com.jaaliska.activitycalendar.domain.usecase

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.jaaliska.activitycalendar.data.csv.GarminCsvParser
import com.jaaliska.activitycalendar.data.db.AppDatabase
import com.jaaliska.activitycalendar.data.file.AssetDemoDataFile
import com.jaaliska.activitycalendar.data.repository.RoomActivityRepository
import com.jaaliska.activitycalendar.domain.ActivityRepository
import com.jaaliska.activitycalendar.domain.ActivitySourceType
import com.jaaliska.activitycalendar.domain.ActivityType
import com.jaaliska.activitycalendar.domain.DemoDataFile
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

@RunWith(RobolectricTestRunner::class)
class LoadDemoDataTest {

    // A Thursday, so the current week is only half over when the samples land.
    private val now = LocalDateTime.of(2027, 2, 11, 21, 0)
    private val today = now.toLocalDate()
    private val clock = Clock.fixed(now.atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault())

    private lateinit var database: AppDatabase
    private lateinit var repository: ActivityRepository
    private lateinit var file: DemoDataFile
    private lateinit var loadDemoData: LoadDemoData

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        repository = RoomActivityRepository(database)
        file = AssetDemoDataFile(context.assets)
        loadDemoData = LoadDemoData(repository, GarminCsvParser(), file, clock)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `the shipped file is read whole and shows every type the app draws`() {
        val parsed = file.open().use { GarminCsvParser().parse(it) }

        assertEquals(emptyList<Any>(), parsed.skippedRows)
        assertEquals(ActivityType.entries.toSet(), parsed.activities.map { it.type }.toSet())
    }

    @Test
    fun `the samples end in the last seven days and keep their weekday and time`() = runTest {
        loadDemoData()

        val newestStored = stored().maxOf { it.startTimeLocal }
        assertTrue(newestStored.toLocalDate() in today.minusDays(6)..today)
        val sameWeekday = stored().all { activity ->
            weekdaysInFile.contains(activity.startTimeLocal.dayOfWeek)
        }
        assertTrue("samples moved off their weekdays", sameWeekday)
    }

    @Test
    fun `the last seven days are filled whatever day the samples are loaded on`() = runTest {
        loadDemoData()

        val lastWeek = stored().filter { it.startTimeLocal.toLocalDate() >= today.minusDays(6) }

        assertTrue("only ${lastWeek.size} in the last seven days", lastWeek.size >= 4)
    }

    @Test
    fun `no sample lands in the future`() = runTest {
        loadDemoData()

        assertTrue(stored().all { it.startTimeLocal <= now })
    }

    @Test
    fun `every sample is stored as demo data`() = runTest {
        val added = loadDemoData()

        val stored = stored()
        assertEquals(added, stored.size)
        assertEquals(setOf(ActivitySourceType.DEMO), stored.map { it.source }.toSet())
        assertEquals(stored.size, repository.observeCountFrom(ActivitySourceType.DEMO).first())
    }

    @Test
    fun `loading the samples twice does not double them`() = runTest {
        val added = loadDemoData()

        assertEquals(0, loadDemoData())
        assertEquals(added, stored().size)
    }

    private val weekdaysInFile: Set<java.time.DayOfWeek>
        get() = file.open().use { GarminCsvParser().parse(it) }
            .activities
            .map { it.startTimeLocal.dayOfWeek }
            .toSet()

    private suspend fun stored() = repository
        .observeRange(LocalDate.of(1970, 1, 1), today.plusDays(1))
        .first()
}
