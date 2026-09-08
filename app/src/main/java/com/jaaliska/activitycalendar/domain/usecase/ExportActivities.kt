package com.jaaliska.activitycalendar.domain.usecase

import com.jaaliska.activitycalendar.domain.ActivityFileWriter
import com.jaaliska.activitycalendar.domain.ActivityRepository
import java.io.OutputStream

/** Writes the stored history into a file the user keeps. */
class ExportActivities(
    private val repository: ActivityRepository,
    private val writer: ActivityFileWriter,
) {

    /**
     * Writes every stored activity, oldest first.
     *
     * @param output where the file goes; the caller closes the stream
     * @return how many activities were written
     */
    suspend operator fun invoke(output: OutputStream): Int {
        val activities = repository.getAll()
        writer.write(activities, output)
        return activities.size
    }
}
