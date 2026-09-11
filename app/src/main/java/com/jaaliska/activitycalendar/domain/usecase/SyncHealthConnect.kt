package com.jaaliska.activitycalendar.domain.usecase

import com.jaaliska.activitycalendar.domain.ActivityRepository
import com.jaaliska.activitycalendar.domain.ActivitySourceType
import com.jaaliska.activitycalendar.domain.healthconnect.HealthConnectSource
import com.jaaliska.activitycalendar.domain.healthconnect.HealthConnectSyncState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime

/** How much of Health Connect one run covers. */
enum class SyncScope {

    /** What changed since the last run, and the last 30 days for what has gone. */
    CHANGES,

    /** The whole history, both for what is new and for what has gone. */
    WHOLE_HISTORY,
}

/** How a synchronisation ended. */
sealed interface SyncResult {

    /**
     * @property added how many activities were not stored before
     * @property removed how many stored activities Health Connect no longer has
     */
    data class Synced(val added: Int, val removed: Int = 0) : SyncResult

    /** Health Connect is unavailable or a permission the app cannot work without is missing. */
    data object NotConnected : SyncResult

    /** @property cause what Health Connect or the database threw */
    data class Failed(val cause: Throwable) : SyncResult
}

/** Makes the repository show what Health Connect holds. */
class SyncHealthConnect(
    private val source: HealthConnectSource,
    private val repository: ActivityRepository,
    private val syncState: HealthConnectSyncState,
    private val clock: Clock = Clock.systemDefaultZone(),
) {

    private val mutex = Mutex()

    /**
     * Stores the sessions Health Connect has and drops the ones it no longer has. Within
     * [SyncScope.CHANGES] the first synchronisation reads the whole history, later ones only
     * what changed since. A call made while another synchronisation runs waits for it instead
     * of reading in parallel.
     */
    suspend operator fun invoke(scope: SyncScope = SyncScope.CHANGES): SyncResult = mutex.withLock {
        if (!source.hasRequiredPermissions()) return@withLock SyncResult.NotConnected
        runCatching {
            if (scope == SyncScope.WHOLE_HISTORY) rebuild() else syncChanges()
        }.fold(
            onSuccess = { it },
            onFailure = { SyncResult.Failed(it) },
        )
    }

    private suspend fun syncChanges(): SyncResult.Synced {
        val token = syncState.changesToken.first()
        val added = if (token == null) fullSync() else incrementalSync(token)
        val to = LocalDateTime.now(clock)
        return SyncResult.Synced(added, mirror(to.minusDays(MIRROR_WINDOW_DAYS), to))
    }

    private suspend fun rebuild(): SyncResult.Synced {
        val to = LocalDateTime.now(clock)
        val added = fullSync()
        return SyncResult.Synced(added, mirror(HISTORY_START, to))
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

    // Nothing at all in the window means a broken connection far more often than a quiet month,
    // so an empty read removes nothing.
    private suspend fun mirror(from: LocalDateTime, to: LocalDateTime): Int {
        val sessions = source.getActivities(from, to)
        if (sessions.isEmpty()) return 0
        return repository.deleteMissing(ActivitySourceType.HEALTH_CONNECT, from, to, sessions)
    }

    private companion object {
        /** Everything Health Connect has: no watch wrote a session before this. */
        val HISTORY_START: LocalDateTime = LocalDateTime.of(1970, 1, 1, 0, 0)

        const val MIRROR_WINDOW_DAYS = 30L
    }
}
