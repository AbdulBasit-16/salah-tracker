package com.salah.tracker.ui.settings

import android.Manifest
import android.content.Context
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

    // Coordinates inputs
    var latText by remember { mutableStateOf("") }
    var lngText by remember { mutableStateOf("") }
    var tzText by remember { mutableStateOf("") }

    // Sync input states with preferences on load
    LaunchedEffect(prefs) {
        latText = prefs.latitude.toString()
        lngText = prefs.longitude.toString()
        tzText = prefs.timezoneOffset.toString()
    }

    // Permission launcher for location detection
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fineGranted || coarseGranted) {
            detectLocation(context) { lat, lng, tz ->
                viewModel.updateCoordinates(lat, lng, tz)
                Toast.makeText(context, "Location updated successfully!", Toast.LENGTH_SHORT).show()
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
                "Location & Coordinates",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            )

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = latText,
                            onValueChange = { latText = it },
                            label = { Text("Latitude") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = lngText,
                            onValueChange = { lngText = it },
                            label = { Text("Longitude") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = tzText,
                        onValueChange = { tzText = it },
                        label = { Text("Timezone Offset (Hours)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val lat = latText.toDoubleOrNull()
                                val lng = lngText.toDoubleOrNull()
                                val tz = tzText.toDoubleOrNull()
                                if (lat != null && lng != null && tz != null) {
                                    viewModel.updateCoordinates(lat, lng, tz)
                                    Toast.makeText(context, "Coordinates saved!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Please enter valid numbers.", Toast.LENGTH_SHORT).show()
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Save Location")
                        }

                        OutlinedButton(
                            onClick = {
                                locationPermissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Auto-Detect")
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
                        val methods = listOf("MWL", "ISNA", "EGYPT", "KARACHI", "UMM_AL_QURA", "GULF", "TEHRAN")
                        
                        var expandedMethod by remember { mutableStateOf(false) }
                        Box {
                            OutlinedButton(
                                onClick = { expandedMethod = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(prefs.calculationMethod)
                            }
                            DropdownMenu(
                                expanded = expandedMethod,
                                onDismissRequest = { expandedMethod = false }
                            ) {
                                methods.forEach { m ->
                                    DropdownMenuItem(
                                        text = { Text(m) },
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

private fun detectLocation(context: Context, onLocationDetected: (Double, Double, Double) -> Unit) {
    try {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        
        // Find last known location
        val location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            
        if (location != null) {
            val tzOffset = TimeZone.getDefault().rawOffset / 3600000.0
            onLocationDetected(location.latitude, location.longitude, tzOffset)
        } else {
            // Default fallback if no last known location is saved on device
            Toast.makeText(context, "Unable to get GPS lock. Setting default (Makkah).", Toast.LENGTH_LONG).show()
            val tzOffset = TimeZone.getDefault().rawOffset / 3600000.0
            onLocationDetected(21.4225, 39.8262, tzOffset)
        }
    } catch (e: SecurityException) {
        Toast.makeText(context, "Location permission missing.", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Location detection failed: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}
