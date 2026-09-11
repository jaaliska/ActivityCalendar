package com.jaaliska.activitycalendar.ui.settings

import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.jaaliska.activitycalendar.data.csv.GarminCsvParser
import com.jaaliska.activitycalendar.data.csv.GarminCsvWriter
import com.jaaliska.activitycalendar.data.db.AppDatabase
import com.jaaliska.activitycalendar.data.file.AssetDemoDataFile
import com.jaaliska.activitycalendar.data.repository.RoomActivityRepository
import com.jaaliska.activitycalendar.domain.ActivityRepository
import com.jaaliska.activitycalendar.domain.ColorSchemeChoice
import com.jaaliska.activitycalendar.domain.FakeAppearanceSettings
import com.jaaliska.activitycalendar.domain.FakeImportHistory
import com.jaaliska.activitycalendar.domain.healthconnect.FakeHealthConnectSource
import com.jaaliska.activitycalendar.domain.healthconnect.FakeHealthConnectSyncState
import com.jaaliska.activitycalendar.domain.usecase.ExportActivities
import com.jaaliska.activitycalendar.domain.usecase.GetHealthConnectStatus
import com.jaaliska.activitycalendar.domain.usecase.LoadDemoData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import com.jaaliska.activitycalendar.ui.file.FileSource
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

@RunWith(RobolectricTestRunner::class)
class SettingsViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val appearance = FakeAppearanceSettings()
    private val exportTarget = RecordingFileSource()

    private lateinit var database: AppDatabase
    private lateinit var repository: ActivityRepository
    private lateinit var loadDemoData: LoadDemoData

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        repository = RoomActivityRepository(database)
        loadDemoData = LoadDemoData(
            repository = repository,
            parser = GarminCsvParser(),
            file = AssetDemoDataFile(context.assets),
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        database.close()
    }

    @Test
    fun `picked scheme is remembered and shown`() = runTest(dispatcher) {
        val viewModel = viewModel()

        viewModel.selectColorScheme(ColorSchemeChoice.GREEN)

        val state = viewModel.state.first { it.colorScheme == ColorSchemeChoice.GREEN }
        assertEquals(ColorSchemeChoice.GREEN, state.colorScheme)
        assertEquals(ColorSchemeChoice.GREEN, appearance.colorScheme.value)
    }

    @Test
    fun `screen opens on the scheme the app is painted with`() = runTest(dispatcher) {
        appearance.colorScheme.value = ColorSchemeChoice.CRIMSON

        val state = viewModel().state.first { it.colorScheme != ColorSchemeChoice.BLUE }

        assertEquals(ColorSchemeChoice.CRIMSON, state.colorScheme)
    }

    @Test
    fun `exporting writes every stored activity into the picked file`() = runTest(dispatcher) {
        val viewModel = viewModel()
        viewModel.loadDemo()
        val loaded = viewModel.state.first { it.demoActivities > 0 }

        viewModel.export(Uri.parse("content://export/history.csv"))

        val message = viewModel.state.first { it.message != null }.message
        assertEquals(SettingsMessage.Exported(loaded.demoActivities), message)
        assertEquals(
            loaded.demoActivities,
            exportTarget.written().lineSequence().filter { it.isNotBlank() }.count() - 1,
        )
    }

    @Test
    fun `an unwritable file is reported as a failed export`() = runTest(dispatcher) {
        exportTarget.failing = true
        val viewModel = viewModel()

        viewModel.export(Uri.parse("content://export/history.csv"))

        assertEquals(
            SettingsMessage.ExportFailed,
            viewModel.state.first { it.message != null }.message,
        )
    }

    @Test
    fun `demo data that cannot be loaded says so instead of failing quietly`() =
        runTest(dispatcher) {
            val viewModel = viewModel(
                LoadDemoData(
                    repository = repository,
                    parser = GarminCsvParser(),
                    file = { throw IOException("no such asset") },
                ),
            )

            viewModel.loadDemo()

            assertEquals(
                SettingsMessage.DemoFailed,
                viewModel.state.first { it.message != null }.message,
            )
        }

    @Test
    fun `the demo row counts the samples and empties when they are removed`() = runTest(dispatcher) {
        val viewModel = viewModel()
        // One collector for the whole test: the state is only built while a screen looks at it.
        backgroundScope.launch { viewModel.state.collect {} }

        viewModel.loadDemo()
        val loaded = viewModel.state.first { it.demoActivities > 0 }
        // The row fills as soon as the database commits, while the load is still finishing;
        // tapping again before it does is what the screen ignores on purpose.
        advanceUntilIdle()

        viewModel.removeDemo()
        val removed = viewModel.state.first { it.demoActivities == 0 }

        assertTrue(loaded.demoActivities > 0)
        assertEquals(0, removed.demoActivities)
    }

    /** A file the export writes into, kept in memory. */
    private class RecordingFileSource : FileSource {

        var failing = false

        private val file = ByteArrayOutputStream()

        fun written(): String = file.toString(Charsets.UTF_8.name())

        override fun openForWriting(uri: Uri): OutputStream =
            if (failing) throw IOException("no room on the device") else file

        override fun open(uri: Uri): InputStream = ByteArrayInputStream(file.toByteArray())

        override fun displayName(uri: Uri): String = "history.csv"
    }

    private fun viewModel(demoData: LoadDemoData = loadDemoData) = SettingsViewModel(
        importHistory = FakeImportHistory(),
        repository = repository,
        appearanceSettings = appearance,
        getHealthConnectStatus = GetHealthConnectStatus(
            FakeHealthConnectSource(),
            FakeHealthConnectSyncState(),
        ),
        loadDemoData = demoData,
        exportActivities = ExportActivities(repository, GarminCsvWriter()),
        fileSource = exportTarget,
        ioDispatcher = dispatcher,
    )
}
