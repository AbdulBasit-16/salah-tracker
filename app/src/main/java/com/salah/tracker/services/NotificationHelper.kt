package com.salah.tracker.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.salah.tracker.MainActivity

class NotificationHelper {

    companion object {
        const val PRAYER_CHANNEL_ID = "salah_tracker_prayer_times"
        const val REMINDER_CHANNEL_ID = "salah_tracker_reminders"
        
        private const val PRAYER_NOTIF_ID_OFFSET = 1000
        private const val REMINDER_NOTIF_ID_OFFSET = 2000

        fun createNotificationChannels(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

                // Prayer Times Channel (High Importance, Serene Sound)
                val prayerChannel = NotificationChannel(
                    PRAYER_CHANNEL_ID,
                    "Prayer Time Alerts",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Notifications exactly at the start time of Fajr, Dhuhr, Asr, Maghrib, and Isha."
                    enableLights(true)
                    enableVibration(true)
                    setShowBadge(true)
                }

                // General Reminders Channel (Default Importance)
                val reminderChannel = NotificationChannel(
                    REMINDER_CHANNEL_ID,
                    "Habit Reminders",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Reminders for missed prayers, post-salah Quran prompts, and daily activity."
                    enableLights(true)
                    setShowBadge(true)
                }

                manager.createNotificationChannel(prayerChannel)
                manager.createNotificationChannel(reminderChannel)
            }
        }

        fun sendPrayerNotification(context: Context, prayerName: String) {
            createNotificationChannels(context)
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                prayerName.hashCode(),
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            val builder = NotificationCompat.Builder(context, PRAYER_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("It's time for $prayerName")
                .setContentText("It is time to offer the $prayerName prayer. Tap to record your Salah.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setSound(soundUri)

            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val notifId = PRAYER_NOTIF_ID_OFFSET + prayerName.hashCode()
            manager.notify(notifId, builder.build())
        }

        fun sendMissedPrayerNotification(context: Context, prayerName: String) {
            createNotificationChannels(context)
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                (prayerName + "missed").hashCode(),
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val builder = NotificationCompat.Builder(context, REMINDER_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Missed $prayerName Reminder")
                .setContentText("You haven't logged your $prayerName prayer yet. You can log it now or record it as a Qaza.")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)

            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val notifId = REMINDER_NOTIF_ID_OFFSET + prayerName.hashCode()
            manager.notify(notifId, builder.build())
        }

        fun sendPostSalahRecitationNotification(context: Context) {
            createNotificationChannels(context)
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                "postsalah".hashCode(),
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val builder = NotificationCompat.Builder(context, REMINDER_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_menu_agenda)
                .setContentTitle("Quran Recitation")
                .setContentText("A beautiful time to connect with the Quran. Spend a few minutes reading a page or two.")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)

            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.notify(3001, builder.build())
        }
    }
}
