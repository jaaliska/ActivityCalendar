package com.jaaliska.activitycalendar.ui.settings

import com.jaaliska.activitycalendar.domain.ColorSchemeChoice
import com.jaaliska.activitycalendar.domain.healthconnect.ConnectionStatus
import java.time.LocalDate

/** How the last export of the history ended, null until one has run. */
sealed interface ExportResult {

    /** @property activities how many activities went into the file */
    data class Done(val activities: Int) : ExportResult

    data object Failed : ExportResult
}

/** What the settings screen shows.
 *
 * @property lastImport day of the last successful CSV import, null if there was none
 * @property healthConnect what the Health Connect row says under its title
 */
data class SettingsUiState(
    val lastImport: LocalDate? = null,
    val healthConnect: ConnectionStatus = ConnectionStatus.NeverConnected,
    val colorScheme: ColorSchemeChoice = ColorSchemeChoice.BLUE,
    val demoActivities: Int = 0,
    val export: ExportResult? = null,
)
