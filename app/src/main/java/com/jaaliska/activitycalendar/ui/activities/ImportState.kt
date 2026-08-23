package com.jaaliska.activitycalendar.ui.activities

import com.jaaliska.activitycalendar.data.csv.CsvImportReport

/** Where the import of a CSV file currently is. */
sealed interface ImportState {
    data object Idle : ImportState
    data object Running : ImportState
    data class Done(val report: CsvImportReport) : ImportState
    data class Failed(val message: String) : ImportState
}
