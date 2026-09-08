package com.jaaliska.activitycalendar.ui.healthconnect

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jaaliska.activitycalendar.domain.healthconnect.HealthConnectAvailability
import com.jaaliska.activitycalendar.domain.healthconnect.HealthConnectSource
import com.jaaliska.activitycalendar.domain.healthconnect.HealthConnectSyncState
import com.jaaliska.activitycalendar.domain.usecase.SyncHealthConnect
import com.jaaliska.activitycalendar.domain.usecase.SyncScope
import com.jaaliska.activitycalendar.domain.usecase.SyncResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Instant

class HealthConnectViewModel(
    private val source: HealthConnectSource,
    private val syncHealthConnect: SyncHealthConnect,
    private val syncState: HealthConnectSyncState,
    private val clock: Clock = Clock.systemDefaultZone(),
) : ViewModel() {

    private val _state = MutableStateFlow<HealthConnectUiState>(availabilityState())
    val state: StateFlow<HealthConnectUiState> = _state.asStateFlow()

    /** The permissions the connect button asks the system for. */
    val permissions: Set<String> = source.permissions

    init {
        refresh()
    }

    /**
     * Re-reads availability and permissions: both can change while the screen is away.
     * A synchronisation started meanwhile has the last word, its answer is the newer one.
     */
    fun refresh() {
        viewModelScope.launch {
            if (_state.value == HealthConnectUiState.Syncing) return@launch
            val refreshed = currentState()
            if (_state.value != HealthConnectUiState.Syncing) _state.value = refreshed
        }
    }

    /** Reads Health Connect now. */
    fun sync() = startSync(afterPermissionDialog = false)

    /** Reads the whole history again, dropping the workouts Health Connect no longer has. */
    fun rebuild() = startSync(afterPermissionDialog = false, scope = SyncScope.WHOLE_HISTORY)

    /**
     * The way back from the system permission dialog: reads Health Connect if everything was
     * granted, and remembers that the dialog has been shown either way.
     */
    fun onPermissionsRequested() = startSync(afterPermissionDialog = true)

    private fun startSync(
        afterPermissionDialog: Boolean,
        scope: SyncScope = SyncScope.CHANGES,
    ) {
        if (_state.value == HealthConnectUiState.Syncing) return
        _state.value = HealthConnectUiState.Syncing
        viewModelScope.launch {
            if (afterPermissionDialog) syncState.recordPermissionsAsked()
            _state.value = when (syncHealthConnect(scope)) {
                is SyncResult.Synced -> connectedState()
                SyncResult.NotConnected -> currentState()
                is SyncResult.Failed -> HealthConnectUiState.SyncFailed(
                    failedAt = Instant.now(clock),
                    lastSync = syncState.lastSync.first(),
                )
            }
        }
    }

    private suspend fun currentState(): HealthConnectUiState = when {
        source.availability() != HealthConnectAvailability.AVAILABLE -> availabilityState()
        !source.hasRequiredPermissions() -> notConnectedState()
        else -> connectedState()
    }

    private suspend fun notConnectedState(): HealthConnectUiState =
        HealthConnectUiState.NotConnected(canAsk = !syncState.permissionsAsked.first())

    private suspend fun connectedState(): HealthConnectUiState =
        if (syncState.seenAnySession.first()) {
            HealthConnectUiState.Connected(
                lastSync = syncState.lastSync.first(),
                backgroundSync = source.canReadInBackground(),
            )
        } else {
            HealthConnectUiState.NoWorkouts
        }

    private fun availabilityState(): HealthConnectUiState =
        when (val availability = source.availability()) {
            HealthConnectAvailability.AVAILABLE -> HealthConnectUiState.NotConnected(canAsk = true)
            else -> HealthConnectUiState.NotAvailable(
                canInstall = availability == HealthConnectAvailability.NOT_INSTALLED,
            )
        }
}
