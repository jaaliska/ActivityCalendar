package com.jaaliska.activitycalendar.domain.usecase

import com.jaaliska.activitycalendar.domain.ActivityFileParser
import com.jaaliska.activitycalendar.domain.ActivityRepository
import com.jaaliska.activitycalendar.domain.ImportHistory
import java.io.InputStream
import java.time.Clock
import java.time.LocalDate

/** What one import did. */
data class ImportReport(
    val imported: Int,
    val duplicates: Int,
    val skippedRows: Int,
    val from: LocalDate?,
    val to: LocalDate?,
)

/** Puts an export file into the repository. */
class ImportActivities(
    private val repository: ActivityRepository,
    private val parser: ActivityFileParser,
    private val importHistory: ImportHistory,
    private val clock: Clock = Clock.systemDefaultZone(),
) {

    /**
     * Reads [input] and stores every activity it holds, keeping the ones already stored as they
     * are, then remembers the day of the import.
     *
     * @param input the export file; the caller closes it
     * @return how the import went
     * @throws IllegalArgumentException if the file is not an export the parser reads
     */
    suspend operator fun invoke(input: InputStream): ImportReport {
        val parsed = parser.parse(input)
        val imported = repository.save(parsed.activities)
        importHistory.record(LocalDate.now(clock))

        val days = parsed.activities.map { it.startTimeLocal.toLocalDate() }
        return ImportReport(
            imported = imported,
            duplicates = parsed.activities.size - imported,
            skippedRows = parsed.skippedRows.size,
            from = days.minOrNull(),
            to = days.maxOrNull(),
        )
    }
}
