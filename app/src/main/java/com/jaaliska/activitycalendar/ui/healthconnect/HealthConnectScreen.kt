package com.jaaliska.activitycalendar.ui.healthconnect

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import com.jaaliska.activitycalendar.R
import com.jaaliska.activitycalendar.ui.components.DetailTopBar
import com.jaaliska.activitycalendar.ui.components.OnResume
import java.time.Instant

/**
 * Status of the Health Connect connection and the one action it needs: connect, or read now.
 *
 * @param onPermissionsResult called when the system permission dialog closes, whatever was granted
 * @param onScreenResumed called on every return to the screen: permissions can change outside it
 */
@Composable
fun HealthConnectScreen(
    state: HealthConnectUiState,
    refreshing: Boolean,
    syncOutcome: SyncOutcome?,
    permissions: Set<String>,
    onPermissionsResult: () -> Unit,
    onSyncNow: () -> Unit,
    onRebuild: () -> Unit,
    onSyncOutcomeShown: () -> Unit,
    onScreenResumed: () -> Unit,
    onImportClick: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract(),
    ) { onPermissionsResult() }
    var confirmingRebuild by rememberSaveable { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    SyncOutcomeSnackbar(syncOutcome, snackbarHostState, onSyncOutcomeShown)

    if (confirmingRebuild) {
        RebuildDialog(
            onConfirm = {
                confirmingRebuild = false
                onRebuild()
            },
            onDismiss = { confirmingRebuild = false },
        )
    }

    OnResume(onScreenResumed)

    Scaffold(
        modifier = modifier,
        topBar = {
            DetailTopBar(title = stringResource(R.string.health_connect_title), onBack = onBack)
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            if (refreshing) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                when (state) {
                    is HealthConnectUiState.NotConnected -> NotConnected(
                        canAsk = state.canAsk,
                        onConnect = { permissionLauncher.launch(permissions) },
                        onOpenSettings = { context.openHealthConnectPermissions() },
                    )

                    is HealthConnectUiState.NotAvailable -> NotAvailable(
                        canInstall = state.canInstall,
                        onImportClick = onImportClick,
                        onInstall = { context.openHealthConnectInStore() },
                    )

                    HealthConnectUiState.Syncing -> Syncing()

                    is HealthConnectUiState.Connected -> Connected(
                        lastSync = state.lastSync,
                        backgroundSync = state.backgroundSync,
                        syncing = refreshing,
                        onSyncNow = onSyncNow,
                        onRebuild = { confirmingRebuild = true },
                        onFixBackground = { context.openHealthConnectPermissions() },
                    )

                    HealthConnectUiState.NoWorkouts -> NoWorkouts(
                        onCheckAgain = onSyncNow,
                        onOpenGarmin = context.garminConnectIntent()?.let {
                            { context.startActivity(it) }
                        },
                    )

                    is HealthConnectUiState.SyncFailed -> SyncFailed(
                        failedAt = state.failedAt,
                        lastSync = state.lastSync,
                        onTryAgain = onSyncNow,
                    )
                }
            }
        }
    }
}

