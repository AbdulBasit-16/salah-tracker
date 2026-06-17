package com.salah.tracker.ui.qibla

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CompassCalibration
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.salah.tracker.viewmodel.SalahViewModel
import kotlin.math.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QiblaScreen(
    viewModel: SalahViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val prefsOrNull by viewModel.userPreferences.collectAsState()
    val prefs = prefsOrNull ?: com.salah.tracker.data.database.entities.UserPreferences()

    val lat = prefs.latitude
    val lng = prefs.longitude

    // Compute Qibla details
    val qiblaAngle = remember(lat, lng) { calculateQibla(lat, lng) }
    val distance = remember(lat, lng) { calculateDistanceToKaaba(lat, lng) }

    // Sensor state
    var heading by remember { mutableStateOf(0f) }
    val sensorManager = remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }

    // Vibration state
    var hasVibrated by remember { mutableStateOf(false) }

    val isAligned = abs(heading - qiblaAngle.toFloat()) < 3f || abs(heading - qiblaAngle.toFloat() + 360f) < 3f || abs(heading - qiblaAngle.toFloat() - 360f) < 3f

    // Trigger vibration once when entering alignment
    LaunchedEffect(isAligned) {
        if (isAligned && !hasVibrated) {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            vibrator?.let {
                if (it.hasVibrator()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        it.vibrate(VibrationEffect.createOneShot(150, VibrationEffect.DEFAULT_AMPLITUDE))
                    } else {
                        @Suppress("DEPRECATION")
                        it.vibrate(150)
                    }
                }
            }
            hasVibrated = true
        } else if (!isAligned) {
            hasVibrated = false
        }
    }

    // Register sensor listener
    DisposableEffect(Unit) {
        val listener = object : SensorEventListener {
            private val gravity = FloatArray(3)
            private val geomagnetic = FloatArray(3)
            private val R = FloatArray(9)
            private val I = FloatArray(9)
            private val orientation = FloatArray(3)

            override fun onSensorChanged(event: SensorEvent) {
                if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                    System.arraycopy(event.values, 0, gravity, 0, event.values.size)
                }
                if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
                    System.arraycopy(event.values, 0, geomagnetic, 0, event.values.size)
                }
                if (SensorManager.getRotationMatrix(R, I, gravity, geomagnetic)) {
                    SensorManager.getOrientation(R, orientation)
                    val azimuthRad = orientation[0]
                    val azimuthDeg = Math.toDegrees(azimuthRad.toDouble()).toFloat()
                    heading = (azimuthDeg + 360f) % 360f
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        val accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val magnet = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        accel?.let { sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI) }
        magnet?.let { sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI) }

        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }

    val themeColor by animateColorAsState(
        targetValue = if (isAligned) Color(0xFF2ECC71) else Color(0xFF3A9AD9),
        label = "themeColor"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Qibla Compass",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
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
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Location metadata
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Current City: ${prefs.selectedCity}",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = String.format("Distance to Kaaba: %,.1f km", distance),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }

            // Compass drawing area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Canvas(
                    modifier = Modifier
                        .size(280.dp)
                        .padding(16.dp)
                ) {
                    val center = Offset(size.width / 2, size.height / 2)
                    val radius = size.width / 2

                    // 1. Draw outer static boundary circle
                    drawCircle(
                        color = themeColor.copy(alpha = 0.15f),
                        radius = radius,
                        center = center
                    )

                    // 2. Draw rotating compass rose
                    // The dial rotates counter-clockwise relative to device heading so N points North
                    rotate(degrees = -heading, pivot = center) {
                        // Draw North indicator line
                        drawLine(
                            color = Color.White,
                            start = center,
                            end = Offset(center.x, center.y - radius),
                            strokeWidth = 3.dp.toPx()
                        )
                        // Label N
                        drawContext.canvas.nativeCanvas.drawText(
                            "N",
                            center.x,
                            center.y - radius + 35f,
                            android.graphics.Paint().apply {
                                color = android.graphics.Color.WHITE
                                textSize = 40f
                                textAlign = android.graphics.Paint.Align.CENTER
                                typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                            }
                        )
                        // Label S
                        drawContext.canvas.nativeCanvas.drawText(
                            "S",
                            center.x,
                            center.y + radius - 15f,
                            android.graphics.Paint().apply {
                                color = android.graphics.Color.GRAY
                                textSize = 32f
                                textAlign = android.graphics.Paint.Align.CENTER
                            }
                        )
                        // Label E
                        drawContext.canvas.nativeCanvas.drawText(
                            "E",
                            center.x + radius - 25f,
                            center.y + 10f,
                            android.graphics.Paint().apply {
                                color = android.graphics.Color.GRAY
                                textSize = 32f
                                textAlign = android.graphics.Paint.Align.CENTER
                            }
                        )
                        // Label W
                        drawContext.canvas.nativeCanvas.drawText(
                            "W",
                            center.x - radius + 25f,
                            center.y + 10f,
                            android.graphics.Paint().apply {
                                color = android.graphics.Color.GRAY
                                textSize = 32f
                                textAlign = android.graphics.Paint.Align.CENTER
                            }
                        )

                        // 3. Draw Qibla pointer line & arrow
                        rotate(degrees = qiblaAngle.toFloat(), pivot = center) {
                            // Draw Qibla pointer line
                            drawLine(
                                color = themeColor,
                                start = center,
                                end = Offset(center.x, center.y - radius + 20f),
                                strokeWidth = 5.dp.toPx()
                            )
                            // Draw Arrowhead
                            val arrowPath = android.graphics.Path().apply {
                                moveTo(center.x, center.y - radius)
                                lineTo(center.x - 20f, center.y - radius + 40f)
                                lineTo(center.x + 20f, center.y - radius + 40f)
                                close()
                            }
                            drawContext.canvas.nativeCanvas.drawPath(
                                arrowPath,
                                android.graphics.Paint().apply {
                                    color = if (isAligned) android.graphics.Color.GREEN else android.graphics.Color.parseColor("#3A9AD9")
                                    style = android.graphics.Paint.Style.FILL
                                }
                            )
                            // Label Kaaba/Qibla icon/text near arrow
                            drawContext.canvas.nativeCanvas.drawText(
                                "🕋",
                                center.x,
                                center.y - radius + 70f,
                                android.graphics.Paint().apply {
                                    textSize = 48f
                                    textAlign = android.graphics.Paint.Align.CENTER
                                }
                            )
                        }
                    }

                    // 4. Center hub pivot point
                    drawCircle(
                        color = Color.White,
                        radius = 8.dp.toPx(),
                        center = center
                    )
                }
            }

            // Direction info
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = if (isAligned) "Aligned with Qibla!" else "Align the arrow with the Kaaba 🕋",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = themeColor
                    )
                )

                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Phone Heading", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        Text(
                            text = String.format("%.0f°", heading),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Qibla Angle", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        Text(
                            text = String.format("%.0f°", qiblaAngle),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = themeColor)
                        )
                    }
                }
            }
        }
    }
}

private fun calculateQibla(latitude: Double, longitude: Double): Double {
    val latRad = Math.toRadians(latitude)
    val lonRad = Math.toRadians(longitude)
    val kaabaLatRad = Math.toRadians(21.4225)
    val kaabaLonRad = Math.toRadians(39.8262)

    val y = sin(kaabaLonRad - lonRad)
    val x = cos(latRad) * tan(kaabaLatRad) - sin(latRad) * cos(kaabaLonRad - lonRad)
    val qiblaRad = atan2(y, x)
    return (Math.toDegrees(qiblaRad) + 360.0) % 360.0
}

private fun calculateDistanceToKaaba(latitude: Double, longitude: Double): Double {
    val r = 6371.0 // Earth radius in km
    val latRad = Math.toRadians(latitude)
    val lonRad = Math.toRadians(longitude)
    val kaabaLatRad = Math.toRadians(21.4225)
    val kaabaLonRad = Math.toRadians(39.8262)

    val dLat = kaabaLatRad - latRad
    val dLon = kaabaLonRad - lonRad

    val a = sin(dLat / 2).pow(2.0) + cos(latRad) * cos(kaabaLatRad) * sin(dLon / 2).pow(2.0)
    val c = 2 * asin(sqrt(a))
    return r * c
}
