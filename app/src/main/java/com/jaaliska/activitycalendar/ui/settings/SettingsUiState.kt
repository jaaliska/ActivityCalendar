package com.jaaliska.activitycalendar.ui.settings

import com.jaaliska.activitycalendar.data.healthconnect.ConnectionStatus
import java.time.LocalDate

/** What the settings screen shows.
 *
 * @property lastImport day of the last successful CSV import, null if there was none
 * @property healthConnect what the Health Connect row says under its title
 */
data class SettingsUiState(
    val lastImport: LocalDate? = null,
    val healthConnect: ConnectionStatus = ConnectionStatus.NeverConnected,
)
