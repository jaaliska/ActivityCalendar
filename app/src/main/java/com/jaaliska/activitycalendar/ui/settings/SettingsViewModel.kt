package com.jaaliska.activitycalendar.ui.settings

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jaaliska.activitycalendar.domain.ActivityRepository
import com.jaaliska.activitycalendar.domain.ActivitySourceType
import com.jaaliska.activitycalendar.domain.AppearanceSettings
import com.jaaliska.activitycalendar.domain.ColorSchemeChoice
import com.jaaliska.activitycalendar.domain.ImportHistory
import com.jaaliska.activitycalendar.domain.healthconnect.ConnectionStatus
import com.jaaliska.activitycalendar.ui.file.FileSource
import com.jaaliska.activitycalendar.domain.usecase.ExportActivities
import com.jaaliska.activitycalendar.domain.usecase.GetHealthConnectStatus
import com.jaaliska.activitycalendar.domain.usecase.LoadDemoData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsViewModel(
    importHistory: ImportHistory,
    private val repository: ActivityRepository,
    private val appearanceSettings: AppearanceSettings,
    private val getHealthConnectStatus: GetHealthConnectStatus,
    private val loadDemoData: LoadDemoData,
    private val exportActivities: ExportActivities,
    private val fileSource: FileSource,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {

    private val healthConnect = MutableStateFlow<ConnectionStatus>(ConnectionStatus.NeverConnected)

    private var demoRunning = false

    private val export = MutableStateFlow<ExportResult?>(null)

    val state: StateFlow<SettingsUiState> = combine(
        importHistory.lastImport,
        healthConnect,
        appearanceSettings.colorScheme,
        repository.observeCountFrom(ActivitySourceType.DEMO).catch { emit(0) },
        export,
    ) { lastImport, connection, scheme, demo, export ->
        SettingsUiState(lastImport, connection, scheme, demo, export)
    }
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

    fun selectColorScheme(choice: ColorSchemeChoice) {
        viewModelScope.launch { appearanceSettings.setColorScheme(choice) }
    }

    /** Writes the whole history into the file the picker created. */
    fun export(uri: Uri) {
        viewModelScope.launch {
            export.value = runCatching {
                withContext(ioDispatcher) {
                    fileSource.openForWriting(uri).use { exportActivities(it) }
                }
            }.fold(
                onSuccess = { ExportResult.Done(it) },
                onFailure = { failure ->
                    Log.w(TAG, "export failed", failure)
                    ExportResult.Failed
                },
            )
        }
    }

    /** Clears the result once the screen has shown it. */
    fun exportShown() {
        export.value = null
    }

    fun loadDemo() = runDemo { loadDemoData() }

    fun removeDemo() = runDemo { repository.deleteAllFrom(ActivitySourceType.DEMO) }

    private fun runDemo(action: suspend () -> Unit) {
        if (demoRunning) return
        demoRunning = true
        viewModelScope.launch {
            runCatching { action() }.onFailure { Log.w(TAG, "demo data action failed", it) }
            demoRunning = false
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
        const val TAG = "SettingsViewModel"
    }
}
