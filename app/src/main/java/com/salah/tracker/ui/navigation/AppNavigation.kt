package com.salah.tracker.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.salah.tracker.ui.dashboard.DashboardScreen
import com.salah.tracker.ui.insights.InsightsScreen
import com.salah.tracker.ui.quran.QuranHubScreen
import com.salah.tracker.ui.settings.SettingsScreen
import com.salah.tracker.ui.qibla.QiblaScreen
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
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val navigationItems = listOf(
        Triple(Screen.Dashboard.route, Icons.Default.Schedule, "Dashboard"),
        Triple(Screen.QuranHub.route, Icons.Default.MenuBook, "Quran"),
        Triple(Screen.Insights.route, Icons.Default.Alarm, "Insights"),
        Triple(Screen.Settings.route, Icons.Default.Language, "Settings"),
        Triple(Screen.Qibla.route, Icons.Default.Explore, "Qibla")
    )

    Scaffold(
        bottomBar = {
            val showBottomBar = currentRoute in navigationItems.map { it.first }
            if (showBottomBar) {
                NavigationBar(
                    containerColor = Color(0xFF121212),
                    tonalElevation = 0.dp
                ) {
                    navigationItems.forEach { (route, icon, label) ->
                        NavigationBarItem(
                            selected = currentRoute == route,
                            onClick = {
                                if (currentRoute != route) {
                                    navController.navigate(route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = { Icon(icon, contentDescription = label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color(0xFF3A9AD9),
                                unselectedIconColor = Color.White.copy(alpha = 0.5f),
                                indicatorColor = Color.Transparent
                            )
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(paddingValues)
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

            composable(Screen.Qibla.route) {
                QiblaScreen(
                    viewModel = salahViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
