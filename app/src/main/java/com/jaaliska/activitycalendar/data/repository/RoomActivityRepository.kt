package com.jaaliska.activitycalendar.data.repository

import com.jaaliska.activitycalendar.data.db.ActivityDao
import com.jaaliska.activitycalendar.data.db.toDbString
import com.jaaliska.activitycalendar.data.db.toDomain
import com.jaaliska.activitycalendar.data.db.toEntity
import com.jaaliska.activitycalendar.data.db.toLocalDateTimeOrThrow
import com.jaaliska.activitycalendar.domain.Activity
import com.jaaliska.activitycalendar.domain.ActivityRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.YearMonth

class RoomActivityRepository(private val dao: ActivityDao) : ActivityRepository {

    override fun observeRange(from: LocalDate, toExclusive: LocalDate): Flow<List<Activity>> =
        dao.observeInRange(from.atStartOfDay().toDbString(), toExclusive.atStartOfDay().toDbString())
            .map { rows -> rows.map { it.toDomain() } }

    override fun observeHistoryStart(): Flow<LocalDate?> =
        dao.observeEarliestStart().map { it?.toLocalDateTimeOrThrow()?.toLocalDate() }

    override suspend fun getMonth(month: YearMonth): List<Activity> {
        val from = month.atDay(1).atStartOfDay().toDbString()
        val to = month.plusMonths(1).atDay(1).atStartOfDay().toDbString()
        return dao.getInRange(from, to).map { it.toDomain() }
    }

    override suspend fun save(activities: List<Activity>): Int =
        dao.insertAll(activities.map { it.toEntity() }).count { rowId -> rowId != -1L }
}
