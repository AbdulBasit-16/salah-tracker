package com.salah.tracker.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.salah.tracker.ui.dashboard.DashboardScreen
import com.salah.tracker.ui.insights.InsightsScreen
import com.salah.tracker.ui.quran.QuranHubScreen
import com.salah.tracker.ui.settings.SettingsScreen
import com.salah.tracker.viewmodel.QuranViewModel
import com.salah.tracker.viewmodel.SalahViewModel
import com.salah.tracker.viewmodel.SettingsViewModel

@Composable
fun AppNavigation(
    salahViewModel: SalahViewModel,
    quranViewModel: QuranViewModel,
    settingsViewModel: SettingsViewModel
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Dashboard.route
    ) {
        composable(Screen.Dashboard.route) {
            DashboardScreen(
                viewModel = salahViewModel,
                onNavigateToQuran = { navController.navigate(Screen.QuranHub.route) },
                onNavigateToInsights = { navController.navigate(Screen.Insights.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
            )
        }

        composable(Screen.Insights.route) {
            InsightsScreen(
                viewModel = salahViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.QuranHub.route) {
            QuranHubScreen(
                viewModel = quranViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                viewModel = settingsViewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
