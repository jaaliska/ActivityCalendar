package com.jaaliska.activitycalendar.ui.csvimport

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jaaliska.activitycalendar.domain.usecase.ImportActivities
import com.jaaliska.activitycalendar.ui.file.FileSource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ImportViewModel(
    private val importActivities: ImportActivities,
    private val fileSource: FileSource,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {

    private val _state = MutableStateFlow<ImportUiState>(ImportUiState.Idle)
    val state: StateFlow<ImportUiState> = _state.asStateFlow()

    /** Reads the file the picker returned and stores what it holds. */
    fun import(uri: Uri) {
        if (_state.value is ImportUiState.Running) return

        val fileName = runCatching { fileSource.displayName(uri) }.getOrDefault("")
        _state.value = ImportUiState.Running(fileName)

        viewModelScope.launch {
            _state.value = runCatching {
                withContext(ioDispatcher) {
                    fileSource.open(uri).use { importActivities(it) }
                }
            }.fold(
                onSuccess = { report -> ImportUiState.Done(report) },
                onFailure = { failure ->
                    ImportUiState.Failed(fileName, failure.toReason())
                },
            )
        }
    }

    private fun Throwable.toReason(): ImportUiState.Failed.Reason =
        if (this is IllegalArgumentException) {
            ImportUiState.Failed.Reason.NOT_A_GARMIN_EXPORT
        } else {
            ImportUiState.Failed.Reason.UNREADABLE
        }
}
