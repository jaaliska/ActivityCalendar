package com.jaaliska.activitycalendar.domain

import java.time.LocalDateTime

/** Sources ordered by how much their title is trusted, the most trusted one first. */
private val TITLE_SOURCES = listOf(
    ActivitySourceType.MANUAL,
    ActivitySourceType.GARMIN_CSV,
    ActivitySourceType.HEALTH_CONNECT,
)

/** Sources ordered by how much their duration and distance are trusted, the most trusted one first. */
private val MEASUREMENT_SOURCES = listOf(
    ActivitySourceType.MANUAL,
    ActivitySourceType.HEALTH_CONNECT,
    ActivitySourceType.GARMIN_CSV,
)

/**
 * Combines two records of the same activity into one, taking every field from the source
 * that is trusted with it. A missing value always loses to a present one, however the
 * sources rank. Identity of the record — its id, source and source id — stays as it is here.
 *
 * @param other the same activity as told by another source
 * @throws IllegalArgumentException if [other] is a different activity
 */
fun Activity.mergeWith(other: Activity): Activity {
    require(startTimeLocal == other.startTimeLocal && type == other.type) {
        "activities of different start time or type cannot be merged"
    }
    return copy(
        duration = choose(other, MEASUREMENT_SOURCES) { it.duration.takeIf { d -> !d.isZero } }
            ?: duration,
        distanceMeters = choose(other, MEASUREMENT_SOURCES) { it.distanceMeters },
        title = choose(other, TITLE_SOURCES) { it.title?.takeIf(String::isNotBlank) },
    )
}

/**
 * Folds the records of one and the same activity into one, keeping the order of first
 * appearance. What wins in a fold is what [mergeWith] says.
 */
fun List<Activity>.mergeDuplicates(): List<Activity> {
    val byKey = LinkedHashMap<Pair<LocalDateTime, ActivityType>, Activity>(size)
    forEach { activity ->
        val key = activity.startTimeLocal to activity.type
        val kept = byKey[key]
        byKey[key] = kept?.mergeWith(activity) ?: activity
    }
    return byKey.values.toList()
}

/** Reads one field off the record whose source is trusted with it; `null` if neither has it. */
private fun <T> Activity.choose(
    other: Activity,
    priority: List<ActivitySourceType>,
    value: (Activity) -> T?,
): T? {
    val own = value(this)
    val theirs = value(other)
    if (own == null) return theirs
    if (theirs == null) return own
    return if (priority.rankOf(source) <= priority.rankOf(other.source)) own else theirs
}

/** An unlisted source is trusted least: a source nobody ranked cannot outrank a ranked one. */
private fun List<ActivitySourceType>.rankOf(source: ActivitySourceType): Int =
    indexOf(source).takeIf { it >= 0 } ?: Int.MAX_VALUE
