package com.jaaliska.activitycalendar.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Stored form of an activity. */
@Entity(
    tableName = "activities",
    indices = [Index(value = ["startTimeLocal", "type"], unique = true)],
)
data class ActivityEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startTimeLocal: String,
    val type: String,
    val durationSeconds: Long,
    val distanceMeters: Double?,
    val title: String?,
    val sourceId: String?,
    val source: String,
)
