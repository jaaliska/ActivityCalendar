package com.jaaliska.activitycalendar.domain

import kotlinx.coroutines.flow.Flow
import java.time.YearMonth

/** Access to the stored activities. */
interface ActivityRepository {

    /** Emits the activities of [month], oldest first, and re-emits on every change. */
    fun observeMonth(month: YearMonth): Flow<List<Activity>>

    /** Returns the activities of [month], oldest first. */
    suspend fun getMonth(month: YearMonth): List<Activity>

    /**
     * Stores [activities], skipping the ones already present.
     *
     * @return how many activities were actually stored
     */
    suspend fun save(activities: List<Activity>): Int
}
