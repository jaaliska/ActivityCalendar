package com.jaaliska.activitycalendar.data.csv

import com.github.doyaaaaaken.kotlincsv.dsl.context.ExcessFieldsRowBehaviour
import com.github.doyaaaaaken.kotlincsv.dsl.context.InsufficientFieldsRowBehaviour
import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import com.jaaliska.activitycalendar.domain.Activity
import com.jaaliska.activitycalendar.domain.ActivitySourceType
import java.io.BufferedInputStream
import java.io.InputStream
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import kotlin.math.roundToLong

/** A row of the export the parser could not read. */
data class SkippedRow(val number: Int, val reason: String)

/** Everything one export file turned out to contain. */
data class CsvParseResult(
    val activities: List<Activity>,
    val skippedRows: List<SkippedRow>,
)

/** Reads the CSV that the Garmin Connect website exports from its Activities tab. */
class GarminCsvParser {

    /**
     * Reads every activity of the export, collecting the rows it had to skip instead of failing.
     *
     * @param input the export file; the caller closes it
     * @return the activities and the skipped rows
     * @throws IllegalArgumentException if the file is not a Garmin export
     */
    fun parse(input: InputStream): CsvParseResult {
        val activities = mutableListOf<Activity>()
        val skipped = mutableListOf<SkippedRow>()
        val stream = input.buffered()

        readerFor(stream.delimiterOfHeader()).open(stream) {
            readAllWithHeaderAsSequence().forEachIndexed { index, row ->
                if (index == 0) {
                    require(row.keys.containsAll(REQUIRED_COLUMNS)) {
                        "not a Garmin export, missing columns: ${REQUIRED_COLUMNS - row.keys}"
                    }
                }
                when (val result = row.toActivity()) {
                    is RowResult.Parsed -> activities += result.activity
                    is RowResult.Skipped -> skipped += SkippedRow(index + 1, result.reason)
                }
            }
        }
        return CsvParseResult(activities, skipped)
    }

    private fun Map<String, String>.toActivity(): RowResult {
        val rawStart = cell(COLUMN_DATE) ?: return RowResult.Skipped("no start time")
        val startTimeLocal = parseStartTime(rawStart)
            ?: return RowResult.Skipped("start time is not yyyy-MM-dd HH:mm:ss: $rawStart")

        val rawDuration = cell(COLUMN_TIME) ?: return RowResult.Skipped("no duration")
        val duration = parseDuration(rawDuration)
            ?: return RowResult.Skipped("duration is not hh:mm:ss: $rawDuration")

        return RowResult.Parsed(
            Activity(
                startTimeLocal = startTimeLocal,
                type = garminCsvActivityTypeOf(cell(COLUMN_TYPE)),
                duration = duration,
                distanceMeters = parseDistanceMeters(cell(COLUMN_DISTANCE)),
                title = cell(COLUMN_TITLE),
                sourceId = null,
                source = ActivitySourceType.GARMIN_CSV,
            )
        )
    }

    private sealed interface RowResult {
        data class Parsed(val activity: Activity) : RowResult
        data class Skipped(val reason: String) : RowResult
    }

    private companion object {

        const val COLUMN_TYPE = "Activity Type"
        const val COLUMN_DATE = "Date"
        const val COLUMN_TITLE = "Title"
        const val COLUMN_DISTANCE = "Distance"
        const val COLUMN_TIME = "Time"

        /** Without these a row cannot become an activity at all. */
        val REQUIRED_COLUMNS = setOf(COLUMN_DATE, COLUMN_TIME)

        /** What Garmin writes instead of leaving a cell empty. */
        const val MISSING_VALUE = "--"

        /** A byte order mark ends up inside the name of the very first column. */
        const val BOM = "\uFEFF"

        val START_TIME_FORMAT: DateTimeFormatter =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

        /** `hh:mm:ss`, with the fraction of a second some rows carry. */
        val DURATION_FORMAT = Regex("""(\d+):(\d{2}):(\d{2})(?:\.\d+)?""")

        /** A comma splitting off exactly three digits groups thousands; any other one is decimal. */
        val THOUSANDS_SEPARATOR = Regex(""",(?=\d{3}(?:\D|$))""")

        /** More than enough for the 43-column header; only used to sniff the delimiter. */
        const val HEADER_LIMIT = 8192

        fun readerFor(delimiter: Char) = csvReader {
            this.delimiter = delimiter
            insufficientFieldsRowBehaviour = InsufficientFieldsRowBehaviour.EMPTY_STRING
            excessFieldsRowBehaviour = ExcessFieldsRowBehaviour.TRIM
        }

        /**
         * Looks at the header to tell a Garmin export from one a spreadsheet has re-saved,
         * then rewinds so the reader still sees the whole file.
         */
        fun BufferedInputStream.delimiterOfHeader(): Char {
            mark(HEADER_LIMIT)
            val buffer = ByteArray(HEADER_LIMIT)
            val read = read(buffer)
            reset()
            if (read <= 0) return ','
            val header = String(buffer, 0, read, Charsets.UTF_8).substringBefore('\n')
            return if (header.count { it == ';' } > header.count { it == ',' }) ';' else ','
        }

        fun Map<String, String>.cell(column: String): String? {
            val value = (this[column] ?: this[BOM + column])?.trim() ?: return null
            return value.takeIf { it.isNotEmpty() && it != MISSING_VALUE }
        }

        fun parseStartTime(value: String): LocalDateTime? = try {
            LocalDateTime.parse(value, START_TIME_FORMAT)
        } catch (_: DateTimeParseException) {
            null
        }

        fun parseDuration(value: String): Duration? {
            val (hours, minutes, seconds) =
                DURATION_FORMAT.matchEntire(value)?.destructured ?: return null
            return Duration.ofHours(hours.toLong())
                .plusMinutes(minutes.toLong())
                .plusSeconds(seconds.toLong())
        }

        fun parseDistanceMeters(value: String?): Double? {
            val kilometers = value
                ?.replace(THOUSANDS_SEPARATOR, "")
                ?.replace(',', '.')
                ?.toDoubleOrNull()
                ?: return null
            return if (kilometers == 0.0) null else (kilometers * 1000).roundToLong().toDouble()
        }
    }
}
