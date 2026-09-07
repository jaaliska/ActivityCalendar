package com.jaaliska.activitycalendar.domain

import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/** When the user last imported a CSV export. */
interface ImportHistory {

    /** Date of the last successful import, or null if there has not been one. */
    val lastImport: Flow<LocalDate?>

    /** Remembers [date] as the day of the last successful import. */
    suspend fun record(date: LocalDate)
}