@Composable
private fun NotConnected(canAsk: Boolean, onConnect: () -> Unit, onOpenSettings: () -> Unit) {
    StatusHeading(icon = R.drawable.ic_sync_disabled, text = stringResource(R.string.health_connect_not_connected_status))
    Explanation(stringResource(R.string.health_connect_not_connected_explanation))
    Card {
        Text(
            text = stringResource(R.string.health_connect_permissions_title),
            style = MaterialTheme.typography.bodyMedium,
        )
        Column(
            modifier = Modifier.padding(top = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Bullet(stringResource(R.string.health_connect_permission_workouts))
            Bullet(stringResource(R.string.health_connect_permission_distance))
            Bullet(stringResource(R.string.health_connect_permission_history))
            Bullet(stringResource(R.string.health_connect_permission_background))
        }
        Text(
            text = stringResource(R.string.health_connect_permission_background_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 12.dp),
        )
    }
    if (canAsk) {
        ActionButton(text = stringResource(R.string.health_connect_connect), onClick = onConnect)
    } else {
        Explanation(stringResource(R.string.health_connect_permissions_in_settings))
        ActionButton(
            text = stringResource(R.string.health_connect_open_settings),
            onClick = onOpenSettings,
        )
    }
}

@Composable
private fun NotAvailable(canInstall: Boolean, onImportClick: () -> Unit, onInstall: () -> Unit) {
    StatusHeading(icon = R.drawable.ic_sync_disabled, text = stringResource(R.string.health_connect_unavailable_status))
    Explanation(stringResource(R.string.health_connect_unavailable_explanation))
    Card {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(
                painter = painterResource(R.drawable.ic_warning),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.health_connect_unavailable_warning_title),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = stringResource(R.string.health_connect_unavailable_warning),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
    Card {
        Text(
            text = stringResource(R.string.health_connect_history_now_title),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
        )
        Text(
            text = stringResource(R.string.health_connect_history_now_explanation),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
        ActionButton(
            text = stringResource(R.string.health_connect_import_csv),
            onClick = onImportClick,
            modifier = Modifier.padding(top = 16.dp),
        )
    }
    if (canInstall) {
        QuietLink(text = stringResource(R.string.health_connect_install), onClick = onInstall)
    }
}

@Composable
private fun Syncing() {
    Card {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_sync),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp),
            )
            Text(
                text = stringResource(R.string.health_connect_syncing_status),
                style = MaterialTheme.typography.titleMedium,
            )
        }
        Text(
            text = stringResource(R.string.health_connect_syncing_explanation),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 6.dp),
        )
        LinearProgressIndicator(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
                .height(4.dp),
        )
    }
    ActionButton(
        text = stringResource(R.string.health_connect_sync_now),
        onClick = {},
        enabled = false,
    )
}

@Composable
private fun Connected(
    lastSync: Instant?,
    backgroundSync: Boolean,
    syncing: Boolean,
    onSyncNow: () -> Unit,
    onRebuild: () -> Unit,
    onFixBackground: () -> Unit,
) {
    Card {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_check_circle),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp),
            )
            Text(
                text = stringResource(R.string.health_connect_connected_status),
                style = MaterialTheme.typography.titleLarge,
            )
        }
        if (lastSync != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 12.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_schedule),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = stringResource(R.string.health_connect_last_sync, timeAgo(lastSync)),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        if (backgroundSync) {
            Text(
                text = stringResource(R.string.health_connect_connected_explanation),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 12.dp),
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 12.dp),
            ) {
                Text(
                    text = stringResource(R.string.health_connect_foreground_only),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = stringResource(R.string.health_connect_fix_it),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable(onClick = onFixBackground),
                )
            }
        }
    }
    ActionButton(
        text = stringResource(R.string.health_connect_sync_now),
        icon = R.drawable.ic_sync,
        onClick = onSyncNow,
        enabled = !syncing,
    )
    TextButton(
        onClick = onRebuild,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(stringResource(R.string.health_connect_rebuild))
    }
}

@Composable
private fun SyncOutcomeSnackbar(
    outcome: SyncOutcome?,
    snackbarHostState: SnackbarHostState,
    onShown: () -> Unit,
) {
    val message = outcome?.let { syncOutcomeMessage(it) }
    LaunchedEffect(outcome) {
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            onShown()
        }
    }
}

@Composable
private fun syncOutcomeMessage(outcome: SyncOutcome): String {
    val added = pluralStringResource(
        R.plurals.health_connect_sync_added,
        outcome.added,
        outcome.added,
    )
    val removed = pluralStringResource(
        R.plurals.health_connect_sync_removed,
        outcome.removed,
        outcome.removed,
    )
    return when {
        outcome.added == 0 && outcome.removed == 0 ->
            stringResource(R.string.health_connect_sync_up_to_date)

        outcome.removed == 0 -> added
        outcome.added == 0 -> removed
        else -> stringResource(R.string.health_connect_sync_added_and_removed, added, removed)
    }
}

