package com.salah.tracker.ui.dashboard

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.salah.tracker.data.database.entities.PrayerLog
import androidx.compose.ui.platform.LocalContext
import com.salah.tracker.ui.theme.ExcusedBlue
import com.salah.tracker.ui.theme.MissedGray
import com.salah.tracker.viewmodel.SalahViewModel
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: SalahViewModel,
    onNavigateToQuran: () -> Unit,
    onNavigateToInsights: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val currentDate by viewModel.currentDate.collectAsState()
    val todayLogs by viewModel.todayPrayerLogs.collectAsState()
    val todayTimes by viewModel.todayPrayerTimes.collectAsState()
    val countdownState by viewModel.countdownState.collectAsState()

    var showStatusDialogForLog by remember { mutableStateOf<PrayerLog?>(null) }

    val dateFormater = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Salah Tracker",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Date Header
            Text(
                text = currentDate.format(dateFormater),
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Medium
                )
            )

            // Countdown Card
            countdownState?.let { (prayerName, timeLeft) ->
                CountdownCard(prayerName = prayerName, timeLeft = timeLeft)
            } ?: Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }

            // Daily Checklist Obligatory Prayers Section
            Text(
                "Obligatory Prayers",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.padding(top = 8.dp)
            )

            val obligatoryPrayers = listOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha")
            val obligatoryLogs = todayLogs.filter { it.prayerName in obligatoryPrayers }

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(8.dp)
                ) {
                    obligatoryLogs.forEachIndexed { index, log ->
                        val time = when (log.prayerName) {
                            "Fajr" -> todayTimes?.fajr
                            "Dhuhr" -> todayTimes?.dhuhr
                            "Asr" -> todayTimes?.asr
                            "Maghrib" -> todayTimes?.maghrib
                            "Isha" -> todayTimes?.isha
                            else -> null
                        }
                        PrayerCheckRow(
                            log = log,
                            time = time,
                            onClick = { showStatusDialogForLog = log }
                        )
                        if (index < obligatoryLogs.lastIndex) {
                            Divider(
                                color = MaterialTheme.colorScheme.background,
                                thickness = 1.dp,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                        }
                    }
                }
            }

            // Optional Prayers (Tahajjud, Sunnah, Nafl)
            Text(
                "Optional Prayers",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            )

            val optionalPrayers = listOf("Tahajjud", "Sunnah", "Nafl")
            val optionalLogs = todayLogs.filter { it.prayerName in optionalPrayers }

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(8.dp)
                ) {
                    optionalLogs.forEachIndexed { index, log ->
                        PrayerCheckRow(
                            log = log,
                            time = null,
                            onClick = { showStatusDialogForLog = log }
                        )
                        if (index < optionalLogs.lastIndex) {
                            Divider(
                                color = MaterialTheme.colorScheme.background,
                                thickness = 1.dp,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                        }
                    }
                }
            }

            // Quick Actions Card
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = onNavigateToQuran,
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Quran Log", style = MaterialTheme.typography.labelLarge)
                }

                OutlinedButton(
                    onClick = onNavigateToInsights,
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = ButtonDefaults.outlinedButtonBorder.copy()
                ) {
                    Icon(Icons.Default.BarChart, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Insights", style = MaterialTheme.typography.labelLarge)
                }
            }
        }

        // Status Selector Dialog
        showStatusDialogForLog?.let { log ->
            StatusSelectorDialog(
                log = log,
                onDismiss = { showStatusDialogForLog = null },
                onSelectStatus = { status ->
                    viewModel.updatePrayerStatus(log, status)
                    showStatusDialogForLog = null
                }
            )
        }
    }
}

@Composable
fun CountdownCard(prayerName: String, timeLeft: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Time until $prayerName",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Medium
                )
            )
            Spacer(Modifier.height(8.dp))
            Text(
                timeLeft,
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 42.sp,
                    color = MaterialTheme.colorScheme.onPrimary,
                    letterSpacing = 1.sp
                )
            )
        }
    }
}

@Composable
fun PrayerCheckRow(
    log: PrayerLog,
    time: LocalTime?,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val timeFormatter = DateTimeFormatter.ofPattern("hh:mm a")
    val timeStr = time?.format(timeFormatter) ?: ""

    val isObligatory = log.prayerName in listOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha")
    val isLocked = isObligatory && time != null && LocalTime.now().isBefore(time)

    // Micro animation on press feedback
    var clicked by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (clicked) 0.96f else 1.0f, label = "click_scale")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable { 
                if (isLocked) {
                    android.widget.Toast.makeText(
                        context,
                        "Marking is only available after ${log.prayerName} time begins.",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                } else {
                    clicked = true
                    onClick()
                    clicked = false
                }
            }
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                log.prayerName,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (isLocked) MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onBackground
                )
            )
            if (timeStr.isNotEmpty()) {
                Text(
                    timeStr,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = if (isLocked) 0.3f else 0.6f)
                    )
                )
            }
        }

        Spacer(Modifier.width(16.dp))

        // Status badge
        val (badgeColor, textColor, textVal, icon) = if (isLocked) {
            Quadruple(
                MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f),
                MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                "Upcoming",
                Icons.Default.Lock
            )
        } else {
            when (log.status) {
                "Offered On-Time" -> Quadruple(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    MaterialTheme.colorScheme.primary,
                    "On-Time",
                    Icons.Default.DoneAll
                )
                "Offered Late" -> Quadruple(
                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                    MaterialTheme.colorScheme.secondary,
                    "Late",
                    Icons.Default.Done
                )
                "Missed/Qaza" -> Quadruple(
                    MissedGray.copy(alpha = 0.15f),
                    MissedGray,
                    "Qaza",
                    Icons.Default.Close
                )
                "Excused" -> Quadruple(
                    ExcusedBlue.copy(alpha = 0.15f),
                    ExcusedBlue,
                    "Excused",
                    Icons.Default.Block
                )
                else -> Quadruple(
                    MaterialTheme.colorScheme.background,
                    MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                    "Pending",
                    Icons.Default.AccessTime
                )
            }
        }

        Surface(
            shape = RoundedCornerShape(8.dp),
            color = badgeColor,
            modifier = Modifier.height(36.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(icon, contentDescription = null, tint = textColor, modifier = Modifier.size(16.dp))
                Text(
                    textVal,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                )
            }
        }
    }
}

@Composable
fun StatusSelectorDialog(
    log: PrayerLog,
    onDismiss: () -> Unit,
    onSelectStatus: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Update ${log.prayerName}",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Select your prayer offering status:")
                Spacer(Modifier.height(8.dp))
                
                val statuses = listOf(
                    "Offered On-Time" to "Offered on time",
                    "Offered Late" to "Offered late",
                    "Missed/Qaza" to "Missed (Increments Qaza)",
                    "Excused" to "Excused (e.g. Travel, Illness)",
                    "Pending" to "Mark Pending"
                )

                statuses.forEach { (statusVal, label) ->
                    val color = when (statusVal) {
                        "Offered On-Time" -> MaterialTheme.colorScheme.primary
                        "Offered Late" -> MaterialTheme.colorScheme.secondary
                        "Missed/Qaza" -> MissedGray
                        "Excused" -> ExcusedBlue
                        else -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    }

                    OutlinedButton(
                        onClick = { onSelectStatus(statusVal) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = color
                        )
                    ) {
                        Text(label, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MaterialTheme.colorScheme.primary)
            }
        }
    )
}

data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
