package com.jaaliska.activitycalendar.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.jaaliska.activitycalendar.AppContainer
import com.jaaliska.activitycalendar.ui.calendar.CalendarScreen
import com.jaaliska.activitycalendar.ui.calendar.CalendarViewModel
import com.jaaliska.activitycalendar.ui.csvimport.ImportScreen
import com.jaaliska.activitycalendar.ui.csvimport.ImportViewModel
import com.jaaliska.activitycalendar.ui.healthconnect.HealthConnectScreen
import com.jaaliska.activitycalendar.ui.healthconnect.HealthConnectViewModel
import com.jaaliska.activitycalendar.ui.settings.SettingsScreen
import com.jaaliska.activitycalendar.ui.settings.SettingsViewModel
import com.jaaliska.activitycalendar.ui.viewModelFactoryOf

@Composable
fun AppNavHost(
    container: AppContainer,
    startDestination: Destination,
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = startDestination.route,
        modifier = modifier,
    ) {
        composable(Destination.CALENDAR.route) {
            val calendarViewModel: CalendarViewModel = viewModel(
                factory = viewModelFactoryOf {
                    CalendarViewModel(
                        observeCalendarMonths = container.observeCalendarMonths,
                        observeRecentSummary = container.observeRecentSummary,
                        repository = container.activityRepository,
                        getHealthConnectStatus = container.getHealthConnectStatus,
                    )
                },
            )
            val calendarState by calendarViewModel.state.collectAsState()
            val syncStopped by calendarViewModel.syncStopped.collectAsState()

            CalendarScreen(
                state = calendarState,
                anchor = calendarViewModel.anchor,
                syncStopped = syncStopped,
                onMonthSettled = calendarViewModel::showMonth,
                onDaySelected = calendarViewModel::selectDay,
                onTodayClick = calendarViewModel::clearDaySelection,
                onSettingsClick = { navController.navigate(Destination.SETTINGS.route) },
                onImportClick = { navController.navigate(Destination.IMPORT.route) },
                onHealthConnectClick = { navController.navigate(Destination.HEALTH_CONNECT.route) },
                onRetry = calendarViewModel::retry,
                onScreenResumed = calendarViewModel::refreshSyncStatus,
            )
        }
        composable(Destination.SETTINGS.route) {
            val settingsViewModel: SettingsViewModel = viewModel(
                factory = viewModelFactoryOf {
                    SettingsViewModel(
                        importHistory = container.importHistory,
                        getHealthConnectStatus = container.getHealthConnectStatus,
                    )
                },
            )
            val state by settingsViewModel.state.collectAsState()

            SettingsScreen(
                state = state,
                onBack = { navController.popBackStack() },
                onImportClick = { navController.navigate(Destination.IMPORT.route) },
                onHealthConnectClick = { navController.navigate(Destination.HEALTH_CONNECT.route) },
                onScreenResumed = settingsViewModel::refresh,
            )
        }
        composable(Destination.IMPORT.route) {
            val importViewModel: ImportViewModel = viewModel(
                factory = viewModelFactoryOf {
                    ImportViewModel(
                        importActivities = container.importActivities,
                        fileSource = container.fileSource,
                    )
                },
            )
            val importState by importViewModel.state.collectAsState()

            ImportScreen(
                state = importState,
                onFilePicked = importViewModel::import,
                onBack = { navController.popBackStack() },
            )
        }
        composable(Destination.HEALTH_CONNECT.route) {
            val healthConnectViewModel: HealthConnectViewModel = viewModel(
                factory = viewModelFactoryOf {
                    HealthConnectViewModel(
                        source = container.healthConnectSource,
                        syncHealthConnect = container.syncHealthConnect,
                        syncState = container.healthConnectSyncState,
                    )
                },
            )
            val healthConnectState by healthConnectViewModel.state.collectAsState()

            HealthConnectScreen(
                state = healthConnectState,
                permissions = healthConnectViewModel.permissions,
                onPermissionsResult = healthConnectViewModel::onPermissionsRequested,
                onSyncNow = healthConnectViewModel::sync,
                onScreenResumed = healthConnectViewModel::refresh,
                onImportClick = { navController.navigate(Destination.IMPORT.route) },
                onBack = { navController.popBackStack() },
            )
        }
    }
}
