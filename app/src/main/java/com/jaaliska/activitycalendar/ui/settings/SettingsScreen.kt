package com.jaaliska.activitycalendar.ui.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.jaaliska.activitycalendar.R
import com.jaaliska.activitycalendar.ui.UI_DATE
import com.jaaliska.activitycalendar.domain.ColorSchemeChoice
import com.jaaliska.activitycalendar.domain.healthconnect.ConnectionStatus
import com.jaaliska.activitycalendar.ui.components.DetailTopBar
import com.jaaliska.activitycalendar.ui.components.OnResume
import com.jaaliska.activitycalendar.ui.healthconnect.timeAgo
import com.jaaliska.activitycalendar.ui.theme.swatch
import java.time.LocalDate

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onBack: () -> Unit,
    onImportClick: () -> Unit,
    onHealthConnectClick: () -> Unit,
    onColorSchemeClick: (ColorSchemeChoice) -> Unit,
    onDemoLoadClick: () -> Unit,
    onDemoRemoveClick: () -> Unit,
    onScreenResumed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OnResume(onScreenResumed)

    Scaffold(
        modifier = modifier,
        topBar = { DetailTopBar(title = stringResource(R.string.settings_title), onBack = onBack) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            SectionHeader(stringResource(R.string.settings_section_data))

            SettingsRow(
                title = stringResource(R.string.settings_import_title),
                subtitle = importSubtitle(state.lastImport),
                onClick = onImportClick,
            )
            RowDivider()
            SettingsRow(
                title = stringResource(R.string.settings_health_connect_title),
                subtitle = healthConnectSubtitle(state.healthConnect),
                onClick = onHealthConnectClick,
            )
            RowDivider()
            if (state.demoActivities == 0) {
                SettingsRow(
                    title = stringResource(R.string.settings_demo_title),
                    subtitle = stringResource(R.string.settings_demo_subtitle),
                    onClick = onDemoLoadClick,
                    trailing = {},
                )
            } else {
                SettingsRow(
                    title = stringResource(R.string.settings_demo_loaded_title),
                    subtitle = pluralStringResource(
                        R.plurals.settings_demo_loaded_subtitle,
                        state.demoActivities,
                        state.demoActivities,
                    ),
                    trailing = {
                        TextButton(onClick = onDemoRemoveClick) {
                            Text(stringResource(R.string.settings_demo_remove))
                        }
                    },
                )
            }

            SectionHeader(stringResource(R.string.settings_section_appearance))

            SettingsRow(
                title = stringResource(R.string.settings_color_scheme_title),
                subtitle = stringResource(schemeName(state.colorScheme)),
                trailing = {
                    SchemeCircles(selected = state.colorScheme, onSelect = onColorSchemeClick)
                },
            )
        }
    }
}

@Composable
private fun importSubtitle(lastImport: LocalDate?): String =
    if (lastImport == null) {
        stringResource(R.string.settings_import_never)
    } else {
        stringResource(R.string.settings_import_last, lastImport.format(UI_DATE))
    }

@Composable
private fun healthConnectSubtitle(status: ConnectionStatus): String = when (status) {
    is ConnectionStatus.Connected ->
        if (status.lastSync == null) {
            stringResource(R.string.health_connect_connected_status)
        } else {
            stringResource(R.string.settings_health_connect_connected, timeAgo(status.lastSync))
        }

    ConnectionStatus.Unavailable -> stringResource(R.string.settings_health_connect_unavailable)
    else -> stringResource(R.string.settings_health_connect_not_connected)
}

@Composable
private fun SectionHeader(title: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.BottomStart,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp),
        )
    }
}

@Composable
private fun SettingsRow(
    title: String,
    subtitle: String,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .then(if (onClick == null) Modifier else Modifier.clickable(onClick = onClick))
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        when {
            trailing != null -> trailing()
            onClick != null -> Icon(
                painter = painterResource(R.drawable.ic_chevron_right),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SchemeCircles(
    selected: ColorSchemeChoice,
    onSelect: (ColorSchemeChoice) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        ColorSchemeChoice.entries.forEach { choice ->
            val name = stringResource(schemeName(choice))
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(color = choice.swatch)
                    .then(
                        if (choice != selected) {
                            Modifier
                        } else {
                            Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                        },
                    )
                    .clickable(onClickLabel = name) { onSelect(choice) }
                    .semantics { contentDescription = name },
            )
        }
    }
}

@StringRes
private fun schemeName(choice: ColorSchemeChoice): Int = when (choice) {
    ColorSchemeChoice.CRIMSON -> R.string.settings_color_scheme_crimson
    ColorSchemeChoice.BLUE -> R.string.settings_color_scheme_blue
    ColorSchemeChoice.ORANGE -> R.string.settings_color_scheme_orange
    ColorSchemeChoice.GREEN -> R.string.settings_color_scheme_green
}

@Composable
private fun RowDivider() {
    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
}
