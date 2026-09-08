package com.jaaliska.activitycalendar.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ActivityDao {

    /** Emits the activities started in `[fromInclusive, toExclusive)`, oldest first. */
    @Query(
        "SELECT * FROM activities " +
            "WHERE startTimeLocal >= :fromInclusive AND startTimeLocal < :toExclusive " +
            "ORDER BY startTimeLocal, id"
    )
    fun observeInRange(fromInclusive: String, toExclusive: String): Flow<List<ActivityEntity>>

    /** Returns the activities started in `[fromInclusive, toExclusive)`, oldest first. */
    @Query(
        "SELECT * FROM activities " +
            "WHERE startTimeLocal >= :fromInclusive AND startTimeLocal < :toExclusive " +
            "ORDER BY startTimeLocal, id"
    )
    suspend fun getInRange(fromInclusive: String, toExclusive: String): List<ActivityEntity>

    /** Returns every stored activity, oldest first. */
    @Query("SELECT * FROM activities ORDER BY startTimeLocal, id")
    suspend fun getAll(): List<ActivityEntity>

    /** Emits how many stored activities came from [source]. */
    @Query("SELECT COUNT(*) FROM activities WHERE source = :source")
    fun observeCountOfSource(source: String): Flow<Int>

    /** Emits the start time of the oldest stored activity, `null` while nothing is stored. */
    @Query("SELECT MIN(startTimeLocal) FROM activities")
    fun observeEarliestStart(): Flow<String?>

    /**
     * Inserts [activities], skipping those whose start time and type are already stored.
     *
     * @return the new row ids, `-1` in place of every skipped activity
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(activities: List<ActivityEntity>): List<Long>

    /** Returns the stored activities that started at one of [startTimes]. */
    @Query("SELECT * FROM activities WHERE startTimeLocal IN (:startTimes)")
    suspend fun getByStartTimes(startTimes: List<String>): List<ActivityEntity>

    /** Rewrites the given rows, each matched by its id. */
    @Update
    suspend fun updateAll(activities: List<ActivityEntity>)

    @Query("DELETE FROM activities WHERE source = :source")
    suspend fun deleteBySource(source: String)

    @Query("DELETE FROM activities")
    suspend fun clear()
}
