package com.jaaliska.activitycalendar.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.Duration
import java.time.LocalDateTime

/** D5: which source wins which field. */
class ActivityMergeTest {

    private val start = LocalDateTime.of(2026, 8, 12, 7, 30, 0)

    private val fromCsv = activity(
        source = ActivitySourceType.GARMIN_CSV,
        duration = Duration.ofMinutes(38),
        distanceMeters = 6500.0,
        title = "Warsaw Running",
    )

    private val fromHealthConnect = activity(
        source = ActivitySourceType.HEALTH_CONNECT,
        duration = Duration.ofSeconds(2311),
        distanceMeters = 6483.2,
        title = null,
    )

    @Test
    fun `title comes from the CSV, distance and duration from Health Connect`() {
        val merged = fromCsv.mergeWith(fromHealthConnect)

        assertEquals("Warsaw Running", merged.title)
        assertEquals(6483.2, merged.distanceMeters!!, 0.001)
        assertEquals(Duration.ofSeconds(2311), merged.duration)
    }

    @Test
    fun `the order the two records arrive in does not change the result`() {
        val csvFirst = fromCsv.mergeWith(fromHealthConnect)
        val healthConnectFirst = fromHealthConnect.mergeWith(fromCsv)

        assertEquals(csvFirst.title, healthConnectFirst.title)
        assertEquals(csvFirst.distanceMeters, healthConnectFirst.distanceMeters)
        assertEquals(csvFirst.duration, healthConnectFirst.duration)
    }

    @Test
    fun `a missing value loses to a present one whatever the source ranking`() {
        val csvWithoutDistance = fromCsv.copy(distanceMeters = null)
        val healthConnectWithoutDistance = fromHealthConnect.copy(distanceMeters = null)

        assertEquals(6483.2, csvWithoutDistance.mergeWith(fromHealthConnect).distanceMeters!!, 0.001)
        assertEquals(6500.0, fromCsv.mergeWith(healthConnectWithoutDistance).distanceMeters!!, 0.001)
    }

    @Test
    fun `manual values outrank both imports`() {
        val manual = activity(
            source = ActivitySourceType.MANUAL,
            duration = Duration.ofMinutes(40),
            distanceMeters = 7000.0,
            title = "Morning run",
        )

        val merged = manual.mergeWith(fromHealthConnect).mergeWith(fromCsv)

        assertEquals("Morning run", merged.title)
        assertEquals(7000.0, merged.distanceMeters!!, 0.001)
        assertEquals(Duration.ofMinutes(40), merged.duration)
    }

    @Test
    fun `identity of the merged record stays the one it is merged into`() {
        val stored = fromCsv.copy(id = 7, sourceId = null)

        val merged = stored.mergeWith(fromHealthConnect.copy(sourceId = "hc-1"))

        assertEquals(7L, merged.id)
        assertEquals(ActivitySourceType.GARMIN_CSV, merged.source)
        assertNull(merged.sourceId)
    }

    @Test
    fun `two different activities cannot be merged`() {
        assertThrows(IllegalArgumentException::class.java) {
            fromCsv.mergeWith(fromHealthConnect.copy(type = ActivityType.WALKING))
        }
    }

    @Test
    fun `duplicates inside one batch fold into a single activity`() {
        val batch = listOf(
            fromHealthConnect,
            fromCsv,
            fromHealthConnect.copy(startTimeLocal = start.plusHours(11)),
        )

        val folded = batch.mergeDuplicates()

        assertEquals(2, folded.size)
        assertEquals("Warsaw Running", folded.first().title)
        assertEquals(6483.2, folded.first().distanceMeters!!, 0.001)
    }

    private fun activity(
        source: ActivitySourceType,
        duration: Duration,
        distanceMeters: Double?,
        title: String?,
    ) = Activity(
        startTimeLocal = start,
        type = ActivityType.RUNNING,
        duration = duration,
        distanceMeters = distanceMeters,
        title = title,
        sourceId = null,
        source = source,
    )
}
