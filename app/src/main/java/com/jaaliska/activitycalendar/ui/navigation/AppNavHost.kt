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
import com.jaaliska.activitycalendar.ui.settings.SettingsScreen
import com.jaaliska.activitycalendar.ui.settings.SettingsViewModel
import com.jaaliska.activitycalendar.ui.viewModelFactoryOf

@Composable
fun AppNavHost(container: AppContainer, modifier: Modifier = Modifier) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Destination.CALENDAR.route,
        modifier = modifier,
    ) {
        composable(Destination.CALENDAR.route) {
            val calendarViewModel: CalendarViewModel = viewModel(
                factory = viewModelFactoryOf { CalendarViewModel(container.activityRepository) },
            )
            val calendarState by calendarViewModel.state.collectAsState()

            CalendarScreen(
                state = calendarState,
                onSettingsClick = { navController.navigate(Destination.SETTINGS.route) },
                onImportClick = { navController.navigate(Destination.IMPORT.route) },
                onHealthConnectClick = { navController.navigate(Destination.HEALTH_CONNECT.route) },
                onRetry = calendarViewModel::retry,
            )
        }
        composable(Destination.SETTINGS.route) {
            val settingsViewModel: SettingsViewModel = viewModel(
                factory = viewModelFactoryOf { SettingsViewModel(container.importHistory) },
            )
            val state by settingsViewModel.state.collectAsState()

            SettingsScreen(
                state = state,
                onBack = { navController.popBackStack() },
                onImportClick = { navController.navigate(Destination.IMPORT.route) },
                onHealthConnectClick = { navController.navigate(Destination.HEALTH_CONNECT.route) },
            )
        }
        composable(Destination.IMPORT.route) {
            val importViewModel: ImportViewModel = viewModel(
                factory = viewModelFactoryOf {
                    ImportViewModel(
                        importer = container.csvImporter,
                        fileSource = container.fileSource,
                        importHistory = container.importHistory,
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
            HealthConnectScreen(onBack = { navController.popBackStack() })
        }
    }
}
