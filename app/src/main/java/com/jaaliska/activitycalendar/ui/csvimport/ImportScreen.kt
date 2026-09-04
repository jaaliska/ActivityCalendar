package com.jaaliska.activitycalendar.ui.csvimport

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jaaliska.activitycalendar.R
import com.jaaliska.activitycalendar.domain.usecase.ImportReport
import com.jaaliska.activitycalendar.ui.UI_DATE
import com.jaaliska.activitycalendar.ui.components.DetailTopBar

@Composable
fun ImportScreen(
    state: ImportUiState,
    onFilePicked: (Uri) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var instructionExpanded by rememberSaveable { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) onFilePicked(uri)
    }

    Scaffold(
        modifier = modifier,
        topBar = { DetailTopBar(title = stringResource(R.string.import_title), onBack = onBack) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.import_intro),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp),
            )

            ChooseFileButton(
                label = if (state is ImportUiState.Failed) {
                    stringResource(R.string.import_choose_another_file)
                } else {
                    stringResource(R.string.import_choose_file)
                },
                enabled = state !is ImportUiState.Running,
                onClick = { picker.launch(CSV_MIME_TYPES) },
            )

            when (state) {
                ImportUiState.Idle -> Unit
                is ImportUiState.Running -> ProgressCard(state.fileName)
                is ImportUiState.Done -> ReportCard(state.report)
                is ImportUiState.Failed -> ErrorCard(state)
            }

            InstructionCard(
                expanded = instructionExpanded,
                enabled = state !is ImportUiState.Running,
                onToggle = { instructionExpanded = !instructionExpanded },
            )
        }
    }
}

@Composable
private fun ChooseFileButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_folder_open),
            contentDescription = null,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

@Composable
private fun ProgressCard(fileName: String) {
    Card {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(R.drawable.ic_folder_open),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text(text = fileName, style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = stringResource(R.string.import_reading),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        LinearProgressIndicator(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
        )
    }
}

@Composable
private fun ReportCard(report: ImportReport) {
    Card {
        CardHeader(
            icon = R.drawable.ic_check_circle,
            title = stringResource(R.string.import_complete),
            tint = MaterialTheme.colorScheme.primary,
        )
        Row(modifier = Modifier.padding(top = 20.dp)) {
            Count(
                value = report.imported,
                label = stringResource(R.string.import_added),
                modifier = Modifier.weight(1f),
            )
            Count(
                value = report.duplicates,
                label = stringResource(R.string.import_duplicates),
                modifier = Modifier.weight(1f),
            )
            Count(
                value = report.skippedRows,
                label = stringResource(R.string.import_skipped),
                accented = report.skippedRows > 0,
                modifier = Modifier.weight(1f),
            )
        }
        if (report.from != null && report.to != null) {
            HorizontalDivider(modifier = Modifier.padding(top = 18.dp))
            Text(
                text = stringResource(
                    R.string.import_period,
                    report.from.format(UI_DATE),
                    report.to.format(UI_DATE),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 14.dp),
            )
        }
    }
    if (report.skippedRows > 0) {
        SkippedNote(report.skippedRows)
    }
}

@Composable
private fun Count(
    value: Int,
    label: String,
    modifier: Modifier = Modifier,
    accented: Boolean = false,
) {
    val color = if (accented) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    Column(modifier = modifier) {
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.headlineMedium,
            color = color,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = if (accented) color else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

@Composable
private fun SkippedNote(count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = RoundedCornerShape(12.dp),
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_warning),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = pluralStringResource(R.plurals.import_skipped_rows, count, count),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(start = 12.dp),
        )
    }
}

@Composable
private fun ErrorCard(state: ImportUiState.Failed) {
    val title = when (state.reason) {
        ImportUiState.Failed.Reason.NOT_A_GARMIN_EXPORT -> R.string.import_error_not_garmin
        ImportUiState.Failed.Reason.UNREADABLE -> R.string.import_error_unreadable
    }
    val explanation = when (state.reason) {
        ImportUiState.Failed.Reason.NOT_A_GARMIN_EXPORT -> R.string.import_error_columns
        ImportUiState.Failed.Reason.UNREADABLE -> R.string.import_error_open
    }
    Card {
        CardHeader(
            icon = R.drawable.ic_error,
            title = stringResource(title),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(R.string.import_error_unchanged),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 12.dp),
        )
        Text(
            text = stringResource(explanation),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
        if (state.fileName.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(8.dp),
                    )
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_folder_open),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = state.fileName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 10.dp),
                )
            }
        }
    }
}

@Composable
private fun InstructionCard(expanded: Boolean, enabled: Boolean, onToggle: () -> Unit) {
    Card(
        modifier = Modifier
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
            .then(if (enabled) Modifier.clickable(onClick = onToggle) else Modifier),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.instruction_title),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            Icon(
                painter = painterResource(R.drawable.ic_expand_more),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.rotate(if (expanded) 180f else 0f),
            )
        }
        AnimatedVisibility(visible = expanded) {
            Column(modifier = Modifier.padding(top = 14.dp)) {
                INSTRUCTION_STEPS.forEachIndexed { index, step ->
                    Step(number = index + 1, text = stringResource(step))
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                INSTRUCTION_WARNINGS.forEach { (icon, text) ->
                    Warning(icon = icon, text = stringResource(text))
                }
            }
        }
    }
}

@Composable
private fun Step(number: Int, text: String) {
    Row(modifier = Modifier.padding(bottom = 12.dp)) {
        Text(
            text = number.toString(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier.size(width = 20.dp, height = 20.dp),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

@Composable
private fun Warning(icon: Int, text: String) {
    Row(modifier = Modifier.padding(bottom = 12.dp)) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 10.dp),
        )
    }
}

@Composable
private fun CardHeader(icon: Int, title: String, tint: androidx.compose.ui.graphics.Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = 10.dp),
        )
    }
}

@Composable
private fun Card(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = RoundedCornerShape(12.dp),
            )
            .padding(16.dp),
    ) {
        content()
    }
}

private val INSTRUCTION_STEPS = listOf(
    R.string.instruction_step_open,
    R.string.instruction_step_activities,
    R.string.instruction_step_scroll,
    R.string.instruction_step_export,
    R.string.instruction_step_save,
)

private val INSTRUCTION_WARNINGS = listOf(
    R.drawable.ic_warning to R.string.instruction_warning_scroll,
    R.drawable.ic_error to R.string.instruction_warning_metric,
    R.drawable.ic_error to R.string.instruction_warning_english,
)

private const val DISABLED_ALPHA = 0.5f

/** File providers disagree on how a `.csv` is typed, so the picker accepts all of these. */
private val CSV_MIME_TYPES = arrayOf(
    "text/csv",
    "text/comma-separated-values",
    "text/plain",
    "application/octet-stream",
)
