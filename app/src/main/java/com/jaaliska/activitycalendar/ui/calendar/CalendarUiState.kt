package com.jaaliska.activitycalendar.ui.calendar

import com.jaaliska.activitycalendar.domain.Activity
import com.jaaliska.activitycalendar.domain.ActivityType
import com.jaaliska.activitycalendar.domain.calendar.gridWeeks
import com.jaaliska.activitycalendar.domain.usecase.PeriodSummary
import java.time.LocalDate
import java.time.YearMonth

/** What the calendar screen shows. */
sealed interface CalendarUiState {

    /** Nothing has ever been stored: the grid gives way to the two ways of getting data. */
    data object NoData : CalendarUiState

    /** The month could not be read. The data is still there, the app just could not get it. */
    data class Failed(val month: YearMonth) : CalendarUiState

    /**
     * The months the calendar can page through.
     *
     * @property pages the months that have been read, by month; paging goes further than that
     * @property reading the read is taking long enough to be worth a progress line
     * @property selectedDay the day whose activities the panel shows, null while it shows
     * the last seven days instead
     * @property selectedDayActivities what that day holds, empty when it holds nothing
     * @property recent the last seven days, null until they have been read
     */
    data class Calendar(
        val today: LocalDate,
        val pages: Map<YearMonth, MonthPage> = emptyMap(),
        val reading: Boolean = false,
        val selectedDay: LocalDate? = null,
        val selectedDayActivities: List<Activity> = emptyList(),
        val recent: PeriodSummary? = null,
    ) : CalendarUiState {

        /** [month] as it should be drawn: an empty grid while it has not been read yet. */
        fun page(month: YearMonth): MonthPage = pages[month] ?: emptyPage(month, today)
    }
}

/**
 * One page of the pager.
 *
 * @property weeks six weeks starting on a Monday, whether or not the month fills them
 * @property historyStart first day of the stored history, set only while the whole month
 * lies before it
 */
data class MonthPage(
    val month: YearMonth,
    val weeks: List<List<CalendarDay>>,
    val historyStart: LocalDate? = null,
)

/**
 * One cell of the grid.
 *
 * @property activities the day's activities, in start-time order
 * @property inMonth false for a day of a neighbouring month, shown muted with its own activities
 */
data class CalendarDay(
    val date: LocalDate,
    val inMonth: Boolean,
    val isToday: Boolean,
    val activities: List<Activity>,
) {
    /** Types the cell draws marks for, in start-time order. */
    val types: List<ActivityType> get() = activities.map { it.type }

    val hasActivity: Boolean get() = activities.isNotEmpty()
}

private fun emptyPage(month: YearMonth, today: LocalDate) = MonthPage(
    month = month,
    weeks = month.gridWeeks().map { week ->
        week.map { date ->
            CalendarDay(
                date = date,
                inMonth = YearMonth.from(date) == month,
                isToday = date == today,
                activities = emptyList(),
            )
        }
    },
)
