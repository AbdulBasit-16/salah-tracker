package com.salah.tracker

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import com.salah.tracker.data.database.AppDatabase
import com.salah.tracker.data.repository.SalahRepositoryImpl
import com.salah.tracker.ui.navigation.AppNavigation
import com.salah.tracker.ui.theme.SalahTrackerTheme
import com.salah.tracker.viewmodel.QuranViewModel
import com.salah.tracker.viewmodel.SalahViewModel
import com.salah.tracker.viewmodel.SettingsViewModel
import com.salah.tracker.viewmodel.ViewModelFactory

class MainActivity : ComponentActivity() {

    // Request notification permission launcher for Android 13+
    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Permission granted, reschedule alarms to set up channel
            com.salah.tracker.services.AlarmReceiver.rescheduleAlarms(applicationContext)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Database & Repository
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = SalahRepositoryImpl(
            preferencesDao = database.userPreferencesDao(),
            prayerLogDao = database.prayerLogDao(),
            qazaCounterDao = database.qazaCounterDao(),
            quranLogDao = database.quranLogDao()
        )
        val factory = ViewModelFactory(repository, applicationContext)

        // ViewModels
        val salahViewModel: SalahViewModel by viewModels { factory }
        val quranViewModel: QuranViewModel by viewModels { factory }
        val settingsViewModel: SettingsViewModel by viewModels { factory }

        // Reschedule alarms on startup
        com.salah.tracker.services.AlarmReceiver.rescheduleAlarms(applicationContext)

        // Request notifications permission if Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            val prefs by salahViewModel.userPreferences.collectAsState()
            val themeName = prefs?.themeName ?: "FOREST_GREEN"

            SalahTrackerTheme(themeName = themeName) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(
                        salahViewModel = salahViewModel,
                        quranViewModel = quranViewModel,
                        settingsViewModel = settingsViewModel
                    )
                }
            }
        }
    }
}
