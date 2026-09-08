package com.jaaliska.activitycalendar.ui.settings

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.jaaliska.activitycalendar.data.csv.GarminCsvParser
import com.jaaliska.activitycalendar.data.db.AppDatabase
import com.jaaliska.activitycalendar.data.file.AssetDemoDataFile
import com.jaaliska.activitycalendar.data.repository.RoomActivityRepository
import com.jaaliska.activitycalendar.domain.ActivityRepository
import com.jaaliska.activitycalendar.domain.ColorSchemeChoice
import com.jaaliska.activitycalendar.domain.FakeAppearanceSettings
import com.jaaliska.activitycalendar.domain.FakeImportHistory
import com.jaaliska.activitycalendar.domain.healthconnect.FakeHealthConnectSource
import com.jaaliska.activitycalendar.domain.healthconnect.FakeHealthConnectSyncState
import com.jaaliska.activitycalendar.domain.usecase.GetHealthConnectStatus
import com.jaaliska.activitycalendar.domain.usecase.LoadDemoData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
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

@RunWith(RobolectricTestRunner::class)
class SettingsViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val appearance = FakeAppearanceSettings()

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
    fun `the demo row counts the samples and empties when they are removed`() = runTest(dispatcher) {
        val viewModel = viewModel()

        viewModel.loadDemo()
        val loaded = viewModel.state.first { it.demoActivities > 0 }

        viewModel.removeDemo()
        val removed = viewModel.state.first { it.demoActivities == 0 }

        assertTrue(loaded.demoActivities > 0)
        assertEquals(0, removed.demoActivities)
    }

    private fun viewModel() = SettingsViewModel(
        importHistory = FakeImportHistory(),
        repository = repository,
        appearanceSettings = appearance,
        getHealthConnectStatus = GetHealthConnectStatus(
            FakeHealthConnectSource(),
            FakeHealthConnectSyncState(),
        ),
        loadDemoData = loadDemoData,
    )
}
