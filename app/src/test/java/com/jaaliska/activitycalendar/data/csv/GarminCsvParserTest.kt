package com.jaaliska.activitycalendar.data.csv

import com.jaaliska.activitycalendar.domain.Activity
import com.jaaliska.activitycalendar.domain.ActivitySourceType
import com.jaaliska.activitycalendar.domain.ActivityType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.InputStream
import java.time.Duration
import java.time.LocalDateTime

class GarminCsvParserTest {

    private val parser = GarminCsvParser()

    private val result = parser.parse(fixture())

    private fun activityAt(startTimeLocal: String): Activity =
        result.activities.first { it.startTimeLocal == LocalDateTime.parse(startTimeLocal) }

    @Test
    fun `reads every row it can and skips the rest`() {
        assertEquals(9, result.activities.size)
        assertEquals(2, result.skippedRows.size)
    }

    @Test
    fun `start time is read as local time`() {
        assertEquals(
            LocalDateTime.of(2026, 8, 18, 9, 5, 52),
            result.activities.first().startTimeLocal,
        )
    }

    @Test
    fun `known types are mapped, unknown ones fall back`() {
        assertEquals(ActivityType.BADMINTON, activityAt("2026-08-18T09:05:52").type)
        assertEquals(ActivityType.RUNNING, activityAt("2026-08-17T18:37:11").type)
        assertEquals(ActivityType.STRENGTH_TRAINING, activityAt("2026-08-11T11:04:14").type)
        assertEquals(ActivityType.YOGA, activityAt("2026-06-16T22:27:49").type)
        assertEquals(ActivityType.CYCLING, activityAt("2026-05-02T08:12:00").type)
        assertEquals(ActivityType.WALKING, activityAt("2026-04-24T13:45:41").type)
        assertEquals(ActivityType.UNKNOWN, activityAt("2026-04-20T19:00:00").type)
    }

    @Test
    fun `kilometres become metres`() {
        assertEquals(350.0, activityAt("2026-08-18T09:05:52").distanceMeters!!, 0.0)
        assertEquals(7210.0, activityAt("2026-08-17T18:37:11").distanceMeters!!, 0.0)
    }

    @Test
    fun `a thousands separator inside a number is not a decimal point`() {
        assertEquals(1_234_500.0, activityAt("2026-05-02T08:12:00").distanceMeters!!, 0.0)
    }

    @Test
    fun `missing distance and zero distance both mean no distance`() {
        assertNull(activityAt("2026-08-11T11:04:14").distanceMeters)
        assertNull(activityAt("2026-06-16T22:27:49").distanceMeters)
    }

    @Test
    fun `duration is read down to a whole second`() {
        assertEquals(Duration.ofSeconds(3584), activityAt("2026-08-18T09:05:52").duration)
        assertEquals(Duration.ofSeconds(455), activityAt("2026-04-24T13:45:41").duration)
    }

    @Test
    fun `a title may hold the separator itself`() {
        assertEquals("Morning run, easy", activityAt("2026-08-17T18:37:11").title)
    }

    @Test
    fun `an empty title is no title`() {
        assertNull(activityAt("2026-04-24T13:45:41").title)
    }

    @Test
    fun `every activity is marked as coming from the csv`() {
        assertTrue(result.activities.all { it.source == ActivitySourceType.GARMIN_CSV })
        assertTrue(result.activities.all { it.sourceId == null })
    }

    @Test
    fun `a row without a start time or with an unreadable duration is skipped`() {
        assertEquals(listOf(9, 10), result.skippedRows.map { it.number })
    }

    @Test
    fun `a file that is not a garmin export is rejected`() {
        val notAnExport = "Name,Age\r\nAlice,30\r\n".byteInputStream()

        assertThrows(IllegalArgumentException::class.java) { parser.parse(notAnExport) }
    }

    @Test
    fun `an empty file yields nothing`() {
        val empty = "".byteInputStream()

        val parsed = parser.parse(empty)

        assertTrue(parsed.activities.isEmpty())
        assertTrue(parsed.skippedRows.isEmpty())
    }

    private companion object {
        fun fixture(): InputStream = checkNotNull(
            GarminCsvParserTest::class.java.getResourceAsStream("/garmin-activities.csv")
        )
    }
}