@Composable
private fun RebuildDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.health_connect_rebuild_title)) },
        text = { Text(stringResource(R.string.health_connect_rebuild_text)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.health_connect_rebuild_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}

@Composable
private fun NoWorkouts(onCheckAgain: () -> Unit, onOpenGarmin: (() -> Unit)?) {
    StatusHeading(icon = R.drawable.ic_warning, text = stringResource(R.string.health_connect_no_workouts_status))
    Explanation(stringResource(R.string.health_connect_no_workouts_explanation))
    CheckItem(
        number = 1,
        title = stringResource(R.string.health_connect_check_garmin_title),
        explanation = stringResource(R.string.health_connect_check_garmin_explanation),
        link = stringResource(R.string.health_connect_open_garmin).takeIf { onOpenGarmin != null },
        onLinkClick = onOpenGarmin,
    )
    CheckItem(
        number = 2,
        title = stringResource(R.string.health_connect_check_watch_title),
        explanation = stringResource(R.string.health_connect_check_watch_explanation),
    )
    ActionButton(
        text = stringResource(R.string.health_connect_check_again),
        icon = R.drawable.ic_sync,
        onClick = onCheckAgain,
    )
}

@Composable
private fun SyncFailed(failedAt: Instant, lastSync: Instant?, onTryAgain: () -> Unit) {
    StatusHeading(icon = R.drawable.ic_error, text = stringResource(R.string.health_connect_failed_status))
    Explanation(stringResource(R.string.health_connect_failed_explanation, clockTime(failedAt)))
    Card {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_check_circle),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = stringResource(R.string.health_connect_still_connected),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
            )
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = 14.dp))
        Text(
            text = stringResource(R.string.health_connect_last_successful_sync),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = if (lastSync == null) {
                stringResource(R.string.health_connect_never_synced)
            } else {
                dayAndTime(lastSync)
            },
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(top = 2.dp),
        )
        if (lastSync != null) {
            Text(
                text = stringResource(R.string.health_connect_failed_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 10.dp),
            )
        }
    }
    ActionButton(
        text = stringResource(R.string.health_connect_try_again),
        icon = R.drawable.ic_sync,
        onClick = onTryAgain,
    )
    Text(
        text = stringResource(R.string.health_connect_automatic_note),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun StatusHeading(icon: Int, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(22.dp),
        )
        Text(text = text, style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
private fun Explanation(text: String) {
    Text(text = text, style = MaterialTheme.typography.bodyMedium)
}

@Composable
private fun Card(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(12.dp))
            .padding(16.dp),
    ) {
        content()
    }
}

@Composable
private fun Bullet(text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "·",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(text = text, style = MaterialTheme.typography.bodyMedium)
    }
}

/** One of the numbered things to check when Health Connect is connected but empty. */
@Composable
private fun CheckItem(
    number: Int,
    title: String,
    explanation: String,
    link: String? = null,
    onLinkClick: (() -> Unit)? = null,
) {
    Card {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = number.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = explanation,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (link != null && onLinkClick != null) {
                    Text(
                        text = link,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .clickable(onClick = onLinkClick),
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: Int? = null,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(24.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp),
    ) {
        if (icon != null) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
        }
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun QuietLink(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}


private fun Context.openHealthConnectInStore() {
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setPackage(PLAY_STORE_PACKAGE)
        data = "market://details?id=$HEALTH_CONNECT_PACKAGE&url=healthconnect%3A%2F%2Fonboarding".toUri()
        putExtra("overlay", true)
        putExtra("callerId", packageName)
    }
    runCatching { startActivity(intent) }
}

/**
 * Opens the permissions Health Connect keeps for this app. The system shows its own dialog
 * once; afterwards this screen is the only place where the missing permissions can be granted.
 */
private fun Context.openHealthConnectPermissions() {
    val forThisApp = Intent(ACTION_MANAGE_HEALTH_PERMISSIONS)
        .putExtra(Intent.EXTRA_PACKAGE_NAME, packageName)
    if (runCatching { startActivity(forThisApp) }.isSuccess) return
    runCatching { startActivity(Intent(HealthConnectClient.ACTION_HEALTH_CONNECT_SETTINGS)) }
}

private fun Context.garminConnectIntent(): Intent? =
    packageManager.getLaunchIntentForPackage(GARMIN_CONNECT_PACKAGE)

private val ACTION_MANAGE_HEALTH_PERMISSIONS =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        "android.health.connect.action.MANAGE_HEALTH_PERMISSIONS"
    } else {
        "androidx.health.ACTION_MANAGE_HEALTH_PERMISSIONS"
    }

private const val PLAY_STORE_PACKAGE = "com.android.vending"
private const val HEALTH_CONNECT_PACKAGE = "com.google.android.apps.healthdata"
private const val GARMIN_CONNECT_PACKAGE = "com.garmin.android.apps.connectmobile"
