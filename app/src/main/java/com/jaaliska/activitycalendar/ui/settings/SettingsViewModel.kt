package com.jaaliska.activitycalendar.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jaaliska.activitycalendar.domain.healthconnect.ConnectionStatus
import com.jaaliska.activitycalendar.domain.usecase.GetHealthConnectStatus
import com.jaaliska.activitycalendar.domain.ImportHistory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    importHistory: ImportHistory,
    private val getHealthConnectStatus: GetHealthConnectStatus,
) : ViewModel() {

    private val healthConnect = MutableStateFlow<ConnectionStatus>(ConnectionStatus.NeverConnected)

    val state: StateFlow<SettingsUiState> = combine(
        importHistory.lastImport,
        healthConnect,
    ) { lastImport, connection -> SettingsUiState(lastImport, connection) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = SettingsUiState(),
        )

    init {
        refresh()
    }

    /** Asks again how Health Connect stands: it is connected and disconnected outside this screen. */
    fun refresh() {
        viewModelScope.launch { healthConnect.value = getHealthConnectStatus() }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
