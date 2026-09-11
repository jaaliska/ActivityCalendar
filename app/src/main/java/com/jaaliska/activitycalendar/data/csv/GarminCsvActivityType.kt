package com.jaaliska.activitycalendar.data.csv

import com.jaaliska.activitycalendar.domain.ActivityType

/** What the `Activity Type` column of a Garmin export can say, by domain type. */
private val NAMES: Map<ActivityType, List<String>> = mapOf(
    ActivityType.BADMINTON to listOf("badminton"),
    ActivityType.BASKETBALL to listOf("basketball"),
    ActivityType.BOXING to listOf("boxing"),
    ActivityType.CYCLING to listOf(
        "cycling", "indoor cycling", "road cycling", "gravel/unpaved cycling",
        "mountain biking", "virtual cycling", "ebike ride",
    ),
    ActivityType.DANCING to listOf("dancing", "dance"),
    ActivityType.HIKING to listOf("hiking"),
    ActivityType.MARTIAL_ARTS to listOf("martial arts", "mixed martial arts"),
    ActivityType.RUNNING to listOf(
        "running", "treadmill running", "trail running", "indoor running", "virtual run",
    ),
    ActivityType.SKIING to listOf(
        "skiing", "resort skiing", "resort skiing/snowboarding", "backcountry skiing",
        "cross country classic ski", "cross country skate ski", "skate skiing",
    ),
    ActivityType.SNOWBOARDING to listOf("snowboarding", "resort snowboarding"),
    ActivityType.SOCCER to listOf("soccer", "football", "indoor soccer"),
    ActivityType.STRENGTH_TRAINING to listOf("strength training", "weight training"),
    ActivityType.STRETCHING to listOf("stretching", "pilates"),
    ActivityType.SWIMMING to listOf(
        "swimming", "pool swim", "pool swimming", "lap swimming",
        "open water swim", "open water swimming",
    ),
    ActivityType.TABLE_TENNIS to listOf("table tennis", "ping pong"),
    ActivityType.TENNIS to listOf("tennis"),
    ActivityType.VOLLEYBALL to listOf("volleyball", "indoor volleyball", "beach volleyball"),
    ActivityType.WALKING to listOf("walking", "indoor walking", "casual walking"),
    ActivityType.YOGA to listOf("yoga"),
)

private val TYPES_BY_NAME: Map<String, ActivityType> =
    NAMES.flatMap { (type, names) -> names.map { it to type } }.toMap()

/**
 * Maps the `Activity Type` column of a Garmin export onto a domain type.
 *
 * @param raw the column value, `null` when the row has no such column
 * @return the domain type, [ActivityType.UNKNOWN] for anything not in English or not supported
 */
internal fun garminCsvActivityTypeOf(raw: String?): ActivityType =
    TYPES_BY_NAME[raw?.trim()?.lowercase()] ?: ActivityType.UNKNOWN

/**
 * Names a domain type the way a Garmin export does.
 *
 * @return the `Activity Type` value; [ActivityType.UNKNOWN] becomes `Other`, which reads back
 * as unknown again
 */
internal fun garminCsvNameOf(type: ActivityType): String = NAMES[type]
    ?.first()
    ?.split(" ")
    ?.joinToString(" ") { word -> word.replaceFirstChar(Char::uppercase) }
    ?: "Other"
