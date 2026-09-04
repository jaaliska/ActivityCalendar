package com.jaaliska.activitycalendar.domain.calendar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

class MonthWindowTest {

    @Test
    fun `the grid always covers six weeks of seven days`() {
        listOf(
            YearMonth.of(2026, 8),
            YearMonth.of(2019, 2),
            YearMonth.of(2021, 2),
            YearMonth.of(2026, 3),
        ).forEach { month ->
            val weeks = month.gridWeeks()

            assertEquals(month.toString(), 6, weeks.size)
            assertEquals(month.toString(), listOf(7, 7, 7, 7, 7, 7), weeks.map { it.size })
        }
    }

    @Test
    fun `every week starts on a Monday`() {
        YearMonth.of(2026, 8).gridWeeks().forEach { week ->
            assertEquals(DayOfWeek.MONDAY, week.first().dayOfWeek)
            assertEquals(DayOfWeek.SUNDAY, week.last().dayOfWeek)
        }
    }

    @Test
    fun `a month starting on a Sunday still shows its first week whole`() {
        // 1 February 2026 is a Sunday: the first row is almost all January.
        val weeks = YearMonth.of(2026, 2).gridWeeks()

        assertEquals(LocalDate.of(2026, 1, 26), weeks.first().first())
        assertEquals(LocalDate.of(2026, 2, 1), weeks.first().last())
    }

    @Test
    fun `a month starting on a Monday opens with the last week of the previous one`() {
        // 1 June 2026 is a Monday.
        assertEquals(LocalDate.of(2026, 5, 25), YearMonth.of(2026, 6).gridStart())
    }

    @Test
    fun `a month starting on a Tuesday opens with the last week of the previous one`() {
        // 1 September 2026 is a Tuesday.
        assertEquals(LocalDate.of(2026, 8, 24), YearMonth.of(2026, 9).gridStart())
    }

    @Test
    fun `the grid holds every day of the month it draws`() {
        var month = YearMonth.of(2026, 1)
        repeat(MONTHS_OF_EVERY_LENGTH_AND_FIRST_DAY) {
            assertTrue(month.toString(), month.gridStart() <= month.atDay(1))
            assertTrue(month.toString(), month.atEndOfMonth() < month.gridEndExclusive())
            month = month.plusMonths(1)
        }
    }

    @Test
    fun `the queried range is exactly the days the grid draws`() {
        val month = YearMonth.of(2026, 8)
        val weeks = month.gridWeeks()

        assertEquals(weeks.first().first(), month.gridStart())
        assertEquals(weeks.last().last().plusDays(1), month.gridEndExclusive())
    }

    private companion object {
        /** Enough months in a row for every month length to meet every possible first day. */
        const val MONTHS_OF_EVERY_LENGTH_AND_FIRST_DAY = 12 * 28
    }
}
