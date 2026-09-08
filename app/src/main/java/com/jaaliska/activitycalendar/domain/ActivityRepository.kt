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

    /** Emits how many stored activities came from [source]. */
    fun observeCountFrom(source: ActivitySourceType): Flow<Int>

    /** Returns every stored activity, oldest first. */
    suspend fun getAll(): List<Activity>

    /** Returns the activities of [month], oldest first. */
    suspend fun getMonth(month: YearMonth): List<Activity>

    /**
     * Stores [activities]. An activity already stored is not stored twice: what the two
     * records know is merged into the stored one, field by field, as [mergeWith] decides.
     *
     * @return how many activities were new
     */
    suspend fun save(activities: List<Activity>): Int

    /** Removes every stored activity that came from [source]. */
    suspend fun deleteAllFrom(source: ActivitySourceType)
}
