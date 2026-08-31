package com.jaaliska.activitycalendar.ui.calendar

import org.junit.Assert.assertEquals
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
    fun `a month starting on a Monday starts the grid on its own first day`() {
        // 1 June 2026 is a Monday: nothing of May is shown.
        assertEquals(LocalDate.of(2026, 6, 1), YearMonth.of(2026, 6).gridStart())
    }

    @Test
    fun `the queried range is exactly the days the grid draws`() {
        val month = YearMonth.of(2026, 8)
        val weeks = month.gridWeeks()

        assertEquals(weeks.first().first(), month.gridStart())
        assertEquals(weeks.last().last().plusDays(1), month.gridEndExclusive())
    }
}
