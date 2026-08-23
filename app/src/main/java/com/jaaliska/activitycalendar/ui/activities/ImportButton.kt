package com.jaaliska.activitycalendar.ui.activities

import android.content.ContentResolver
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.jaaliska.activitycalendar.data.csv.CsvImportReport
import com.jaaliska.activitycalendar.data.csv.CsvImporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.format.DateTimeFormatter

/**
 * Button that asks the system for a CSV export and imports the file the user picks.
 *
 * Stays available after a successful import: picking the same file again is how
 * deduplication gets checked by hand.
 */
@Composable
fun ImportButton(importer: CsvImporter, modifier: Modifier = Modifier) {
    val contentResolver = LocalContext.current.contentResolver
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf<ImportState>(ImportState.Idle) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        state = ImportState.Running
        scope.launch {
            state = importFrom(importer, contentResolver, uri)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Button(
            onClick = { picker.launch(CSV_MIME_TYPES) },
            enabled = state !is ImportState.Running,
        ) {
            Text(text = "Загрузить данные")
        }
        ImportStatus(state)
    }
}

@Composable
private fun ImportStatus(state: ImportState) {
    when (state) {
        ImportState.Idle -> Unit

        ImportState.Running -> CircularProgressIndicator(
            modifier = Modifier
                .padding(top = 12.dp)
                .size(20.dp),
        )

        is ImportState.Done -> StatusText(state.report.describe())

        is ImportState.Failed -> StatusText(
            text = "Не удалось прочитать файл: ${state.message}",
            color = MaterialTheme.colorScheme.error,
        )
    }
}

@Composable
private fun StatusText(text: String, color: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = color,
        modifier = Modifier.padding(top = 8.dp),
    )
}

private suspend fun importFrom(
    importer: CsvImporter,
    contentResolver: ContentResolver,
    uri: Uri,
): ImportState = runCatching {
    withContext(Dispatchers.IO) {
        val input = contentResolver.openInputStream(uri) ?: error("файл недоступен")
        input.use { importer.import(it) }
    }
}.fold(
    onSuccess = ImportState::Done,
    onFailure = { ImportState.Failed(it.message ?: it.javaClass.simpleName) },
)

private fun CsvImportReport.describe(): String = buildString {
    append("Импортировано $imported, дублей $duplicates")
    if (skippedRows > 0) append(", пропущено строк $skippedRows")
    if (from != null && to != null) {
        append(" · ${from.format(REPORT_DATE_FORMAT)} — ${to.format(REPORT_DATE_FORMAT)}")
    }
}

/** File providers disagree on how a `.csv` is typed, so the picker accepts all of these. */
private val CSV_MIME_TYPES = arrayOf(
    "text/csv",
    "text/comma-separated-values",
    "text/plain",
    "application/octet-stream",
)

private val REPORT_DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
