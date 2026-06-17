package com.salah.tracker.services

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.salah.tracker.MainActivity
import com.salah.tracker.data.database.AppDatabase
import com.salah.tracker.data.database.entities.UserPreferences
import com.salah.tracker.data.calculator.PrayerTimeCalculator
import kotlinx.coroutines.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit

class PrayerCountdownService : Service() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)
    private val calculator = PrayerTimeCalculator()
    private var tickerJob: Job? = null

    companion object {
        private const val CHANNEL_ID = "prayer_countdown_channel"
        private const val NOTIFICATION_ID = 2026
        
        fun startService(context: Context) {
            val intent = Intent(context, PrayerCountdownService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, PrayerCountdownService::class.java)
            context.stopService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = createNotification("Calculating...")
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            startForeground(
                NOTIFICATION_ID, 
                notification, 
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        startTicker()
        return START_STICKY
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = serviceScope.launch {
            val db = AppDatabase.getDatabase(applicationContext)
            while (isActive) {
                val prefs = db.userPreferencesDao().getUserPreferences() ?: UserPreferences()
                val now = LocalDateTime.now()
                val todayTimes = calculateTimesForDate(prefs, now.toLocalDate())
                val tomorrowTimes = calculateTimesForDate(prefs, now.toLocalDate().plusDays(1))

                val prayerList = listOf(
                    "Fajr" to LocalDateTime.of(now.toLocalDate(), todayTimes.fajr),
                    "Sunrise" to LocalDateTime.of(now.toLocalDate(), todayTimes.sunrise),
                    "Dhuhr" to LocalDateTime.of(now.toLocalDate(), todayTimes.dhuhr),
                    "Asr" to LocalDateTime.of(now.toLocalDate(), todayTimes.asr),
                    "Maghrib" to LocalDateTime.of(now.toLocalDate(), todayTimes.maghrib),
                    "Isha" to LocalDateTime.of(now.toLocalDate(), todayTimes.isha),
                    "Fajr (Tomorrow)" to LocalDateTime.of(now.toLocalDate().plusDays(1), tomorrowTimes.fajr)
                )

                val nextPrayer = prayerList.firstOrNull { it.second.isAfter(now) }
                if (nextPrayer != null) {
                    val seconds = ChronoUnit.SECONDS.between(now, nextPrayer.second)
                    val h = seconds / 3600
                    val m = (seconds % 3600) / 60
                    val s = seconds % 60
                    val timeLeftStr = String.format("%02d:%02d:%02d", h, m, s)
                    
                    val content = "${nextPrayer.first} in $timeLeftStr (${prefs.selectedCity})"
                    val notification = createNotification(content)
                    val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    notificationManager.notify(NOTIFICATION_ID, notification)
                    
                    // Also trigger Widget update!
                    PrayerAppWidgetProvider.updateWidget(applicationContext, prefs.selectedCity, nextPrayer.first, timeLeftStr)
                }
                delay(1000L)
            }
        }
    }

    private fun calculateTimesForDate(prefs: UserPreferences, date: LocalDate): PrayerTimeCalculator.PrayerTimes {
        val method = try {
            PrayerTimeCalculator.CalculationMethod.valueOf(prefs.calculationMethod)
        } catch (e: Exception) {
            PrayerTimeCalculator.CalculationMethod.MWL
        }
        val juristic = try {
            PrayerTimeCalculator.JuristicMethod.valueOf(prefs.juristicMethod)
        } catch (e: Exception) {
            PrayerTimeCalculator.JuristicMethod.STANDARD
        }
        return calculator.calculate(
            latitude = prefs.latitude,
            longitude = prefs.longitude,
            timezoneOffset = prefs.timezoneOffset,
            year = date.year,
            month = date.monthValue,
            day = date.dayOfMonth,
            method = method,
            juristic = juristic
        )
    }

    private fun createNotification(contentText: String): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Salah remaining time")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_menu_today)
            .setContentIntent(pendingIntent)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID, "Salah remaining time Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        tickerJob?.cancel()
        serviceJob.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
