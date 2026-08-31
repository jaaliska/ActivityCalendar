package com.jaaliska.activitycalendar.ui.settings

import java.time.LocalDate

/** What the settings screen shows.
 *
 * @property lastImport day of the last successful CSV import, null if there was none
 */
data class SettingsUiState(val lastImport: LocalDate? = null)
