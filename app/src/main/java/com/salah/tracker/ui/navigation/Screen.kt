package com.salah.tracker.ui.navigation

sealed class Screen(val route: String) {
    object Dashboard : Screen("dashboard")
    object Insights : Screen("insights")
    object QuranHub : Screen("quran")
    object Settings : Screen("settings")
}
