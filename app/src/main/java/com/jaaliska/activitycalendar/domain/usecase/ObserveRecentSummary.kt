package com.jaaliska.activitycalendar.domain.usecase

import com.jaaliska.activitycalendar.domain.Activity
import com.jaaliska.activitycalendar.domain.ActivityRepository
import com.jaaliska.activitycalendar.domain.ActivityType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Duration
import java.time.LocalDate

/** How much of one activity type a period holds. */
data class TypeTotals(
    val type: ActivityType,
    val count: Int,
    val duration: Duration,
    /** Metres covered, null when no activity of the type recorded a distance. */
    val distanceMeters: Double?,
)

/**
 * The activities of a period, added up by type.
 *
 * @property from first day of the period
 * @property to last day of the period, itself included
 * @property byType one entry per type that happened, heaviest by time first
 */
data class PeriodSummary(
    val from: LocalDate,
    val to: LocalDate,
    val byType: List<TypeTotals>,
) {
    val isEmpty: Boolean get() = byType.isEmpty()
}

/** Adds up the seven days ending today: what the calendar shows below the grid. */
class ObserveRecentSummary(private val repository: ActivityRepository) {

    /** Emits the summary of the seven days ending on [today], and re-emits on every change. */
    operator fun invoke(today: LocalDate): Flow<PeriodSummary> {
        val from = today.minusDays(DAYS - 1)
        return repository.observeRange(from, today.plusDays(1))
            .map { activities -> PeriodSummary(from, today, activities.byType()) }
    }

    private fun List<Activity>.byType(): List<TypeTotals> = groupBy { it.type }
        .map { (type, activities) -> activities.totals(type) }
        .sortedWith(compareByDescending<TypeTotals> { it.duration }.thenBy { it.type })

    private fun List<Activity>.totals(type: ActivityType) = TypeTotals(
        type = type,
        count = size,
        duration = fold(Duration.ZERO) { total, activity -> total + activity.duration },
        distanceMeters = mapNotNull { it.distanceMeters }.takeIf { it.isNotEmpty() }?.sum(),
    )

    private companion object {
        const val DAYS = 7L
    }
}
