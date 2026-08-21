package com.jaaliska.activitycalendar.domain

import java.time.Duration
import java.time.LocalDateTime

/** A single training session in the terms of this app. */
data class Activity(
    val id: Long = 0,
    val startTimeLocal: LocalDateTime,
    val type: ActivityType,
    val duration: Duration,
    val distanceMeters: Double?,
    val title: String?,
    val sourceId: String?,
    val source: ActivitySourceType,
)
