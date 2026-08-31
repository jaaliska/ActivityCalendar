package com.jaaliska.activitycalendar.ui.calendar

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.YearMonth

class MonthPagerTest {

    private val anchor = YearMonth.of(2026, 8)

    @Test
    fun `the page the calendar opens on is the anchor month`() {
        assertEquals(anchor, monthAt(ANCHOR_PAGE, anchor))
        assertEquals(ANCHOR_PAGE, pageOf(anchor, anchor))
    }

    @Test
    fun `the page before is the month before`() {
        assertEquals(YearMonth.of(2026, 7), monthAt(ANCHOR_PAGE - 1, anchor))
        assertEquals(YearMonth.of(2026, 9), monthAt(ANCHOR_PAGE + 1, anchor))
    }

    @Test
    fun `paging crosses years`() {
        assertEquals(YearMonth.of(2019, 2), monthAt(ANCHOR_PAGE - 90, anchor))
        assertEquals(ANCHOR_PAGE - 90, pageOf(YearMonth.of(2019, 2), anchor))
    }

    @Test
    fun `page and month are the same thing said two ways`() {
        listOf(
            YearMonth.of(1970, 1),
            YearMonth.of(2019, 12),
            anchor,
            YearMonth.of(2400, 6),
        ).forEach { month ->
            assertEquals(month, monthAt(pageOf(month, anchor), anchor))
        }
    }

    @Test
    fun `both ends of the pager are centuries away`() {
        assertEquals(YearMonth.of(1526, 8), monthAt(0, anchor))
        assertEquals(YearMonth.of(2526, 8), monthAt(PAGE_COUNT - 1, anchor))
    }
}
