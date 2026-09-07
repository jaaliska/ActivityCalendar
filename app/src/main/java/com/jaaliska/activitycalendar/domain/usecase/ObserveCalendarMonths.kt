package com.jaaliska.activitycalendar.domain.usecase

import com.jaaliska.activitycalendar.domain.Activity
import com.jaaliska.activitycalendar.domain.ActivityRepository
import com.jaaliska.activitycalendar.domain.calendar.gridEndExclusive
import com.jaaliska.activitycalendar.domain.calendar.gridStart
import com.jaaliska.activitycalendar.domain.calendar.gridWeeks
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import java.time.YearMonth

/**
 * One day of a month's grid.
 *
 * @property inMonth false for a day of a neighbouring month, which keeps its own activities
 * @property activities the day's activities, in start-time order
 */
data class DayActivities(
    val date: LocalDate,
    val inMonth: Boolean,
    val activities: List<Activity>,
)

/**
 * One month of the calendar.
 *
 * @property weeks six weeks starting on a Monday, whether or not the month fills them
 * @property historyStart first day of the stored history, set only while the whole month
 * lies before it
 */
data class MonthActivities(
    val month: YearMonth,
    val weeks: List<List<DayActivities>>,
    val historyStart: LocalDate? = null,
)

/**
 * The months read around the one on screen.
 *
 * @property historyStart first day of the stored history, null while nothing is stored at all
 */
data class CalendarWindow(
    val months: List<MonthActivities>,
    val historyStart: LocalDate?,
)

/** Reads the months the calendar shows around the one it is on. */
class ObserveCalendarMonths(private val repository: ActivityRepository) {

    /**
     * Emits [month] and the month on each side of it, so paging to a neighbour finds it already
     * read, and re-emits on every change of what is stored.
     */
    operator fun invoke(month: YearMonth): Flow<CalendarWindow> {
        val window = month.minusMonths(1)..month.plusMonths(1)
        return combine(
            repository.observeRange(
                window.start.gridStart(),
                window.endInclusive.gridEndExclusive(),
            ),
            repository.observeHistoryStart(),
        ) { activities, historyStart -> window(window, activities, historyStart) }
    }

    private fun window(
        months: ClosedRange<YearMonth>,
        activities: List<Activity>,
        historyStart: LocalDate?,
    ): CalendarWindow {
        val byDay = activities.groupBy { it.startTimeLocal.toLocalDate() }
        return CalendarWindow(
            months = generateSequence(months.start) { it.plusMonths(1) }
                .takeWhile { it <= months.endInclusive }
                .map { month -> month(month, byDay, historyStart) }
                .toList(),
            historyStart = historyStart,
        )
    }

    private fun month(
        month: YearMonth,
        byDay: Map<LocalDate, List<Activity>>,
        historyStart: LocalDate?,
    ) = MonthActivities(
        month = month,
        weeks = month.gridWeeks().map { week ->
            week.map { date ->
                DayActivities(
                    date = date,
                    inMonth = YearMonth.from(date) == month,
                    activities = byDay[date].orEmpty(),
                )
            }
        },
        historyStart = historyStart?.takeIf { month.atEndOfMonth() < it },
    )
}
