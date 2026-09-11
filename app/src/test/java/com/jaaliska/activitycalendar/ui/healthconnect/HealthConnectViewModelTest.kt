package com.jaaliska.activitycalendar.ui.healthconnect

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.jaaliska.activitycalendar.data.db.AppDatabase
import com.jaaliska.activitycalendar.domain.healthconnect.FakeHealthConnectSource
import com.jaaliska.activitycalendar.domain.healthconnect.HealthConnectAvailability
import com.jaaliska.activitycalendar.domain.healthconnect.healthConnectActivity
import com.jaaliska.activitycalendar.data.repository.RoomActivityRepository
import com.jaaliska.activitycalendar.data.settings.DataStoreHealthConnectSyncState
import com.jaaliska.activitycalendar.domain.healthconnect.HealthConnectSyncState
import com.jaaliska.activitycalendar.domain.usecase.SyncHealthConnect
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@RunWith(RobolectricTestRunner::class)
class HealthConnectViewModelTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val dispatcher = StandardTestDispatcher()
    private val source = FakeHealthConnectSource()

    private lateinit var database: AppDatabase
    private lateinit var syncState: HealthConnectSyncState
    private lateinit var syncHealthConnect: SyncHealthConnect

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        syncState = DataStoreHealthConnectSyncState(
            PreferenceDataStoreFactory.create(
                scope = CoroutineScope(Job() + Dispatchers.IO),
                produceFile = { temporaryFolder.newFile("sync.preferences_pb") },
            ),
        )
        syncHealthConnect = SyncHealthConnect(
            source = source,
            repository = RoomActivityRepository(database),
            syncState = syncState,
            clock = clock,
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        database.close()
    }

    @Test
    fun `a phone without Health Connect is offered the app store`() = runTest(dispatcher) {
        source.availability = HealthConnectAvailability.NOT_INSTALLED

        val state = stateOf(viewModel())

        assertEquals(HealthConnectUiState.NotAvailable(canInstall = true), state)
    }

    @Test
    fun `without every permission the screen asks to connect`() = runTest(dispatcher) {
        source.requiredPermissionsGranted = false

        val state = stateOf(viewModel())

        assertEquals(HealthConnectUiState.NotConnected(canAsk = true), state)
    }

    @Test
    fun `granting only some permissions sends the next attempt to Health Connect settings`() =
        runTest(dispatcher) {
            source.requiredPermissionsGranted = false
            val viewModel = viewModel()

            viewModel.onPermissionsRequested()

            assertEquals(HealthConnectUiState.NotConnected(canAsk = false), viewModel.settled())
        }

    @Test
    fun `the dialog stays the way in until the system has shown it once`() = runTest(dispatcher) {
        source.requiredPermissionsGranted = false
        val viewModel = viewModel()

        viewModel.sync()

        assertEquals(HealthConnectUiState.NotConnected(canAsk = true), viewModel.settled())
    }

    @Test
    fun `a screen opened after the dialog was shown does not offer it again`() =
        runTest(dispatcher) {
            source.requiredPermissionsGranted = false
            val first = viewModel()
            first.onPermissionsRequested()
            first.settled()

            val reopened = viewModel()

            assertEquals(
                HealthConnectUiState.NotConnected(canAsk = false),
                reopened.afterFirstRead(),
            )
        }

    @Test
    fun `a sync that finds workouts reports when it happened`() = runTest(dispatcher) {
        source.sessions = listOf(healthConnectActivity("2026-08-12T19:00:00"))
        val viewModel = viewModel()

        viewModel.sync()

        assertEquals(
            HealthConnectUiState.Connected(lastSync = NOW, backgroundSync = true),
            viewModel.settled(),
        )
    }

    @Test
    fun `a Health Connect with nothing in it says so instead of claiming a sync`() =
        runTest(dispatcher) {
            val viewModel = viewModel()

            viewModel.sync()

            assertEquals(HealthConnectUiState.NoWorkouts, viewModel.settled())
        }

    @Test
    fun `connected without background reading says it updates only while open`() =
        runTest(dispatcher) {
            source.sessions = listOf(healthConnectActivity("2026-08-12T19:00:00"))
            source.backgroundPermissionGranted = false
            val viewModel = viewModel()

            viewModel.sync()

            assertEquals(
                HealthConnectUiState.Connected(lastSync = NOW, backgroundSync = false),
                viewModel.settled(),
            )
        }

    @Test
    fun `a failed sync keeps the time of the last successful one`() = runTest(dispatcher) {
        source.sessions = listOf(healthConnectActivity("2026-08-12T19:00:00"))
        val viewModel = viewModel()
        viewModel.sync()
        viewModel.settled()

        source.failure = IOException("Health Connect did not answer")
        viewModel.sync()

        assertEquals(
            HealthConnectUiState.SyncFailed(failedAt = NOW, lastSync = NOW),
            viewModel.settled(),
        )
    }

    @Test
    fun `a sync the user started says what it did`() = runTest(dispatcher) {
        source.sessions = listOf(healthConnectActivity("2026-08-12T19:00:00"))
        val viewModel = viewModel()

        viewModel.sync()
        viewModel.settled()

        assertEquals(SyncOutcome(added = 1, removed = 0), viewModel.syncOutcome.value)

        viewModel.syncOutcomeShown()
        assertNull(viewModel.syncOutcome.value)
    }

    @Test
    fun `the sync that answers the permission dialog says nothing on its own`() =
        runTest(dispatcher) {
            source.sessions = listOf(healthConnectActivity("2026-08-12T19:00:00"))
            val viewModel = viewModel()

            viewModel.onPermissionsRequested()
            viewModel.settled()

            assertNull(viewModel.syncOutcome.value)
        }

    @Test
    fun `asking to sync while a sync runs does not start a second one`() = runTest(dispatcher) {
        source.sessions = listOf(healthConnectActivity("2026-08-12T19:00:00"))
        val viewModel = viewModel()

        viewModel.sync()
        viewModel.sync()

        assertTrue(viewModel.settled() is HealthConnectUiState.Connected)
        assertEquals(1, source.fullReads)
    }

    private fun viewModel() = HealthConnectViewModel(
        source = source,
        syncHealthConnect = syncHealthConnect,
        syncState = syncState,
        clock = clock,
    )

    private fun TestScope.stateOf(viewModel: HealthConnectViewModel): HealthConnectUiState {
        advanceUntilIdle()
        return viewModel.state.value
    }

    /** What the screen shows once a freshly opened view model has read how things stand. */
    private suspend fun HealthConnectViewModel.afterFirstRead(): HealthConnectUiState =
        state.drop(1).first()

    /** The state the screen ends up in once the synchronisation it started has finished. */
    private suspend fun HealthConnectViewModel.settled(): HealthConnectUiState {
        refreshing.first { !it }
        return state.first { it != HealthConnectUiState.Syncing }
    }

    private companion object {
        val ZONE: ZoneId = ZoneId.of("Europe/Warsaw")
        val NOW: Instant = LocalDate.of(2026, 9, 1).atTime(10, 0).atZone(ZONE).toInstant()
        val clock: Clock = Clock.fixed(NOW, ZONE)
    }
}
