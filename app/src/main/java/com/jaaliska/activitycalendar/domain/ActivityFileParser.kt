package com.jaaliska.activitycalendar.domain

import java.io.InputStream

/** A row of an export the parser could not read. */
data class SkippedRow(val number: Int, val reason: String)

/** Everything one export file turned out to contain. */
data class ParsedActivities(
    val activities: List<Activity>,
    val skippedRows: List<SkippedRow>,
)

/** Reads an export file of another app into activities. */
fun interface ActivityFileParser {

    /**
     * Reads every activity of the export, collecting the rows it had to skip instead of failing.
     *
     * @param input the export file; the caller closes it
     * @return the activities and the skipped rows
     * @throws IllegalArgumentException if the file is not the export this parser reads
     */
    fun parse(input: InputStream): ParsedActivities
}
