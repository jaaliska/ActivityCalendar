package com.jaaliska.activitycalendar.data.repository

import androidx.room.withTransaction
import com.jaaliska.activitycalendar.data.db.ActivityEntity
import com.jaaliska.activitycalendar.data.db.AppDatabase
import com.jaaliska.activitycalendar.data.db.toDbString
import com.jaaliska.activitycalendar.data.db.toDomain
import com.jaaliska.activitycalendar.data.db.toEntity
import com.jaaliska.activitycalendar.data.db.toLocalDateTimeOrThrow
import com.jaaliska.activitycalendar.domain.Activity
import com.jaaliska.activitycalendar.domain.ActivityRepository
import com.jaaliska.activitycalendar.domain.ActivitySourceType
import com.jaaliska.activitycalendar.domain.mergeDuplicates
import com.jaaliska.activitycalendar.domain.mergeWith
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.YearMonth

class RoomActivityRepository(private val database: AppDatabase) : ActivityRepository {

    private val dao = database.activityDao()

    override fun observeRange(from: LocalDate, toExclusive: LocalDate): Flow<List<Activity>> =
        dao.observeInRange(from.atStartOfDay().toDbString(), toExclusive.atStartOfDay().toDbString())
            .map { rows -> rows.map { it.toDomain() } }

    override fun observeHistoryStart(): Flow<LocalDate?> =
        dao.observeEarliestStart().map { it?.toLocalDateTimeOrThrow()?.toLocalDate() }

    override suspend fun getAll(): List<Activity> = dao.getAll().map { it.toDomain() }

    override fun observeCountFrom(source: ActivitySourceType): Flow<Int> =
        dao.observeCountOfSource(source.name)

    override suspend fun getMonth(month: YearMonth): List<Activity> {
        val from = month.atDay(1).atStartOfDay().toDbString()
        val to = month.plusMonths(1).atDay(1).atStartOfDay().toDbString()
        return dao.getInRange(from, to).map { it.toDomain() }
    }

    override suspend fun save(activities: List<Activity>): Int = database.withTransaction {
        val incoming = activities.mergeDuplicates()
        val rowIds = dao.insertAll(incoming.map { it.toEntity() })
        val alreadyStored = incoming.filterIndexed { index, _ -> rowIds[index] == SKIPPED }
        mergeIntoStored(alreadyStored)
        rowIds.count { it != SKIPPED }
    }

    override suspend fun deleteAllFrom(source: ActivitySourceType) =
        dao.deleteBySource(source.name)

    /** Applies what [activities] know to the rows already holding the same activities. */
    private suspend fun mergeIntoStored(activities: List<Activity>) {
        val changed = activities
            .chunked(START_TIMES_PER_QUERY)
            .flatMap { chunk -> mergeChunk(chunk) }
        if (changed.isNotEmpty()) dao.updateAll(changed)
    }

    private suspend fun mergeChunk(activities: List<Activity>): List<ActivityEntity> {
        val stored = dao
            .getByStartTimes(activities.map { it.startTimeLocal.toDbString() })
            .associateBy { it.startTimeLocal to it.type }
        return activities.mapNotNull { incoming ->
            val row = stored[incoming.startTimeLocal.toDbString() to incoming.type.name]
                ?: return@mapNotNull null
            // An unchanged row is left alone: rewriting it would make every reader re-read.
            row.toDomain().mergeWith(incoming).toEntity().takeIf { it != row }
        }
    }

    private companion object {
        /** What `INSERT OR IGNORE` returns in place of a row it did not insert. */
        const val SKIPPED = -1L

        /** Kept well under the number of values SQLite accepts in one `IN`. */
        const val START_TIMES_PER_QUERY = 400
    }
}
