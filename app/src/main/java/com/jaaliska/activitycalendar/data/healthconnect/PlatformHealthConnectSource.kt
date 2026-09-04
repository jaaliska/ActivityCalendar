package com.jaaliska.activitycalendar.data.healthconnect

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.changes.UpsertionChange
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ChangesTokenRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.jaaliska.activitycalendar.domain.Activity
import com.jaaliska.activitycalendar.domain.ActivitySourceType
import java.time.LocalDateTime
import java.time.ZoneId

/** Reads exercise sessions from the Health Connect installed on the phone. */
class PlatformHealthConnectSource(
    private val context: Context,
    private val deviceZone: () -> ZoneId = ZoneId::systemDefault,
) : HealthConnectSource {

    override val sourceType: ActivitySourceType = ActivitySourceType.HEALTH_CONNECT

    private val requiredPermissions: Set<String> = setOf(
        HealthPermission.getReadPermission(ExerciseSessionRecord::class),
        HealthPermission.getReadPermission(DistanceRecord::class),
        HealthPermission.PERMISSION_READ_HEALTH_DATA_HISTORY,
    )

    override val permissions: Set<String> =
        requiredPermissions + HealthPermission.PERMISSION_READ_HEALTH_DATA_IN_BACKGROUND

    private val client: HealthConnectClient by lazy { HealthConnectClient.getOrCreate(context) }

    override fun availability(): HealthConnectAvailability =
        when (HealthConnectClient.getSdkStatus(context)) {
            HealthConnectClient.SDK_AVAILABLE -> HealthConnectAvailability.AVAILABLE
            HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED ->
                HealthConnectAvailability.NOT_INSTALLED

            else -> HealthConnectAvailability.UNSUPPORTED
        }

    override suspend fun hasRequiredPermissions(): Boolean =
        granted().containsAll(requiredPermissions)

    override suspend fun canReadInBackground(): Boolean =
        HealthPermission.PERMISSION_READ_HEALTH_DATA_IN_BACKGROUND in granted()

    private suspend fun granted(): Set<String> =
        if (availability() != HealthConnectAvailability.AVAILABLE) {
            emptySet()
        } else {
            client.permissionController.getGrantedPermissions()
        }

    override suspend fun getActivities(from: LocalDateTime, to: LocalDateTime): List<Activity> =
        readSessions(TimeRangeFilter.between(from, to)).map { it.toDomain() }

    override suspend fun changesToken(): String = client.getChangesToken(
        ChangesTokenRequest(recordTypes = setOf(ExerciseSessionRecord::class)),
    )

    override suspend fun changesSince(token: String): HealthConnectChanges {
        val sessions = mutableListOf<ExerciseSessionRecord>()
        var next = token
        while (true) {
            val response = client.getChanges(next)
            if (response.changesTokenExpired) {
                return HealthConnectChanges(
                    activities = emptyList(),
                    nextToken = response.nextChangesToken,
                    expired = true,
                )
            }
            response.changes
                .filterIsInstance<UpsertionChange>()
                .mapNotNullTo(sessions) { it.record as? ExerciseSessionRecord }
            next = response.nextChangesToken
            if (!response.hasMore) break
        }
        return HealthConnectChanges(
            activities = sessions.map { it.toDomain() },
            nextToken = next,
            expired = false,
        )
    }

    private suspend fun readSessions(range: TimeRangeFilter): List<ExerciseSessionRecord> {
        val sessions = mutableListOf<ExerciseSessionRecord>()
        var pageToken: String? = null
        do {
            val response = client.readRecords(
                ReadRecordsRequest(
                    recordType = ExerciseSessionRecord::class,
                    timeRangeFilter = range,
                    pageToken = pageToken,
                ),
            )
            sessions += response.records
            pageToken = response.pageToken
        } while (pageToken != null)
        return sessions
    }

    private suspend fun ExerciseSessionRecord.toDomain(): Activity =
        toActivity(distanceMeters = readDistanceMeters(), deviceZone = deviceZone())

    /** The metres of this session alone: another app's distances fall outside the origin filter. */
    private suspend fun ExerciseSessionRecord.readDistanceMeters(): Double? {
        if (!startTime.isBefore(endTime)) return null
        return client.aggregate(
            AggregateRequest(
                metrics = setOf(DistanceRecord.DISTANCE_TOTAL),
                timeRangeFilter = TimeRangeFilter.between(startTime, endTime),
                dataOriginFilter = setOf(metadata.dataOrigin),
            ),
        )[DistanceRecord.DISTANCE_TOTAL]?.inMeters
    }
}
