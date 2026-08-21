package com.jaaliska.activitycalendar.data.db

import com.jaaliska.activitycalendar.domain.Activity
import com.jaaliska.activitycalendar.domain.ActivitySourceType
import com.jaaliska.activitycalendar.domain.ActivityType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration
import java.time.LocalDateTime

class ActivityMapperTest {

    @Test
    fun `zero seconds are kept in the stored string`() {
        val start = LocalDateTime.of(2026, 8, 18, 18, 30)

        assertEquals("2026-08-18T18:30:00", start.toDbString())
    }

    @Test
    fun `stored strings of one month are all the same length`() {
        val lengths = listOf(
            LocalDateTime.of(2026, 8, 1, 0, 0, 0),
            LocalDateTime.of(2026, 8, 18, 18, 30),
            LocalDateTime.of(2026, 8, 31, 23, 59, 59),
        ).map { it.toDbString().length }.distinct()

        assertEquals(listOf(19), lengths)
    }

    @Test
    fun `sub-second precision is dropped, not rounded`() {
        val start = LocalDateTime.of(2026, 8, 18, 18, 30, 12, 999_000_000)

        assertEquals("2026-08-18T18:30:12", start.toDbString())
    }

    @Test
    fun `activity survives the round trip`() {
        val activity = Activity(
            id = 7,
            startTimeLocal = LocalDateTime.of(2026, 8, 18, 18, 30),
            type = ActivityType.BADMINTON,
            duration = Duration.ofMinutes(90),
            distanceMeters = null,
            title = "Warsaw Badminton",
            sourceId = "12345678901",
            source = ActivitySourceType.HEALTH_CONNECT,
        )

        assertEquals(activity, activity.toEntity().toDomain())
    }

    @Test
    fun `unknown type in the database does not blow up`() {
        val entity = ActivityEntity(
            id = 1,
            startTimeLocal = "2026-08-18T18:30:00",
            type = "KITESURFING_FROM_A_FUTURE_VERSION",
            durationSeconds = 3600,
            distanceMeters = null,
            title = null,
            sourceId = null,
            source = ActivitySourceType.GARMIN_CSV.name,
        )

        assertEquals(ActivityType.UNKNOWN, entity.toDomain().type)
    }
}
