package com.jaaliska.activitycalendar.data.healthconnect

import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.metadata.Metadata
import com.jaaliska.activitycalendar.domain.ActivitySourceType
import com.jaaliska.activitycalendar.domain.ActivityType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

class HealthConnectMapperTest {

    @Test
    fun `a session keeps the local time of the place it was recorded in`() {
        // Recorded at 09:00 in +03:00 while the phone stands in +02:00.
        val session = session(
            startTime = Instant.parse("2026-08-12T06:00:00Z"),
            startZoneOffset = ZoneOffset.ofHours(3),
            endTime = Instant.parse("2026-08-12T07:00:00Z"),
        )

        val activity = session.toActivity(distanceMeters = null, deviceZone = WARSAW)

        assertEquals(LocalDateTime.parse("2026-08-12T09:00:00"), activity.startTimeLocal)
    }

    @Test
    fun `a session without an offset is dated in the zone of the phone`() {
        val session = session(
            startTime = Instant.parse("2026-08-12T06:00:00Z"),
            startZoneOffset = null,
            endTime = Instant.parse("2026-08-12T07:00:00Z"),
        )

        val activity = session.toActivity(distanceMeters = null, deviceZone = WARSAW)

        assertEquals(LocalDateTime.parse("2026-08-12T08:00:00"), activity.startTimeLocal)
    }

    @Test
    fun `the duration is the length of the session, down to the second`() {
        val session = session(
            startTime = Instant.parse("2026-08-12T06:00:00.400Z"),
            endTime = Instant.parse("2026-08-12T06:42:30.900Z"),
        )

        val activity = session.toActivity(distanceMeters = null, deviceZone = WARSAW)

        assertEquals(Duration.ofSeconds(2550), activity.duration)
        assertEquals(LocalDateTime.parse("2026-08-12T08:00:00"), activity.startTimeLocal)
    }

    @Test
    fun `zero metres means the workout has no distance`() {
        val activity = session().toActivity(distanceMeters = 0.0, deviceZone = WARSAW)

        assertNull(activity.distanceMeters)
    }

    @Test
    fun `every supported exercise type keeps its own kind`() {
        val mapped = listOf(
            ExerciseSessionRecord.EXERCISE_TYPE_BADMINTON to ActivityType.BADMINTON,
            ExerciseSessionRecord.EXERCISE_TYPE_BIKING to ActivityType.CYCLING,
            ExerciseSessionRecord.EXERCISE_TYPE_RUNNING to ActivityType.RUNNING,
            ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING to
                ActivityType.STRENGTH_TRAINING,
            ExerciseSessionRecord.EXERCISE_TYPE_WALKING to ActivityType.WALKING,
            ExerciseSessionRecord.EXERCISE_TYPE_YOGA to ActivityType.YOGA,
        ).map { (exerciseType, expected) ->
            expected to session(exerciseType = exerciseType)
                .toActivity(distanceMeters = null, deviceZone = WARSAW).type
        }

        assertEquals(mapped.map { it.first }, mapped.map { it.second })
    }

    @Test
    fun `an unsupported exercise type is stored as unknown rather than dropped`() {
        val activity = session(exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_SURFING)
            .toActivity(distanceMeters = 1200.0, deviceZone = WARSAW)

        assertEquals(ActivityType.UNKNOWN, activity.type)
        assertEquals(1200.0, activity.distanceMeters!!, 0.001)
        assertEquals(ActivitySourceType.HEALTH_CONNECT, activity.source)
    }

    @Test
    fun `the id of the recording app is kept for checking against it later`() {
        val session = session(metadata = Metadata.manualEntry(clientRecordId = "garmin-42"))

        val activity = session.toActivity(distanceMeters = null, deviceZone = WARSAW)

        assertEquals("garmin-42", activity.sourceId)
    }

    private fun session(
        startTime: Instant = Instant.parse("2026-08-12T06:00:00Z"),
        startZoneOffset: ZoneOffset? = null,
        endTime: Instant = Instant.parse("2026-08-12T07:00:00Z"),
        exerciseType: Int = ExerciseSessionRecord.EXERCISE_TYPE_RUNNING,
        metadata: Metadata = Metadata.manualEntry(),
    ) = ExerciseSessionRecord(
        startTime = startTime,
        startZoneOffset = startZoneOffset,
        endTime = endTime,
        endZoneOffset = startZoneOffset,
        metadata = metadata,
        exerciseType = exerciseType,
    )

    private companion object {
        val WARSAW: ZoneId = ZoneId.of("Europe/Warsaw")
    }
}
