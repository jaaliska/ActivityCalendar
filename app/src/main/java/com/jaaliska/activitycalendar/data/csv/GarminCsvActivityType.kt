package com.jaaliska.activitycalendar.data.csv

import com.jaaliska.activitycalendar.domain.ActivityType

/**
 * Maps the `Activity Type` column of a Garmin export onto a domain type.
 *
 * @param raw the column value, `null` when the row has no such column
 * @return the domain type, [ActivityType.UNKNOWN] for anything not in English or not supported
 */
internal fun garminCsvActivityTypeOf(raw: String?): ActivityType = when (raw?.trim()?.lowercase()) {
    "badminton" -> ActivityType.BADMINTON
    "cycling" -> ActivityType.CYCLING
    "running" -> ActivityType.RUNNING
    "strength training" -> ActivityType.STRENGTH_TRAINING
    "walking" -> ActivityType.WALKING
    "yoga" -> ActivityType.YOGA
    else -> ActivityType.UNKNOWN
}
