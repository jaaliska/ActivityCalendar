package com.jaaliska.activitycalendar.data.healthconnect

import com.jaaliska.activitycalendar.data.settings.HealthConnectSyncState
import com.jaaliska.activitycalendar.domain.ActivityRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime

/** How a synchronisation ended. */
sealed interface SyncResult {

    /** @property added how many activities were not stored before */
    data class Synced(val added: Int) : SyncResult

    /** Health Connect is unavailable or a permission the app cannot work without is missing. */
    data object NotConnected : SyncResult

    /** @property cause what Health Connect or the database threw */
    data class Failed(val cause: Throwable) : SyncResult
}

/** Puts what Health Connect holds into the repository. */
class HealthConnectSyncer(
    private val source: HealthConnectSource,
    private val repository: ActivityRepository,
    private val syncState: HealthConnectSyncState,
    private val clock: Clock = Clock.systemDefaultZone(),
) {

    private val mutex = Mutex()

    /**
     * Stores the sessions Health Connect has, keeping the activities already stored as they are.
     * The first synchronisation reads the whole history, later ones only what changed since.
     * A call made while another synchronisation runs waits for it instead of reading in parallel.
     */
    suspend fun sync(): SyncResult = mutex.withLock {
        if (!source.hasRequiredPermissions()) return@withLock SyncResult.NotConnected
        runCatching { readAndStore() }.fold(
            onSuccess = { SyncResult.Synced(it) },
            onFailure = { SyncResult.Failed(it) },
        )
    }

    private suspend fun readAndStore(): Int {
        val token = syncState.changesToken.first()
        return if (token == null) fullSync() else incrementalSync(token)
    }

    private suspend fun fullSync(): Int {
        // Taken before the read: a session that arrives while it runs comes back as a change.
        val token = source.changesToken()
        val activities = source.getActivities(HISTORY_START, LocalDateTime.now(clock))
        val added = repository.save(activities)
        syncState.record(token, Instant.now(clock), sawSessions = activities.isNotEmpty())
        return added
    }

    private suspend fun incrementalSync(token: String): Int {
        val changes = source.changesSince(token)
        if (changes.expired) return fullSync()
        val added = repository.save(changes.activities)
        syncState.record(
            changesToken = changes.nextToken,
            syncedAt = Instant.now(clock),
            sawSessions = changes.activities.isNotEmpty(),
        )
        return added
    }

    private companion object {
        /** Everything Health Connect has: no watch wrote a session before this. */
        val HISTORY_START: LocalDateTime = LocalDateTime.of(1970, 1, 1, 0, 0)
    }
}
