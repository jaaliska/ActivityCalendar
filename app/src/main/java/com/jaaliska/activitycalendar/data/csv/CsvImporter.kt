package com.jaaliska.activitycalendar.data.csv

import android.util.Log
import com.jaaliska.activitycalendar.domain.ActivityRepository
import java.io.InputStream
import java.time.LocalDate

/** What one import did. */
data class CsvImportReport(
    val imported: Int,
    val duplicates: Int,
    val skippedRows: Int,
    val from: LocalDate?,
    val to: LocalDate?,
)

/** Puts a Garmin CSV export into the repository. */
class CsvImporter(
    private val repository: ActivityRepository,
    private val parser: GarminCsvParser = GarminCsvParser(),
) {

    /**
     * Reads [input] and stores every activity it holds, keeping the ones already stored as they are.
     *
     * @param input the export file; the caller closes it
     * @return how the import went
     * @throws IllegalArgumentException if the file is not a Garmin export
     */
    suspend fun import(input: InputStream): CsvImportReport {
        val parsed = parser.parse(input)
        parsed.skippedRows.forEach { Log.w(TAG, "row ${it.number} skipped: ${it.reason}") }

        val imported = repository.save(parsed.activities)
        val days = parsed.activities.map { it.startTimeLocal.toLocalDate() }
        return CsvImportReport(
            imported = imported,
            duplicates = parsed.activities.size - imported,
            skippedRows = parsed.skippedRows.size,
            from = days.minOrNull(),
            to = days.maxOrNull(),
        )
    }

    private companion object {
        const val TAG = "CsvImporter"
    }
}
