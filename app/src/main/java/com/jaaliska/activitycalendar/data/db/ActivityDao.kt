package com.jaaliska.activitycalendar.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
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

    /**
     * Inserts [activities], skipping those whose start time and type are already stored.
     *
     * @return the new row ids, `-1` in place of every skipped activity
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(activities: List<ActivityEntity>): List<Long>

    @Query("DELETE FROM activities")
    suspend fun clear()
}
