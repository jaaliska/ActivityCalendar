package com.jaaliska.activitycalendar.data.healthconnect

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.jaaliska.activitycalendar.data.db.AppDatabase
import com.jaaliska.activitycalendar.data.repository.RoomActivityRepository
import com.jaaliska.activitycalendar.data.settings.HealthConnectSyncState
import com.jaaliska.activitycalendar.domain.ActivityType
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
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

@RunWith(RobolectricTestRunner::class)
class HealthConnectSyncerTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private lateinit var database: AppDatabase
    private lateinit var repository: RoomActivityRepository
    private lateinit var syncState: HealthConnectSyncState

    private val source = FakeHealthConnectSource()

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        repository = RoomActivityRepository(database.activityDao())
        syncState = HealthConnectSyncState(
            PreferenceDataStoreFactory.create(
                scope = CoroutineScope(Job() + Dispatchers.IO),
                produceFile = { temporaryFolder.newFile("sync.preferences_pb") },
            ),
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

        val result = syncer().sync()

        assertEquals(SyncResult.Synced(added = 2), result)
        assertEquals(2, storedIn(YearMonth.of(2026, 3)) + storedIn(YearMonth.of(2026, 8)))
        assertEquals(NOW, syncState.lastSync.first())
    }

    @Test
    fun `later syncs ask only for what changed`() = runTest {
        source.sessions = listOf(healthConnectActivity("2026-08-12T19:00:00"))
        val syncer = syncer()
        syncer.sync()

        source.changed = listOf(healthConnectActivity("2026-08-13T07:15:00", ActivityType.WALKING))
        val result = syncer.sync()

        assertEquals(SyncResult.Synced(added = 1), result)
        assertEquals(1, source.fullReads)
        assertEquals(1, source.changeReads)
        assertEquals(2, storedIn(YearMonth.of(2026, 8)))
    }

    @Test
    fun `a session that comes back a second time does not become a second workout`() = runTest {
        val run = healthConnectActivity("2026-08-12T19:00:00")
        source.sessions = listOf(run)
        val syncer = syncer()
        syncer.sync()

        source.changed = listOf(run)
        val result = syncer.sync()

        assertEquals(SyncResult.Synced(added = 0), result)
        assertEquals(1, storedIn(YearMonth.of(2026, 8)))
    }

    @Test
    fun `a token too old to answer with sends the sync back over the whole history`() = runTest {
        source.sessions = listOf(healthConnectActivity("2026-08-12T19:00:00"))
        val syncer = syncer()
        syncer.sync()

        source.tokenExpired = true
        source.sessions = source.sessions + healthConnectActivity("2026-08-13T07:15:00")
        val result = syncer.sync()

        assertEquals(SyncResult.Synced(added = 1), result)
        assertEquals(2, source.fullReads)
        assertEquals(2, storedIn(YearMonth.of(2026, 8)))
    }

    @Test
    fun `a failed sync leaves the time of the last successful one alone`() = runTest {
        source.sessions = listOf(healthConnectActivity("2026-08-12T19:00:00"))
        val syncer = syncer()
        syncer.sync()

        source.failure = IOException("Health Connect did not answer")
        val result = syncer.sync()

        assertTrue(result is SyncResult.Failed)
        assertEquals(NOW, syncState.lastSync.first())
        assertEquals(1, storedIn(YearMonth.of(2026, 8)))
    }

    @Test
    fun `without a permission the app needs nothing is read and nothing is stored`() = runTest {
        source.sessions = listOf(healthConnectActivity("2026-08-12T19:00:00"))
        source.requiredPermissionsGranted = false

        val result = syncer().sync()

        assertEquals(SyncResult.NotConnected, result)
        assertEquals(0, source.fullReads)
        assertEquals(0, storedIn(YearMonth.of(2026, 8)))
        assertNull(syncState.lastSync.first())
    }

    @Test
    fun `refusing background reading does not stop a sync the app runs itself`() = runTest {
        source.sessions = listOf(healthConnectActivity("2026-08-12T19:00:00"))
        source.backgroundPermissionGranted = false

        val result = syncer().sync()

        assertEquals(SyncResult.Synced(added = 1), result)
        assertEquals(1, storedIn(YearMonth.of(2026, 8)))
    }

    @Test
    fun `a Health Connect without a single session is remembered as empty until one arrives`() =
        runTest {
            val syncer = syncer()
            syncer.sync()
            assertFalse(syncState.seenAnySession.first())

            source.changed = listOf(healthConnectActivity("2026-08-12T19:00:00"))
            syncer.sync()

            assertTrue(syncState.seenAnySession.first())
        }

    private fun syncer() = HealthConnectSyncer(
        source = source,
        repository = repository,
        syncState = syncState,
        clock = Clock.fixed(NOW, ZoneId.of("Europe/Warsaw")),
    )

    private suspend fun storedIn(month: YearMonth): Int = repository.getMonth(month).size

    private companion object {
        val NOW: Instant = LocalDate.of(2026, 9, 1).atTime(10, 0)
            .atZone(ZoneId.of("Europe/Warsaw")).toInstant()
    }
}
