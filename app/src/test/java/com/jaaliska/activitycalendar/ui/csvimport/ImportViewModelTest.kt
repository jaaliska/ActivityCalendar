package com.jaaliska.activitycalendar.ui.csvimport

import android.content.Context
import android.net.Uri
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.jaaliska.activitycalendar.data.csv.CsvImporter
import com.jaaliska.activitycalendar.data.db.AppDatabase
import com.jaaliska.activitycalendar.data.file.FileSource
import com.jaaliska.activitycalendar.data.repository.RoomActivityRepository
import com.jaaliska.activitycalendar.data.settings.ImportHistory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayInputStream
import java.io.FileNotFoundException
import java.io.InputStream
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

@RunWith(RobolectricTestRunner::class)
class ImportViewModelTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val dispatcher = StandardTestDispatcher()
    private val uri: Uri = Uri.parse("content://test/export.csv")

    private lateinit var database: AppDatabase
    private lateinit var repository: RoomActivityRepository
    private lateinit var importHistory: ImportHistory

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        repository = RoomActivityRepository(database)
        importHistory = ImportHistory(
            PreferenceDataStoreFactory.create(
                scope = CoroutineScope(Job() + Dispatchers.IO),
                produceFile = { temporaryFolder.newFile("settings.preferences_pb") },
            ),
        )
    }

    @After
    fun tearDown() {
        database.close()
        Dispatchers.resetMain()
    }

    @Test
    fun `a Garmin export ends in a report and lands in the repository`() = runTest(dispatcher) {
        val viewModel = viewModelReading(fixture())

        viewModel.import(uri)

        val state = viewModel.finished()
        assertTrue("expected a report, got $state", state is ImportUiState.Done)
        val report = (state as ImportUiState.Done).report
        assertEquals(8, report.imported)
        assertEquals(2, report.skippedRows)
        assertEquals(LocalDate.of(2026, 3, 11), report.from)
        assertEquals(3, repository.getMonth(YearMonth.of(2026, 8)).size)
    }

    @Test
    fun `a successful import is remembered as the last one`() = runTest(dispatcher) {
        val viewModel = viewModelReading(fixture(), clock = clockAt(LocalDate.of(2026, 8, 27)))

        viewModel.import(uri)
        viewModel.finished()

        assertEquals(LocalDate.of(2026, 8, 27), importHistory.lastImport.first())
    }

    @Test
    fun `a file without the needed columns changes nothing`() = runTest(dispatcher) {
        val viewModel = viewModelReading("one;two\r\n1;2\r\n".byteInputStream())

        viewModel.import(uri)

        assertEquals(
            ImportUiState.Failed("export.csv", ImportUiState.Failed.Reason.NOT_A_GARMIN_EXPORT),
            viewModel.finished(),
        )
        assertTrue(repository.getMonth(YearMonth.of(2026, 8)).isEmpty())
        assertEquals(null, importHistory.lastImport.first())
    }

    @Test
    fun `a file of nothing but a header imports zero activities without an error`() =
        runTest(dispatcher) {
            val header = "Activity Type,Date,Title,Distance,Time\r\n"
            val viewModel = viewModelReading(header.byteInputStream())

            viewModel.import(uri)

            val state = viewModel.finished()
            assertTrue("expected a report, got $state", state is ImportUiState.Done)
            assertEquals(0, (state as ImportUiState.Done).report.imported)
        }

    @Test
    fun `a file that cannot be opened is reported as unreadable`() = runTest(dispatcher) {
        val viewModel = viewModel(object : FileSource {
            override fun open(uri: Uri): InputStream = throw FileNotFoundException()
            override fun displayName(uri: Uri): String = "gone.csv"
        })

        viewModel.import(uri)

        assertEquals(
            ImportUiState.Failed("gone.csv", ImportUiState.Failed.Reason.UNREADABLE),
            viewModel.finished(),
        )
    }

    @Test
    fun `the file being read is named while the import runs`() = runTest(dispatcher) {
        val viewModel = viewModelReading(fixture())

        viewModel.import(uri)

        assertEquals(ImportUiState.Running("export.csv"), viewModel.state.value)
    }

    /** Waits for the import to leave the running state, as the screen does. */
    private suspend fun ImportViewModel.finished(): ImportUiState =
        state.first { it !is ImportUiState.Running }

    private fun viewModelReading(
        content: InputStream,
        clock: Clock = clockAt(LocalDate.of(2026, 8, 27)),
    ) = viewModel(
        object : FileSource {
            override fun open(uri: Uri): InputStream = content
            override fun displayName(uri: Uri): String = "export.csv"
        },
        clock,
    )

    private fun viewModel(
        fileSource: FileSource,
        clock: Clock = clockAt(LocalDate.of(2026, 8, 27)),
    ) = ImportViewModel(
        importer = CsvImporter(repository),
        fileSource = fileSource,
        importHistory = importHistory,
        clock = clock,
        ioDispatcher = dispatcher,
    )

    private fun clockAt(date: LocalDate): Clock =
        Clock.fixed(date.atStartOfDay(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault())

    private fun fixture(): InputStream =
        ByteArrayInputStream(
            checkNotNull(javaClass.classLoader).getResourceAsStream("garmin-activities.csv")
                .readBytes(),
        )
}
