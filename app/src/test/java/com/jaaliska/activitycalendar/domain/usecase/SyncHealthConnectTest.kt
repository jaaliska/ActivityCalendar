package com.jaaliska.activitycalendar.domain.usecase

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.jaaliska.activitycalendar.data.db.AppDatabase
import com.jaaliska.activitycalendar.data.repository.RoomActivityRepository
import com.jaaliska.activitycalendar.data.settings.DataStoreHealthConnectSyncState
import com.jaaliska.activitycalendar.domain.Activity
import com.jaaliska.activitycalendar.domain.ActivitySourceType
import com.jaaliska.activitycalendar.domain.ActivityType
import com.jaaliska.activitycalendar.domain.healthconnect.FakeHealthConnectSource
import com.jaaliska.activitycalendar.domain.healthconnect.HealthConnectSyncState
import com.jaaliska.activitycalendar.domain.healthconnect.healthConnectActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId

@RunWith(RobolectricTestRunner::class)
class SyncHealthConnectTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private lateinit var database: AppDatabase
    private lateinit var repository: RoomActivityRepository
    private lateinit var syncState: HealthConnectSyncState
    private lateinit var syncHealthConnect: SyncHealthConnect

    private val source = FakeHealthConnectSource()

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        repository = RoomActivityRepository(database)
        syncState = DataStoreHealthConnectSyncState(
            PreferenceDataStoreFactory.create(
                scope = CoroutineScope(Job() + Dispatchers.IO),
                produceFile = { temporaryFolder.newFile("sync.preferences_pb") },
            ),
        )
        syncHealthConnect = SyncHealthConnect(
            source = source,
            repository = repository,
            syncState = syncState,
            clock = Clock.fixed(NOW, ZoneId.of("Europe/Warsaw")),
        )
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun `the first sync stores everything Health Connect has`() = runTest {
        source.sessions = listOf(
            healthConnectActivity("2026-03-11T07:30:00"),
            healthConnectActivity("2026-08-12T19:00:00", ActivityType.YOGA),
        )

        val result = syncHealthConnect()

        assertEquals(SyncResult.Synced(added = 2), result)
        assertEquals(2, storedIn(YearMonth.of(2026, 3)) + storedIn(YearMonth.of(2026, 8)))
        assertEquals(NOW, syncState.lastSync.first())
    }

    @Test
    fun `later syncs ask only for what changed`() = runTest {
        source.sessions = listOf(healthConnectActivity("2026-08-12T19:00:00"))
        syncHealthConnect()

        val walk = healthConnectActivity("2026-08-13T07:15:00", ActivityType.WALKING)
        source.changed = listOf(walk)
        source.sessions = source.sessions + walk
        val result = syncHealthConnect()

        assertEquals(SyncResult.Synced(added = 1), result)
        assertEquals(1, source.fullReads)
        assertEquals(1, source.changeReads)
        assertEquals(2, storedIn(YearMonth.of(2026, 8)))
    }

    @Test
    fun `a session that comes back a second time does not become a second workout`() = runTest {
        val run = healthConnectActivity("2026-08-12T19:00:00")
        source.sessions = listOf(run)
        syncHealthConnect()

        source.changed = listOf(run)
        val result = syncHealthConnect()

        assertEquals(SyncResult.Synced(added = 0), result)
        assertEquals(1, storedIn(YearMonth.of(2026, 8)))
    }

    @Test
    fun `a token too old to answer with sends the sync back over the whole history`() = runTest {
        source.sessions = listOf(healthConnectActivity("2026-08-12T19:00:00"))
        syncHealthConnect()

        source.tokenExpired = true
        source.sessions = source.sessions + healthConnectActivity("2026-08-13T07:15:00")
        val result = syncHealthConnect()

        assertEquals(SyncResult.Synced(added = 1), result)
        assertEquals(2, source.fullReads)
        assertEquals(2, storedIn(YearMonth.of(2026, 8)))
    }

    @Test
    fun `a failed sync leaves the time of the last successful one alone`() = runTest {
        source.sessions = listOf(healthConnectActivity("2026-08-12T19:00:00"))
        syncHealthConnect()

        source.failure = IOException("Health Connect did not answer")
        val result = syncHealthConnect()

        assertTrue(result is SyncResult.Failed)
        assertEquals(NOW, syncState.lastSync.first())
        assertEquals(1, storedIn(YearMonth.of(2026, 8)))
    }

    @Test
    fun `without a permission the app needs nothing is read and nothing is stored`() = runTest {
        source.sessions = listOf(healthConnectActivity("2026-08-12T19:00:00"))
        source.requiredPermissionsGranted = false

        val result = syncHealthConnect()

        assertEquals(SyncResult.NotConnected, result)
        assertEquals(0, source.fullReads)
        assertEquals(0, storedIn(YearMonth.of(2026, 8)))
        assertNull(syncState.lastSync.first())
    }

    @Test
    fun `refusing background reading does not stop a sync the app runs itself`() = runTest {
        source.sessions = listOf(healthConnectActivity("2026-08-12T19:00:00"))
        source.backgroundPermissionGranted = false

        val result = syncHealthConnect()

        assertEquals(SyncResult.Synced(added = 1), result)
        assertEquals(1, storedIn(YearMonth.of(2026, 8)))
    }

    @Test
    fun `a Health Connect without a single session is remembered as empty until one arrives`() =
        runTest {
                syncHealthConnect()
            assertFalse(syncState.seenAnySession.first())

            source.changed = listOf(healthConnectActivity("2026-08-12T19:00:00"))
            syncHealthConnect()

            assertTrue(syncState.seenAnySession.first())
        }

    @Test
    fun `a workout deleted in Health Connect leaves the calendar`() = runTest {
        source.sessions = listOf(
            healthConnectActivity("2026-08-20T07:30:00"),
            healthConnectActivity("2026-08-25T19:00:00", ActivityType.YOGA),
        )
        syncHealthConnect()

        source.sessions = listOf(healthConnectActivity("2026-08-20T07:30:00"))
        val result = syncHealthConnect()

        assertEquals(SyncResult.Synced(added = 0, removed = 1), result)
        assertEquals(1, storedIn(YearMonth.of(2026, 8)))
    }

    @Test
    fun `the mirror leaves the imported history alone`() = runTest {
        repository.save(listOf(importedActivity("2026-08-21T18:00:00")))
        source.sessions = listOf(healthConnectActivity("2026-08-20T07:30:00"))

        syncHealthConnect()

        assertEquals(2, storedIn(YearMonth.of(2026, 8)))
    }

    @Test
    fun `a Health Connect answering with nothing removes nothing`() = runTest {
        source.sessions = listOf(healthConnectActivity("2026-08-20T07:30:00"))
        syncHealthConnect()

        source.sessions = emptyList()
        val result = syncHealthConnect()

        assertEquals(SyncResult.Synced(added = 0, removed = 0), result)
        assertEquals(1, storedIn(YearMonth.of(2026, 8)))
    }

    @Test
    fun `a sync mirrors the last 30 days and nothing older`() = runTest {
        source.sessions = listOf(
            healthConnectActivity("2026-06-01T07:30:00"),
            healthConnectActivity("2026-08-20T07:30:00"),
        )
        syncHealthConnect()

        source.sessions = listOf(healthConnectActivity("2026-08-20T07:30:00"))
        val result = syncHealthConnect()

        assertEquals(SyncResult.Synced(added = 0, removed = 0), result)
        assertEquals(1, storedIn(YearMonth.of(2026, 6)))
    }

    @Test
    fun `a rebuild mirrors the whole history`() = runTest {
        source.sessions = listOf(
            healthConnectActivity("2026-06-01T07:30:00"),
            healthConnectActivity("2026-08-20T07:30:00"),
        )
        syncHealthConnect()

        source.sessions = listOf(healthConnectActivity("2026-08-20T07:30:00"))
        val result = syncHealthConnect(SyncScope.WHOLE_HISTORY)

        assertEquals(SyncResult.Synced(added = 0, removed = 1), result)
        assertEquals(0, storedIn(YearMonth.of(2026, 6)))
        assertEquals(1, storedIn(YearMonth.of(2026, 8)))
    }

    private suspend fun storedIn(month: YearMonth): Int = repository.getMonth(month).size

    private fun importedActivity(startTimeLocal: String) = Activity(
        startTimeLocal = LocalDateTime.parse(startTimeLocal),
        type = ActivityType.RUNNING,
        duration = Duration.ofMinutes(40),
        distanceMeters = 6500.0,
        title = "Morning Run",
        sourceId = null,
        source = ActivitySourceType.GARMIN_CSV,
    )

    private companion object {
        val NOW: Instant = LocalDate.of(2026, 9, 1).atTime(10, 0)
            .atZone(ZoneId.of("Europe/Warsaw")).toInstant()
    }
}
