package com.jaaliska.activitycalendar.data.source

import com.jaaliska.activitycalendar.domain.Activity
import com.jaaliska.activitycalendar.domain.ActivitySource
import com.jaaliska.activitycalendar.domain.ActivitySourceType
import com.jaaliska.activitycalendar.domain.ActivityType
import java.time.Duration
import java.time.LocalDateTime

/**
 * Hand-written sample activities.
 *
 * The odd times right at the edges of a month are deliberate: `ActivityDaoTest` checks
 * the month boundaries on them.
 */
class FixtureActivitySource : ActivitySource {

    override val sourceType: ActivitySourceType = ActivitySourceType.MANUAL

    override suspend fun getActivities(from: LocalDateTime, to: LocalDateTime): List<Activity> =
        ALL.filter { it.startTimeLocal >= from && it.startTimeLocal < to }

    companion object {

        val ALL: List<Activity> = listOf(
            activity("2026-07-15T07:30:00", ActivityType.RUNNING, 42, 7200.0),
            activity("2026-07-31T23:59:59", ActivityType.WALKING, 20, 1800.0),

            activity("2026-08-01T00:00:00", ActivityType.CYCLING, 65, 22000.0),
            activity("2026-08-03T18:30:00", ActivityType.BADMINTON, 60, null),
            activity("2026-08-05T07:15:00", ActivityType.RUNNING, 38, 6500.0),
            activity("2026-08-08T10:00:00", ActivityType.WALKING, 72, 5400.0),
            activity("2026-08-11T19:00:00", ActivityType.STRENGTH_TRAINING, 45, null),
            activity("2026-08-14T07:20:00", ActivityType.RUNNING, 51, 9000.0),
            activity("2026-08-18T18:30:00", ActivityType.BADMINTON, 90, null),
            activity("2026-08-22T09:00:00", ActivityType.YOGA, 40, null),
            activity("2026-08-27T17:45:00", ActivityType.CYCLING, 108, 38500.0),
            activity("2026-08-31T23:59:59", ActivityType.YOGA, 25, null),

            activity("2026-09-01T00:00:00", ActivityType.RUNNING, 30, 5200.0),
            activity("2026-09-04T08:00:00", ActivityType.WALKING, 55, 4300.0),
        )

        private fun activity(
            startTimeLocal: String,
            type: ActivityType,
            minutes: Long,
            distanceMeters: Double?,
        ) = Activity(
            startTimeLocal = LocalDateTime.parse(startTimeLocal),
            type = type,
            duration = Duration.ofMinutes(minutes),
            distanceMeters = distanceMeters,
            title = null,
            sourceId = null,
            source = ActivitySourceType.MANUAL,
        )
    }
}
