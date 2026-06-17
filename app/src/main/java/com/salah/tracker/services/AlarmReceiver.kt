package com.salah.tracker.services

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.salah.tracker.data.database.AppDatabase
import com.salah.tracker.data.database.entities.PrayerLog
import com.salah.tracker.data.database.entities.UserPreferences
import com.salah.tracker.data.calculator.PrayerTimeCalculator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val actionType = intent.getStringExtra(EXTRA_ACTION_TYPE) ?: return
        val prayerName = intent.getStringExtra(EXTRA_PRAYER_NAME) ?: ""
        val dateStr = intent.getStringExtra(EXTRA_DATE) ?: LocalDate.now().toString()

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(context)
                val prefs = db.userPreferencesDao().getUserPreferences() ?: UserPreferences()

                when (actionType) {
                    ACTION_PRAYER_START -> {
                        // 1. Send prayer notification if enabled
                        val isEnabled = when (prayerName) {
                            "Fajr" -> prefs.fajrNotifEnabled
                            "Dhuhr" -> prefs.dhuhrNotifEnabled
                            "Asr" -> prefs.asrNotifEnabled
                            "Maghrib" -> prefs.maghribNotifEnabled
                            "Isha" -> prefs.ishaNotifEnabled
                            else -> false
                        }
                        if (isEnabled) {
                            NotificationHelper.sendPrayerNotification(context, prayerName)
                        }

                        // 2. Schedule Missed check (30 minutes before next prayer)
                        if (prefs.missedPrayerRemindersEnabled) {
                            scheduleMissedCheck30MinsBeforeNext(context, prayerName, dateStr, prefs)
                        }
                    }

                    ACTION_MISSED_CHECK -> {
                        // Query the log for today's prayer
                        val log = db.prayerLogDao().getPrayerLog(dateStr, prayerName)
                        // If log is pending (not offered / excused), mark as Missed/Qaza
                        if (log == null || log.status == "Pending") {
                            val existingLog = log ?: PrayerLog(date = dateStr, prayerName = prayerName, status = "Pending")
                            db.prayerLogDao().insertOrReplace(existingLog.copy(status = "Missed/Qaza"))
                            db.qazaCounterDao().incrementQaza(prayerName)
                            
                            // Fire missed notification
                            NotificationHelper.sendMissedPrayerNotification(context, prayerName)
                        }
                    }

                    ACTION_POST_SALAH_PROMPT -> {
                        if (prefs.postSalahRecitationEnabled) {
                            NotificationHelper.sendPostSalahRecitationNotification(context)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("AlarmReceiver", "Error processing alarm", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val EXTRA_ACTION_TYPE = "action_type"
        const val EXTRA_PRAYER_NAME = "prayer_name"
        const val EXTRA_DATE = "date"

        const val ACTION_PRAYER_START = "ACTION_PRAYER_START"
        const val ACTION_MISSED_CHECK = "ACTION_MISSED_CHECK"
        const val ACTION_POST_SALAH_PROMPT = "ACTION_POST_SALAH_PROMPT"

        fun rescheduleAlarms(context: Context) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = AppDatabase.getDatabase(context)
                    val prefs = db.userPreferencesDao().getUserPreferences() ?: UserPreferences()
                    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

                    val now = LocalDateTime.now()
                    val today = now.toLocalDate()
                    val tomorrow = today.plusDays(1)

                    // Calculate times for today and tomorrow
                    val todayTimes = calculateTimes(prefs, today)
                    val tomorrowTimes = calculateTimes(prefs, tomorrow)

                    val scheduleList = mutableListOf<Pair<String, LocalDateTime>>()
                    
                    // Add today's prayers
                    val todayPrayers = listOf(
                        "Fajr" to todayTimes.fajr,
                        "Dhuhr" to todayTimes.dhuhr,
                        "Asr" to todayTimes.asr,
                        "Maghrib" to todayTimes.maghrib,
                        "Isha" to todayTimes.isha
                    )
                    for ((name, time) in todayPrayers) {
                        val dt = LocalDateTime.of(today, time)
                        if (dt.isAfter(now)) {
                            scheduleList.add(name to dt)
                        }
                    }

                    // Add tomorrow's prayers
                    val tomorrowPrayers = listOf(
                        "Fajr" to tomorrowTimes.fajr,
                        "Dhuhr" to tomorrowTimes.dhuhr,
                        "Asr" to tomorrowTimes.asr,
                        "Maghrib" to tomorrowTimes.maghrib,
                        "Isha" to tomorrowTimes.isha
                    )
                    for ((name, time) in tomorrowPrayers) {
                        val dt = LocalDateTime.of(tomorrow, time)
                        scheduleList.add(name to dt)
                    }

                    // Cancel old alarms and schedule new ones (limit to next 5 upcoming prayers for battery/system limit safety)
                    val upcoming = scheduleList.sortedBy { it.second }.take(5)
                    for (i in upcoming.indices) {
                        val (name, dt) = upcoming[i]
                        val intent = Intent(context, AlarmReceiver::class.java).apply {
                            putExtra(EXTRA_ACTION_TYPE, ACTION_PRAYER_START)
                            putExtra(EXTRA_PRAYER_NAME, name)
                            putExtra(EXTRA_DATE, dt.toLocalDate().toString())
                        }
                        
                        val requestCode = 100 + i
                        val pendingIntent = PendingIntent.getBroadcast(
                            context,
                            requestCode,
                            intent,
                            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                        )

                        val epochMillis = dt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                        
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            if (alarmManager.canScheduleExactAlarms()) {
                                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, epochMillis, pendingIntent)
                            } else {
                                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, epochMillis, pendingIntent)
                            }
                        } else {
                            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, epochMillis, pendingIntent)
                        }
                    }

                    // Register a check for tomorrow morning to trigger reschedule again
                    val midnightRescheduleTime = LocalDateTime.of(tomorrow, LocalTime.of(0, 5)) // 12:05 AM
                    val midnightIntent = Intent(context, BootReceiver::class.java) // Send to boot receiver to trigger full reschedule
                    val midnightPendingIntent = PendingIntent.getBroadcast(
                        context,
                        999,
                        midnightIntent,
                        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                    )
                    val midnightEpoch = midnightRescheduleTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, midnightEpoch, midnightPendingIntent)

                } catch (e: Exception) {
                    Log.e("AlarmReceiver", "Failed to reschedule alarms", e)
                }
            }
        }

        fun schedulePostSalahPrompt(context: Context, delayMinutes: Int) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, AlarmReceiver::class.java).apply {
                putExtra(EXTRA_ACTION_TYPE, ACTION_POST_SALAH_PROMPT)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                500,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            val triggerTime = System.currentTimeMillis() + delayMinutes * 60 * 1000L
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        }

        private fun scheduleMissedCheck30MinsBeforeNext(
            context: Context,
            currentPrayerName: String,
            currentDateStr: String,
            prefs: UserPreferences
        ) {
            try {
                val currentDate = LocalDate.parse(currentDateStr)
                val (nextPrayerName, nextDate) = when (currentPrayerName) {
                    "Fajr" -> "Dhuhr" to currentDate
                    "Dhuhr" -> "Asr" to currentDate
                    "Asr" -> "Maghrib" to currentDate
                    "Maghrib" -> "Isha" to currentDate
                    "Isha" -> "Fajr" to currentDate.plusDays(1)
                    else -> return
                }

                val nextTimes = calculateTimes(prefs, nextDate)
                val nextLocalTime = when (nextPrayerName) {
                    "Fajr" -> nextTimes.fajr
                    "Dhuhr" -> nextTimes.dhuhr
                    "Asr" -> nextTimes.asr
                    "Maghrib" -> nextTimes.maghrib
                    "Isha" -> nextTimes.isha
                    else -> return
                }

                val nextPrayerDateTime = LocalDateTime.of(nextDate, nextLocalTime)
                val triggerDateTime = nextPrayerDateTime.minusMinutes(30)

                val now = LocalDateTime.now()
                val finalTriggerDateTime = if (triggerDateTime.isBefore(now)) {
                    now.plusMinutes(1)
                } else {
                    triggerDateTime
                }

                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                val intent = Intent(context, AlarmReceiver::class.java).apply {
                    putExtra(EXTRA_ACTION_TYPE, ACTION_MISSED_CHECK)
                    putExtra(EXTRA_PRAYER_NAME, currentPrayerName)
                    putExtra(EXTRA_DATE, currentDateStr)
                }

                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    300 + currentPrayerName.hashCode(),
                    intent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )

                val epochMillis = finalTriggerDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, epochMillis, pendingIntent)
                    } else {
                        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, epochMillis, pendingIntent)
                    }
                } else {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, epochMillis, pendingIntent)
                }
                Log.d("AlarmReceiver", "Scheduled Qaza check for $currentPrayerName on $currentDateStr at $finalTriggerDateTime (30 mins before $nextPrayerName)")
            } catch (e: Exception) {
                Log.e("AlarmReceiver", "Error scheduling missed check", e)
            }
        }

        private fun calculateTimes(prefs: UserPreferences, date: LocalDate): PrayerTimeCalculator.PrayerTimes {
            val calculator = PrayerTimeCalculator()
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
    }
}
