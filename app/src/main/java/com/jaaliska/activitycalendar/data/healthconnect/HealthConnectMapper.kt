package com.jaaliska.activitycalendar.data.healthconnect

import androidx.health.connect.client.records.ExerciseSessionRecord
import com.jaaliska.activitycalendar.domain.Activity
import com.jaaliska.activitycalendar.domain.ActivitySourceType
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * Maps a Health Connect session onto the domain model.
 *
 * @param distanceMeters the metres this session covered, if any were recorded
 * @param deviceZone the zone to date the session in when it carries no offset of its own
 */
internal fun ExerciseSessionRecord.toActivity(
    distanceMeters: Double?,
    deviceZone: ZoneId,
): Activity = Activity(
    startTimeLocal = LocalDateTime
        .ofInstant(startTime, startZoneOffset ?: deviceZone)
        .truncatedTo(ChronoUnit.SECONDS),
    type = healthConnectActivityTypeOf(exerciseType),
    duration = Duration.ofSeconds(Duration.between(startTime, endTime).seconds),
    // Zero metres means the workout has no distance, the same as none being recorded at all.
    distanceMeters = distanceMeters?.takeIf { it > 0 },
    title = title,
    sourceId = metadata.clientRecordId ?: metadata.id.takeIf { it.isNotEmpty() },
    source = ActivitySourceType.HEALTH_CONNECT,
)
