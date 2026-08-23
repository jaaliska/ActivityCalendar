package com.jaaliska.activitycalendar.data.csv

import com.jaaliska.activitycalendar.domain.Activity
import com.jaaliska.activitycalendar.domain.ActivityType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Duration
import java.time.LocalDateTime

/**
 * A spreadsheet that re-saves the export writes it out in the list format of its own locale:
 * semicolons instead of commas, no quotes at all, and possibly a decimal comma.
 */
class GarminCsvParserSemicolonTest {

    private val result = GarminCsvParser().parse(
        checkNotNull(javaClass.getResourceAsStream("/garmin-activities-semicolon.csv"))
    )

    private fun activityAt(startTimeLocal: String): Activity =
        result.activities.first { it.startTimeLocal == LocalDateTime.parse(startTimeLocal) }

    @Test
    fun `a semicolon file reads exactly like a comma one`() {
        assertEquals(9, result.activities.size)
        assertEquals(listOf(9, 10), result.skippedRows.map { it.number })
    }

    @Test
    fun `columns are still found by name`() {
        assertEquals(ActivityType.BADMINTON, activityAt("2026-08-18T09:05:52").type)
        assertEquals(ActivityType.UNKNOWN, activityAt("2026-04-20T19:00:00").type)
        assertEquals(Duration.ofSeconds(455), activityAt("2026-04-24T13:45:41").duration)
        assertNull(activityAt("2026-08-11T11:04:14").distanceMeters)
    }

    @Test
    fun `an unquoted title may hold a comma`() {
        assertEquals("Morning run, easy", activityAt("2026-08-17T18:37:11").title)
    }

    @Test
    fun `a comma before two digits is a decimal point`() {
        assertEquals(7210.0, activityAt("2026-08-17T18:37:11").distanceMeters!!, 0.0)
        assertEquals(520.0, activityAt("2026-04-24T13:45:41").distanceMeters!!, 0.0)
    }

    @Test
    fun `a comma before three digits still groups thousands`() {
        assertEquals(1_234_500.0, activityAt("2026-05-02T08:12:00").distanceMeters!!, 0.0)
    }
}
