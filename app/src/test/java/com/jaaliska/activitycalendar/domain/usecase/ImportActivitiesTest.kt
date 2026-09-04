package com.jaaliska.activitycalendar.domain.usecase

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.jaaliska.activitycalendar.data.csv.GarminCsvParser
import com.jaaliska.activitycalendar.data.db.AppDatabase
import com.jaaliska.activitycalendar.data.repository.RoomActivityRepository
import com.jaaliska.activitycalendar.domain.FakeImportHistory
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.InputStream
import java.time.LocalDate
import java.time.YearMonth

@RunWith(RobolectricTestRunner::class)
class ImportActivitiesTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: RoomActivityRepository
    private lateinit var importActivities: ImportActivities

    @Before
    fun setUp() {
        database = Room
            .inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .build()
        repository = RoomActivityRepository(database)
        importActivities = ImportActivities(
            repository = repository,
            parser = GarminCsvParser(),
            importHistory = FakeImportHistory(),
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `an import stores every activity of the file once`() = runTest {
        val report = importActivities(fixture())

        assertEquals(8, report.imported)
        assertEquals(1, report.duplicates)
        assertEquals(2, report.skippedRows)
    }

    @Test
    fun `the report covers the period the file spans`() = runTest {
        val report = importActivities(fixture())

        assertEquals(LocalDate.of(2026, 3, 11), report.from)
        assertEquals(LocalDate.of(2026, 8, 18), report.to)
    }

    @Test
    fun `importing the same file twice adds nothing`() = runTest {
        importActivities(fixture())

        val second = importActivities(fixture())

        assertEquals(0, second.imported)
        assertEquals(9, second.duplicates)
        assertEquals(2, second.skippedRows)
    }

    @Test
    fun `imported activities are readable by month`() = runTest {
        importActivities(fixture())

        val august = repository.getMonth(YearMonth.of(2026, 8))

        assertEquals(3, august.size)
        assertEquals(LocalDate.of(2026, 8, 11), august.first().startTimeLocal.toLocalDate())
    }

    private fun fixture(): InputStream = checkNotNull(
        javaClass.getResourceAsStream("/garmin-activities.csv")
    )
}
