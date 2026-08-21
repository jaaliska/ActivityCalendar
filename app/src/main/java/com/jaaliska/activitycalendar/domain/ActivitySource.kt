package com.jaaliska.activitycalendar.domain

import java.time.LocalDateTime

/** A source that can be asked for activities over a time range. */
interface ActivitySource {

    val sourceType: ActivitySourceType

    /**
     * Returns the activities that started within the range, oldest first.
     *
     * @param from start of the range, inclusive
     * @param to end of the range, exclusive
     */
    suspend fun getActivities(from: LocalDateTime, to: LocalDateTime): List<Activity>
}
