package com.jaaliska.activitycalendar.domain

import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.YearMonth

/** Access to the stored activities. */
interface ActivityRepository {

    /**
     * Emits the activities started in `[from, toExclusive)`, oldest first,
     * and re-emits on every change.
     */
    fun observeRange(from: LocalDate, toExclusive: LocalDate): Flow<List<Activity>>

    /** Emits the day of the oldest stored activity, `null` while the database is empty. */
    fun observeHistoryStart(): Flow<LocalDate?>

    /** Returns the activities of [month], oldest first. */
    suspend fun getMonth(month: YearMonth): List<Activity>

    /**
     * Stores [activities], skipping the ones already present.
     *
     * @return how many activities were actually stored
     */
    suspend fun save(activities: List<Activity>): Int
}
