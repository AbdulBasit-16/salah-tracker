package com.salah.tracker.ui.dashboard

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.salah.tracker.data.database.entities.PrayerLog
import com.salah.tracker.services.PrayerCountdownService
import com.salah.tracker.ui.theme.ExcusedBlue
import com.salah.tracker.ui.theme.MissedGray
import com.salah.tracker.viewmodel.SalahViewModel
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: SalahViewModel,
    onNavigateToQuran: () -> Unit,
    onNavigateToInsights: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val context = LocalContext.current
    val currentDate by viewModel.currentDate.collectAsState()
    val todayLogs by viewModel.todayPrayerLogs.collectAsState()
    val todayTimes by viewModel.todayPrayerTimes.collectAsState()
    val countdownState by viewModel.countdownState.collectAsState()
    val prefs by viewModel.userPreferences.collectAsState()

    var showStatusDialogForLog by remember { mutableStateOf<PrayerLog?>(null) }
    var showFloatingWidget by remember { mutableStateOf(false) }

    val dateFormater = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy")
    val selectedCity = prefs?.selectedCity ?: "Makkah"
    val pair = getCurrentAndNextPrayer(todayTimes)

    // Start background countdown service
    LaunchedEffect(Unit) {
        PrayerCountdownService.startService(context)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                // Centered Location Header and controls
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            Icons.Default.Menu,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.Place,
                                contentDescription = null,
                                tint = Color(0xFF3A9AD9),
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = selectedCity,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            )
                        }
                        Text(
                            text = "Location detected automatically",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                            )
                        )
                    }

                    IconButton(onClick = { showFloatingWidget = !showFloatingWidget }) {
                        Icon(
                            Icons.Default.Widgets,
                            contentDescription = "Toggle Floating Widget",
                            tint = if (showFloatingWidget) Color(0xFF3A9AD9) else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Beautiful Custom Analog Clock
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CustomAnalogClock(
                        currentStart = pair?.first,
                        nextStart = pair?.second,
                        modifier = Modifier.size(240.dp)
                    )
                }

                // Countdown Text below the Clock
                countdownState?.let { (prayerName, timeLeft) ->
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = prayerName,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        )

                        // Next prayer start time
                        val nextTime = when (prayerName) {
                            "Fajr", "Fajr (Tomorrow)" -> todayTimes?.fajr
                            "Dhuhr" -> todayTimes?.dhuhr
                            "Asr" -> todayTimes?.asr
                            "Maghrib" -> todayTimes?.maghrib
                            "Isha" -> todayTimes?.isha
                            "Sunrise" -> todayTimes?.sunrise
                            else -> null
                        }
                        nextTime?.let {
                            val timeFormatter = DateTimeFormatter.ofPattern("hh:mm a")
                            Text(
                                text = it.format(timeFormatter),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                                )
                            )
                        }

                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "- $timeLeft",
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                fontSize = 34.sp,
                                color = MaterialTheme.colorScheme.onBackground,
                                letterSpacing = 1.sp
                            )
                        )
                    }
                }

                // Date Label
                Text(
                    text = currentDate.format(dateFormater),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Medium
                    ),
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

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

        // In-App Draggable Floating Countdown Widget
        if (showFloatingWidget) {
            var offsetX by remember { mutableStateOf(100f) }
            var offsetY by remember { mutableStateOf(100f) }

            Box(
                modifier = Modifier
                    .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            offsetX += dragAmount.x
                            offsetY += dragAmount.y
                        }
                    }
                    .size(width = 160.dp, height = 75.dp)
            ) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xEE1A1A1A)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF3A9AD9))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = countdownState?.first ?: "Next Salah",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        )
                        Text(
                            text = countdownState?.second ?: "--:--:--",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF3A9AD9)
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CustomAnalogClock(
    currentStart: LocalTime?,
    nextStart: LocalTime?,
    modifier: Modifier = Modifier
) {
    var time by remember { mutableStateOf(LocalTime.now()) }

    LaunchedEffect(Unit) {
        while (true) {
            time = LocalTime.now()
            kotlinx.coroutines.delay(1000)
        }
    }

    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2, size.height / 2)
        val radius = size.width / 2

        // Outer dark background circle
        drawCircle(
            color = Color(0xFF181818),
            radius = radius,
            center = center
        )

        // Outer progress arc showing elapsed prayer duration
        if (currentStart != null) {
            val startHour = currentStart.hour + currentStart.minute / 60.0 + currentStart.second / 3600.0
            val nowHour = time.hour + time.minute / 60.0 + time.second / 3600.0

            val startAngle = (startHour * 30.0 - 90.0).toFloat()
            val nowAngle = (nowHour * 30.0 - 90.0).toFloat()
            var sweepAngle = nowAngle - startAngle
            if (sweepAngle < 0) sweepAngle += 360f

            drawArc(
                color = Color(0xFF3A9AD9),
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(width = 6.dp.toPx()),
                size = size,
                topLeft = Offset.Zero
            )
        }

        // Inner dial circle
        drawCircle(
            color = Color(0xFF2C2C2C),
            radius = radius * 0.82f,
            center = center
        )

        // Draw numbers
        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 28f
            textAlign = android.graphics.Paint.Align.CENTER
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        }

        for (i in 1..12) {
            val angleDeg = i * 30.0 - 90.0
            val angleRad = Math.toRadians(angleDeg)
            val tx = center.x + (radius * 0.68f) * cos(angleRad).toFloat()
            val ty = center.y + (radius * 0.68f) * sin(angleRad).toFloat() + 10f
            drawContext.canvas.nativeCanvas.drawText(i.toString(), tx, ty, paint)
        }

        // Hand lengths
        val hourHandLength = radius * 0.45f
        val minuteHandLength = radius * 0.65f
        val secondHandLength = radius * 0.72f

        val hourAngle = ((time.hour % 12 + time.minute / 60.0) * 30.0 - 90.0).toFloat()
        val minuteAngle = (time.minute * 6.0 - 90.0).toFloat()
        val secondAngle = (time.second * 6.0 - 90.0).toFloat()

        // Hour hand
        rotate(degrees = hourAngle, pivot = center) {
            drawLine(
                color = Color.White,
                start = center,
                end = Offset(center.x + hourHandLength, center.y),
                strokeWidth = 6.dp.toPx()
            )
        }

        // Minute hand
        rotate(degrees = minuteAngle, pivot = center) {
            drawLine(
                color = Color(0xFF3A9AD9),
                start = center,
                end = Offset(center.x + minuteHandLength, center.y),
                strokeWidth = 4.dp.toPx()
            )
        }

        // Second hand
        rotate(degrees = secondAngle, pivot = center) {
            drawLine(
                color = Color.White.copy(alpha = 0.8f),
                start = center,
                end = Offset(center.x + secondHandLength, center.y),
                strokeWidth = 2.dp.toPx()
            )
        }

        // Pivot point
        drawCircle(
            color = Color.White,
            radius = 6.dp.toPx(),
            center = center
        )
    }
}

private fun getCurrentAndNextPrayer(times: com.salah.tracker.data.calculator.PrayerTimeCalculator.PrayerTimes?): Pair<LocalTime, LocalTime>? {
    if (times == null) return null
    val now = LocalTime.now()
    val list = listOf(
        times.fajr,
        times.sunrise,
        times.dhuhr,
        times.asr,
        times.maghrib,
        times.isha
    )

    var currentIdx = -1
    for (i in list.indices) {
        if (!now.isBefore(list[i])) {
            currentIdx = i
        }
    }
    return if (currentIdx != -1) {
        val current = list[currentIdx]
        val next = if (currentIdx < list.lastIndex) list[currentIdx + 1] else list[0]
        Pair(current, next)
    } else {
        Pair(list.last(), list.first())
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
