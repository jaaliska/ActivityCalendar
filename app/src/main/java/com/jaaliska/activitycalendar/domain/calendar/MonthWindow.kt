package com.jaaliska.activitycalendar.domain.calendar

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters

/** Rows the grid always draws, so nothing below it moves when the month changes. */
const val WEEKS_IN_GRID = 6

private const val DAYS_IN_WEEK = 7

/**
 * The first day the grid of [month] shows: the Monday of the week its 1st falls into, or the
 * Monday a week earlier when the 1st is itself a Monday or a Tuesday.
 */
fun YearMonth.gridStart(): LocalDate {
    val monday = atDay(1).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    return if (atDay(1).dayOfWeek <= DayOfWeek.TUESDAY) monday.minusWeeks(1) else monday
}

/** The day after the last one the grid of [month] shows. */
fun YearMonth.gridEndExclusive(): LocalDate =
    gridStart().plusDays((WEEKS_IN_GRID * DAYS_IN_WEEK).toLong())

/** The six weeks of [month]'s grid, Monday first, each week seven days long. */
fun YearMonth.gridWeeks(): List<List<LocalDate>> {
    val start = gridStart()
    return List(WEEKS_IN_GRID) { week ->
        List(DAYS_IN_WEEK) { day -> start.plusDays((week * DAYS_IN_WEEK + day).toLong()) }
    }
}
