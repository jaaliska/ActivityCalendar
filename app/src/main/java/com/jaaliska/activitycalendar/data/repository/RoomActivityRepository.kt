package com.jaaliska.activitycalendar.data.repository

import com.jaaliska.activitycalendar.data.db.ActivityDao
import com.jaaliska.activitycalendar.data.db.toDbString
import com.jaaliska.activitycalendar.data.db.toDomain
import com.jaaliska.activitycalendar.data.db.toEntity
import com.jaaliska.activitycalendar.domain.Activity
import com.jaaliska.activitycalendar.domain.ActivityRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.YearMonth

class RoomActivityRepository(private val dao: ActivityDao) : ActivityRepository {

    override fun observeMonth(month: YearMonth): Flow<List<Activity>> {
        val (from, to) = month.bounds()
        return dao.observeInRange(from, to).map { rows -> rows.map { it.toDomain() } }
    }

    override suspend fun getMonth(month: YearMonth): List<Activity> {
        val (from, to) = month.bounds()
        return dao.getInRange(from, to).map { it.toDomain() }
    }

    override suspend fun save(activities: List<Activity>): Int =
        dao.insertAll(activities.map { it.toEntity() }).count { rowId -> rowId != -1L }
}

/** Start of the month inclusive, start of the next month exclusive. */
private fun YearMonth.bounds(): Pair<String, String> =
    atDay(1).atStartOfDay().toDbString() to plusMonths(1).atDay(1).atStartOfDay().toDbString()
