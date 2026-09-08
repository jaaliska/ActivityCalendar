package com.jaaliska.activitycalendar.data.csv

import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter
import com.jaaliska.activitycalendar.domain.Activity
import com.jaaliska.activitycalendar.domain.ActivityFileWriter
import java.io.OutputStream
import java.time.Duration
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Writes activities as the CSV the Garmin Connect website exports. */
class GarminCsvWriter : ActivityFileWriter {

    override fun write(activities: List<Activity>, output: OutputStream) {
        csvWriter { lineTerminator = LINE_TERMINATOR }.open(output) {
            writeRow(HEADER)
            activities.forEach { writeRow(it.row()) }
        }
    }

    private fun Activity.row(): List<String> = listOf(
        garminCsvNameOf(type),
        startTimeLocal.format(START_TIME_FORMAT),
        title.orEmpty(),
        distanceMeters?.let { KILOMETRES.format(Locale.ROOT, it / METRES_IN_KILOMETRE) }
            ?: MISSING_VALUE,
        duration.formatted(),
    )

    private fun Duration.formatted(): String = String.format(
        Locale.ROOT,
        "%02d:%02d:%02d",
        seconds / SECONDS_IN_HOUR,
        seconds % SECONDS_IN_HOUR / SECONDS_IN_MINUTE,
        seconds % SECONDS_IN_MINUTE,
    )

    private companion object {

        val HEADER = listOf("Activity Type", "Date", "Title", "Distance", "Time")

        const val LINE_TERMINATOR = "\r\n"

        /** What Garmin writes instead of leaving a cell empty. */
        const val MISSING_VALUE = "--"

        /** Three decimals: the model stores metres, and the file keeps them. */
        const val KILOMETRES = "%.3f"

        const val METRES_IN_KILOMETRE = 1000.0

        const val SECONDS_IN_HOUR = 3600
        const val SECONDS_IN_MINUTE = 60

        val START_TIME_FORMAT: DateTimeFormatter =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    }
}
