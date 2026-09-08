package com.jaaliska.activitycalendar.data.csv

import com.jaaliska.activitycalendar.data.source.FixtureActivitySource
import com.jaaliska.activitycalendar.domain.Activity
import com.jaaliska.activitycalendar.domain.ActivitySourceType
import com.jaaliska.activitycalendar.domain.ActivityType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.time.Duration
import java.time.LocalDateTime

class GarminCsvWriterTest {

    private val writer = GarminCsvWriter()

    @Test
    fun `a row carries the five columns the parser reads`() {
        val csv = write(
            activity(
                type = ActivityType.STRENGTH_TRAINING,
                distanceMeters = null,
                title = "Evening session",
            ),
        )

        assertEquals("Activity Type,Date,Title,Distance,Time", csv.lines()[0])
        assertEquals(
            "Strength Training,2026-08-05 07:15:00,Evening session,--,00:38:00",
            csv.lines()[1],
        )
    }

    @Test
    fun `an unknown type is written as Other and read back as unknown`() {
        val csv = write(activity(type = ActivityType.UNKNOWN))

        assertTrue(csv.lines()[1].startsWith("Other,"))
        assertEquals(ActivityType.UNKNOWN, parse(csv).single().type)
    }

    @Test
    fun `a title with a comma stays one column`() {
        val csv = write(activity(title = "Run, then coffee"))

        assertEquals("\"Run, then coffee\"", csv.lines()[1].split(",")[2] + "," + csv.lines()[1].split(",")[3])
        assertEquals("Run, then coffee", parse(csv).single().title)
    }

    @Test
    fun `an empty history is a file with nothing but the header`() {
        val csv = write()

        assertEquals("Activity Type,Date,Title,Distance,Time", csv.trim())
        assertEquals(emptyList<Activity>(), parse(csv))
    }

    @Test
    fun `the whole history survives a trip through the file`() {
        val exported = FixtureActivitySource.ALL

        val read = parse(write(*exported.toTypedArray()))

        assertEquals(exported.size, read.size)
        exported.zip(read).forEach { (written, readBack) ->
            assertEquals(written.startTimeLocal, readBack.startTimeLocal)
            assertEquals(written.type, readBack.type)
            assertEquals(written.duration, readBack.duration)
            assertEquals(written.title, readBack.title)
            assertEquals(written.distanceMeters ?: 0.0, readBack.distanceMeters ?: 0.0, 0.5)
        }
    }

    private fun write(vararg activities: Activity): String {
        val output = ByteArrayOutputStream()
        writer.write(activities.toList(), output)
        return output.toString(Charsets.UTF_8.name())
    }

    private fun parse(csv: String): List<Activity> =
        ByteArrayInputStream(csv.toByteArray()).use { GarminCsvParser().parse(it).activities }

    private fun activity(
        type: ActivityType = ActivityType.RUNNING,
        distanceMeters: Double? = 6483.2,
        title: String? = "Morning Run",
    ) = Activity(
        startTimeLocal = LocalDateTime.of(2026, 8, 5, 7, 15),
        type = type,
        duration = Duration.ofMinutes(38),
        distanceMeters = distanceMeters,
        title = title,
        sourceId = null,
        source = ActivitySourceType.GARMIN_CSV,
    )
}
