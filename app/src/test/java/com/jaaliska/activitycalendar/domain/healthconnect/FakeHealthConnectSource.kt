package com.jaaliska.activitycalendar.domain.healthconnect

import com.jaaliska.activitycalendar.domain.Activity
import com.jaaliska.activitycalendar.domain.ActivitySourceType
import com.jaaliska.activitycalendar.domain.ActivityType
import java.time.Duration
import java.time.LocalDateTime

/** A Health Connect that holds whatever a test puts in it. */
class FakeHealthConnectSource(
    var sessions: List<Activity> = emptyList(),
    var requiredPermissionsGranted: Boolean = true,
    var backgroundPermissionGranted: Boolean = true,
    var availability: HealthConnectAvailability = HealthConnectAvailability.AVAILABLE,
) : HealthConnectSource {

    override val sourceType: ActivitySourceType = ActivitySourceType.HEALTH_CONNECT

    override val permissions: Set<String> = setOf("read-exercise", "read-in-background")

    /** What the next call for changes answers with. */
    var changed: List<Activity> = emptyList()

    /** Whether the next call for changes reports the token as too old. */
    var tokenExpired: Boolean = false

    /** What every read throws, if the test wants Health Connect to fail. */
    var failure: Throwable? = null

    var fullReads: Int = 0
        private set

    var changeReads: Int = 0
        private set

    private var tokensTaken = 0

    override fun availability(): HealthConnectAvailability = availability

    override suspend fun hasRequiredPermissions(): Boolean =
        requiredPermissionsGranted && availability == HealthConnectAvailability.AVAILABLE

    override suspend fun canReadInBackground(): Boolean =
        backgroundPermissionGranted && availability == HealthConnectAvailability.AVAILABLE

    override suspend fun getActivities(from: LocalDateTime, to: LocalDateTime): List<Activity> {
        failure?.let { throw it }
        fullReads++
        return sessions.filter { it.startTimeLocal >= from && it.startTimeLocal < to }
    }

    override suspend fun changesToken(): String {
        failure?.let { throw it }
        tokensTaken++
        return "token-$tokensTaken"
    }

    override suspend fun changesSince(token: String): HealthConnectChanges {
        failure?.let { throw it }
        changeReads++
        return HealthConnectChanges(
            activities = if (tokenExpired) emptyList() else changed,
            nextToken = "$token-next",
            expired = tokenExpired,
        )
    }
}

/** A session as Health Connect would hand it over, already in the terms of this app. */
fun healthConnectActivity(
    startTimeLocal: String,
    type: ActivityType = ActivityType.RUNNING,
    distanceMeters: Double? = null,
) = Activity(
    startTimeLocal = LocalDateTime.parse(startTimeLocal),
    type = type,
    duration = Duration.ofMinutes(42),
    distanceMeters = distanceMeters,
    title = null,
    sourceId = null,
    source = ActivitySourceType.HEALTH_CONNECT,
)
