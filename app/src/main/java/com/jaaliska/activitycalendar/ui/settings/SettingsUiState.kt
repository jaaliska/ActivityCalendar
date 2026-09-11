package com.jaaliska.activitycalendar.ui.settings

import com.jaaliska.activitycalendar.domain.ColorSchemeChoice
import com.jaaliska.activitycalendar.domain.healthconnect.ConnectionStatus
import java.time.LocalDate

/** What the screen has to tell the user, null while it has nothing to say. */
sealed interface SettingsMessage {

    /** @property activities how many activities went into the exported file */
    data class Exported(val activities: Int) : SettingsMessage

    data object ExportFailed : SettingsMessage

    data object DemoFailed : SettingsMessage
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
    val message: SettingsMessage? = null,
)
