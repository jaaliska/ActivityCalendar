package com.jaaliska.activitycalendar.domain

import java.io.OutputStream

/** Writes activities in the export format of another app. */
fun interface ActivityFileWriter {

    /**
     * Writes every activity of [activities] as one export file.
     *
     * @param output where the file goes; the caller closes the stream
     */
    fun write(activities: List<Activity>, output: OutputStream)
}
