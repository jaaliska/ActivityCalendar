package com.jaaliska.activitycalendar.data.db

import com.jaaliska.activitycalendar.domain.Activity
import com.jaaliska.activitycalendar.domain.ActivitySourceType
import com.jaaliska.activitycalendar.domain.ActivityType
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val START_TIME_FORMAT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")

/** Formats the time for storage, dropping anything below a second. */
fun LocalDateTime.toDbString(): String = format(START_TIME_FORMAT)

fun String.toLocalDateTimeOrThrow(): LocalDateTime = LocalDateTime.parse(this, START_TIME_FORMAT)

fun Activity.toEntity(): ActivityEntity = ActivityEntity(
    id = id,
    startTimeLocal = startTimeLocal.toDbString(),
    type = type.name,
    durationSeconds = duration.seconds,
    distanceMeters = distanceMeters,
    title = title,
    sourceId = sourceId,
    source = source.name,
)

/** Maps the stored form back, turning a type this version does not know into [ActivityType.UNKNOWN]. */
fun ActivityEntity.toDomain(): Activity = Activity(
    id = id,
    startTimeLocal = startTimeLocal.toLocalDateTimeOrThrow(),
    type = ActivityType.entries.firstOrNull { it.name == type } ?: ActivityType.UNKNOWN,
    duration = Duration.ofSeconds(durationSeconds),
    distanceMeters = distanceMeters,
    title = title,
    sourceId = sourceId,
    source = ActivitySourceType.valueOf(source),
)
