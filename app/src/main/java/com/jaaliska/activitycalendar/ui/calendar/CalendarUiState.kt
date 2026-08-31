package com.jaaliska.activitycalendar.ui.calendar

import com.jaaliska.activitycalendar.domain.ActivityType
import java.time.LocalDate
import java.time.YearMonth

/** What the calendar screen shows. */
sealed interface CalendarUiState {

    /** Nothing has ever been stored: the grid gives way to the two ways of getting data. */
    data object NoData : CalendarUiState

    /** The month could not be read. The data is still there, the app just could not get it. */
    data class Failed(val month: YearMonth) : CalendarUiState

    /**
     * A month of the calendar.
     *
     * @property weeks six weeks starting on a Monday, whether or not the month fills them
     * @property historyStart first day of the stored history, set only while the whole
     * month lies before it
     * @property reading the read is taking long enough to be worth a progress line
     */
    data class Month(
        val month: YearMonth,
        val weeks: List<List<CalendarDay>>,
        val historyStart: LocalDate? = null,
        val reading: Boolean = false,
    ) : CalendarUiState
}

/**
 * One cell of the grid.
 *
 * @property types types of the day's activities, in start-time order
 * @property inMonth false for a day of a neighbouring month, shown muted with its own activities
 */
data class CalendarDay(
    val date: LocalDate,
    val inMonth: Boolean,
    val isToday: Boolean,
    val types: List<ActivityType>,
)
