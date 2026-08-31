package com.jaaliska.activitycalendar.ui.calendar

import java.time.YearMonth
import java.time.temporal.ChronoUnit

/**
 * How many months the pager reaches in each direction. Five centuries: the edge exists
 * because a pager needs a page count, and no one can page to it.
 */
private const val MONTHS_EACH_WAY = 6_000

/** The page the pager opens on, the one holding [anchor]. */
const val ANCHOR_PAGE = MONTHS_EACH_WAY

/** Every page the pager has. */
const val PAGE_COUNT = MONTHS_EACH_WAY * 2 + 1

/** The month page number [page] shows, counting from [anchor] at [ANCHOR_PAGE]. */
fun monthAt(page: Int, anchor: YearMonth): YearMonth =
    anchor.plusMonths((page - ANCHOR_PAGE).toLong())

/** The page [month] lives on. */
fun pageOf(month: YearMonth, anchor: YearMonth): Int =
    ANCHOR_PAGE + anchor.until(month, ChronoUnit.MONTHS).toInt()
