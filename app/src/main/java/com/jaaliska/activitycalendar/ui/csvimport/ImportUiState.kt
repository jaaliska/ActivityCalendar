package com.jaaliska.activitycalendar.ui.csvimport

import com.jaaliska.activitycalendar.domain.usecase.ImportReport

/** Where the import of a CSV file currently is. */
sealed interface ImportUiState {

    data object Idle : ImportUiState

    data class Running(val fileName: String) : ImportUiState

    data class Done(val report: ImportReport) : ImportUiState

    data class Failed(val fileName: String, val reason: Reason) : ImportUiState {

        /** Why the file could not be imported. */
        enum class Reason { NOT_A_GARMIN_EXPORT, UNREADABLE }
    }
}
