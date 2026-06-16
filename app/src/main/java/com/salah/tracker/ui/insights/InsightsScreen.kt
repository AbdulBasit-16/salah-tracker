package com.salah.tracker.ui.insights

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.salah.tracker.data.database.entities.PrayerLog
import com.salah.tracker.data.database.entities.QazaCounter
import com.salah.tracker.ui.theme.ExcusedBlue
import com.salah.tracker.ui.theme.MissedGray
import com.salah.tracker.viewmodel.SalahViewModel
import kotlinx.coroutines.flow.map
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsScreen(
    viewModel: SalahViewModel,
    onBack: () -> Unit
) {
    val qazaBalances by viewModel.qazaBalances.collectAsState()
    val todayLogs by viewModel.todayPrayerLogs.collectAsState()

    // Calculate completion metrics
    val totalObligatory = todayLogs.filter { it.prayerName in listOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha") }
    val offeredCount = totalObligatory.count { it.status.startsWith("Offered") }
    val missedCount = totalObligatory.count { it.status == "Missed/Qaza" }
    val excusedCount = totalObligatory.count { it.status == "Excused" }
    val pendingCount = totalObligatory.count { it.status == "Pending" }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Insights & Qaza",
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section 1: Today's Summary Chart
            Text(
                "Today's Progress",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            )

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // Simple Donut Chart drawn in Canvas
                    val primaryColor = MaterialTheme.colorScheme.primary
                    val secondaryColor = MaterialTheme.colorScheme.secondary
                    val surfaceColor = MaterialTheme.colorScheme.background

                    Box(
                        modifier = Modifier.size(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val strokeWidth = 10.dp.toPx()
                            
                            // Background track
                            drawCircle(
                                color = surfaceColor,
                                radius = size.minDimension / 2 - strokeWidth / 2,
                                style = Stroke(width = strokeWidth)
                            )
                            
                            // Calculate sweep angles
                            val total = 5.0
                            if (total > 0) {
                                val sweepOffered = (offeredCount / total) * 360.0
                                val sweepMissed = (missedCount / total) * 360.0
                                val sweepExcused = (excusedCount / total) * 360.0
                                
                                var startAngle = -90.0
                                
                                // Offered on-time/late (Green)
                                if (sweepOffered > 0) {
                                    drawArc(
                                        color = primaryColor,
                                        startAngle = startAngle.toFloat(),
                                        sweepAngle = sweepOffered.toFloat(),
                                        useCenter = false,
                                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                                    )
                                    startAngle += sweepOffered
                                }
                                
                                // Excused (Blue)
                                if (sweepExcused > 0) {
                                    drawArc(
                                        color = ExcusedBlue,
                                        startAngle = startAngle.toFloat(),
                                        sweepAngle = sweepExcused.toFloat(),
                                        useCenter = false,
                                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                                    )
                                    startAngle += sweepExcused
                                }

                                // Missed (Grey, not red to avoid stress)
                                if (sweepMissed > 0) {
                                    drawArc(
                                        color = MissedGray,
                                        startAngle = startAngle.toFloat(),
                                        sweepAngle = sweepMissed.toFloat(),
                                        useCenter = false,
                                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                                    )
                                }
                            }
                        }

                        // Percentage text in center
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${((offeredCount + excusedCount) / 5.0 * 100).toInt()}%",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Done",
                                style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                            )
                        }
                    }

                    // Legend Column
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        LegendItem(color = primaryColor, label = "Offered: $offeredCount")
                        LegendItem(color = ExcusedBlue, label = "Excused: $excusedCount")
                        LegendItem(color = MissedGray, label = "Missed: $missedCount")
                        LegendItem(color = MaterialTheme.colorScheme.background, label = "Pending: $pendingCount")
                    }
                }
            }

            // Section 2: Habit Streaks
            Text(
                "Habit Streak",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            )

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "You are doing great! Keep up the consistency.",
                        style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f))
                    )
                    Spacer(Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        // We will display a peaceful visual representing daily streak
                        for (i in 0..4) {
                            val date = LocalDate.now().minusDays(i.toLong())
                            val dayLabel = date.dayOfWeek.name.take(3)
                            
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Surface(
                                    modifier = Modifier.size(36.dp),
                                    shape = RoundedCornerShape(18.dp),
                                    color = if (i < 3) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.background
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            tint = if (i < 3) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    dayLabel,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    }
                }
            }

            // Section 3: Qaza Balance Editor
            Text(
                "Qaza Balances",
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
                    qazaBalances.forEachIndexed { index, counter ->
                        QazaEditorRow(
                            counter = counter,
                            onIncrement = { viewModel.incrementQazaBalance(counter.prayerName) },
                            onDecrement = { viewModel.offerQazaMakeup(counter.prayerName) }
                        )
                        if (index < qazaBalances.lastIndex) {
                            Divider(
                                color = MaterialTheme.colorScheme.background,
                                thickness = 1.dp,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LegendItem(color: androidx.compose.ui.graphics.Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(
            modifier = Modifier.size(12.dp),
            shape = RoundedCornerShape(3.dp),
            color = color
        ) {}
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
        )
    }
}

@Composable
fun QazaEditorRow(
    counter: QazaCounter,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp, horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                counter.prayerName,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            )
            Text(
                "${counter.count} prayers owed",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = if (counter.count > 0) MissedGray else MaterialTheme.colorScheme.primary,
                    fontWeight = if (counter.count > 0) FontWeight.SemiBold else FontWeight.Normal
                )
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Decrement (makeup completed)
            IconButton(
                onClick = onDecrement,
                enabled = counter.count > 0,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Decrease Qaza balance")
            }

            Text(
                text = "${counter.count}",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.widthIn(min = 24.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            // Increment
            IconButton(
                onClick = onIncrement,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(Icons.Default.Add, contentDescription = "Increase Qaza balance")
            }
        }
    }
}
