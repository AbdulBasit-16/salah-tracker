package com.salah.tracker.ui.settings

import android.Manifest
import android.content.Context
import com.salah.tracker.ui.detectLocation
import android.location.LocationManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.salah.tracker.data.database.entities.UserPreferences
import com.salah.tracker.viewmodel.SettingsViewModel
import com.salah.tracker.data.model.PakistanCities
import com.salah.tracker.data.model.PakistanCity
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val prefsOrNull by viewModel.preferences.collectAsState()
    val prefs = prefsOrNull ?: UserPreferences()
    val context = LocalContext.current

    // Permission launcher for location detection
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fineGranted || coarseGranted) {
            detectLocation(context) { lat, lng, tz, cityName ->
                viewModel.updateSelectedCity(cityName, lat, lng, tz)
                Toast.makeText(context, "Location updated: $cityName", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Location permission denied.", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Settings",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Location Settings Card
            Text(
                "Location Settings",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            )

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = if (prefs.selectedCity.isEmpty()) "Location: Unknown" else "Location: ${prefs.selectedCity}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = String.format("Coordinates: %.4f, %.4f", prefs.latitude, prefs.longitude),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Text(
                            text = String.format("Timezone Offset: GMT%+.1f", prefs.timezoneOffset),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }

                    Button(
                        onClick = {
                            locationPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Auto-Detect Location")
                    }
                }
            }

            // Theme Configuration Card
            Text(
                "App Appearance Theme",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            )

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Theme Palette", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                    val themes = listOf(
                        "FOREST_GREEN" to "Forest Green (Serene)",
                        "DEEP_BLUE" to "Deep Ocean Blue (Calm)",
                        "OLIVE_GOLD" to "Olive Gold (Warm)",
                        "ROYAL_PURPLE" to "Royal Purple (Noble)",
                        "SLATE_ROSE" to "Slate Rose (Tranquil)",
                        "ROSE_GOLD" to "Rose Gold (Luxury)",
                        "TEAL_MINT" to "Teal Mint (Fresh)",
                        "MIDNIGHT_SUNSET" to "Midnight Sunset (Vibrant)",
                        "OCEAN_BREEZE" to "Ocean Breeze (Soothing)",
                        "WARM_TERRACOTTA" to "Warm Terracotta (Cozy)",
                        "NIGHT_NEON" to "Night Neon (Cyberpunk)"
                    )
                    
                    var expandedTheme by remember { mutableStateOf(false) }
                    Box {
                        OutlinedButton(
                            onClick = { expandedTheme = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(themes.firstOrNull { it.first == prefs.themeName }?.second ?: "Forest Green (Serene)")
                        }
                        DropdownMenu(
                            expanded = expandedTheme,
                            onDismissRequest = { expandedTheme = false }
                        ) {
                            themes.forEach { (key, label) ->
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = {
                                        viewModel.updateThemeName(key)
                                        expandedTheme = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Quran Recitation Settings Card
            Text(
                "Quran Recitation Settings",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            )

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Script selector
                    Column {
                        Text("Arabic Script Style", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                        Spacer(Modifier.height(4.dp))
                        val scripts = listOf("UTHMANI" to "Uthmani Script (Global)", "INDOPAK" to "Indo-Pak Script (South Asia)")
                        
                        var expandedScript by remember { mutableStateOf(false) }
                        Box {
                            OutlinedButton(
                                onClick = { expandedScript = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(scripts.firstOrNull { it.first == prefs.quranScript }?.second ?: "Uthmani Script (Global)")
                            }
                            DropdownMenu(
                                expanded = expandedScript,
                                onDismissRequest = { expandedScript = false }
                            ) {
                                scripts.forEach { (key, label) ->
                                    DropdownMenuItem(
                                        text = { Text(label) },
                                        onClick = {
                                            viewModel.updateQuranPreferences(key, prefs.showEnglishTranslation, prefs.showUrduTranslation)
                                            expandedScript = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Divider(color = MaterialTheme.colorScheme.background, thickness = 1.dp)

                    // Translation Toggles
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Translations", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.updateQuranPreferences(
                                        prefs.quranScript,
                                        !prefs.showEnglishTranslation,
                                        prefs.showUrduTranslation
                                    )
                                }
                                .padding(vertical = 8.dp)
                        ) {
                            Checkbox(
                                checked = prefs.showEnglishTranslation,
                                onCheckedChange = {
                                    viewModel.updateQuranPreferences(
                                        prefs.quranScript,
                                        it,
                                        prefs.showUrduTranslation
                                    )
                                }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("English Translation (Sahih International)", style = MaterialTheme.typography.bodyLarge)
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.updateQuranPreferences(
                                        prefs.quranScript,
                                        prefs.showEnglishTranslation,
                                        !prefs.showUrduTranslation
                                    )
                                }
                                .padding(vertical = 8.dp)
                        ) {
                            Checkbox(
                                checked = prefs.showUrduTranslation,
                                onCheckedChange = {
                                    viewModel.updateQuranPreferences(
                                        prefs.quranScript,
                                        prefs.showEnglishTranslation,
                                        it
                                    )
                                }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Urdu Translation (Jalandhari)", style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }

            // Calculation parameters Card
            Text(
                "Calculation Configuration",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            )

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Method selector
                    Column {
                        Text("Calculation Method", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                        Spacer(Modifier.height(4.dp))
                        val methodLabels = mapOf(
                            "MWL" to "Muslim World League (MWL)",
                            "ISNA" to "Islamic Society of North America (ISNA)",
                            "EGYPT" to "Egyptian General Authority of Survey",
                            "KARACHI" to "University of Islamic Sciences, Karachi",
                            "UMM_AL_QURA" to "Umm Al-Qura University, Makkah",
                            "GULF" to "Gulf Region",
                            "TEHRAN" to "Institute of Geophysics, University of Tehran"
                        )
                        val methods = listOf("MWL", "ISNA", "EGYPT", "KARACHI", "UMM_AL_QURA", "GULF", "TEHRAN")
                        
                        var expandedMethod by remember { mutableStateOf(false) }
                        Box {
                            OutlinedButton(
                                onClick = { expandedMethod = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(methodLabels[prefs.calculationMethod] ?: prefs.calculationMethod)
                            }
                            DropdownMenu(
                                expanded = expandedMethod,
                                onDismissRequest = { expandedMethod = false }
                            ) {
                                methods.forEach { m ->
                                    DropdownMenuItem(
                                        text = { Text(methodLabels[m] ?: m) },
                                        onClick = {
                                            viewModel.updateCalculationMethod(m)
                                            expandedMethod = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Juristic Method selector
                    Column {
                        Text("Asr Juristic Method", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                        Spacer(Modifier.height(4.dp))
                        val juristics = listOf("STANDARD" to "Standard (Shafi'i, Maliki, Hanbali)", "HANAFI" to "Hanafi")
                        
                        var expandedJuristic by remember { mutableStateOf(false) }
                        Box {
                            OutlinedButton(
                                onClick = { expandedJuristic = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(if (prefs.juristicMethod == "HANAFI") "Hanafi" else "Standard (Shafi'i, etc.)")
                            }
                            DropdownMenu(
                                expanded = expandedJuristic,
                                onDismissRequest = { expandedJuristic = false }
                            ) {
                                juristics.forEach { (key, label) ->
                                    DropdownMenuItem(
                                        text = { Text(label) },
                                        onClick = {
                                            viewModel.updateJuristicMethod(key)
                                            expandedJuristic = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Notification Toggles Card
            Text(
                "Notifications & Reminders",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            )

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(8.dp)
                ) {
                    ToggleSettingRow(
                        label = "Fajr Notification",
                        checked = prefs.fajrNotifEnabled,
                        onCheckedChange = { viewModel.updateNotificationToggles(it, prefs.dhuhrNotifEnabled, prefs.asrNotifEnabled, prefs.maghribNotifEnabled, prefs.ishaNotifEnabled, prefs.missedPrayerRemindersEnabled, prefs.postSalahRecitationEnabled) }
                    )
                    ToggleSettingRow(
                        label = "Dhuhr Notification",
                        checked = prefs.dhuhrNotifEnabled,
                        onCheckedChange = { viewModel.updateNotificationToggles(prefs.fajrNotifEnabled, it, prefs.asrNotifEnabled, prefs.maghribNotifEnabled, prefs.ishaNotifEnabled, prefs.missedPrayerRemindersEnabled, prefs.postSalahRecitationEnabled) }
                    )
                    ToggleSettingRow(
                        label = "Asr Notification",
                        checked = prefs.asrNotifEnabled,
                        onCheckedChange = { viewModel.updateNotificationToggles(prefs.fajrNotifEnabled, prefs.dhuhrNotifEnabled, it, prefs.maghribNotifEnabled, prefs.ishaNotifEnabled, prefs.missedPrayerRemindersEnabled, prefs.postSalahRecitationEnabled) }
                    )
                    ToggleSettingRow(
                        label = "Maghrib Notification",
                        checked = prefs.maghribNotifEnabled,
                        onCheckedChange = { viewModel.updateNotificationToggles(prefs.fajrNotifEnabled, prefs.dhuhrNotifEnabled, prefs.asrNotifEnabled, it, prefs.ishaNotifEnabled, prefs.missedPrayerRemindersEnabled, prefs.postSalahRecitationEnabled) }
                    )
                    ToggleSettingRow(
                        label = "Isha Notification",
                        checked = prefs.ishaNotifEnabled,
                        onCheckedChange = { viewModel.updateNotificationToggles(prefs.fajrNotifEnabled, prefs.dhuhrNotifEnabled, prefs.asrNotifEnabled, prefs.maghribNotifEnabled, it, prefs.missedPrayerRemindersEnabled, prefs.postSalahRecitationEnabled) }
                    )
                    Divider(color = MaterialTheme.colorScheme.background, thickness = 1.dp, modifier = Modifier.padding(horizontal = 8.dp))
                    ToggleSettingRow(
                        label = "Missed Prayer Qaza Reminders",
                        checked = prefs.missedPrayerRemindersEnabled,
                        onCheckedChange = { viewModel.updateNotificationToggles(prefs.fajrNotifEnabled, prefs.dhuhrNotifEnabled, prefs.asrNotifEnabled, prefs.maghribNotifEnabled, prefs.ishaNotifEnabled, it, prefs.postSalahRecitationEnabled) }
                    )
                    ToggleSettingRow(
                        label = "Post-Salah Recitation Prompt",
                        checked = prefs.postSalahRecitationEnabled,
                        onCheckedChange = { viewModel.updateNotificationToggles(prefs.fajrNotifEnabled, prefs.dhuhrNotifEnabled, prefs.asrNotifEnabled, prefs.maghribNotifEnabled, prefs.ishaNotifEnabled, prefs.missedPrayerRemindersEnabled, it) }
                    )
                }
            }

            // Islamic Calendar Settings Card
            Text(
                "Islamic Calendar Settings",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            )

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Hijri Adjustment (Days)", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                    
                    val adjustments = listOf(
                        -2 to "-2 Days",
                        -1 to "-1 Day",
                        0 to "Standard (0)",
                        1 to "+1 Day",
                        2 to "+2 Days"
                    )
                    
                    var expandedHijri by remember { mutableStateOf(false) }
                    Box {
                        OutlinedButton(
                            onClick = { expandedHijri = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            val currentText = adjustments.firstOrNull { it.first == prefs.hijriAdjustment }?.second ?: "Standard (0)"
                            Text(currentText)
                        }
                        DropdownMenu(
                            expanded = expandedHijri,
                            onDismissRequest = { expandedHijri = false }
                        ) {
                            adjustments.forEach { (adj, label) ->
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = {
                                        viewModel.updateHijriAdjustment(adj)
                                        expandedHijri = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ToggleSettingRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 12.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}
