package com.salah.tracker.services

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.widget.RemoteViews
import com.salah.tracker.R

class PrayerAppWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        // General updates are handled, but PrayerCountdownService pushes tick updates.
    }

    companion object {
        fun updateWidget(context: Context, location: String, prayerName: String, timeLeft: String) {
            try {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val thisWidget = ComponentName(context, PrayerAppWidgetProvider::class.java)
                val allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
                if (allWidgetIds.isEmpty()) return

                for (widgetId in allWidgetIds) {
                    val views = RemoteViews(context.packageName, R.layout.widget_layout)
                    views.setTextViewText(R.id.widget_location, "📌 $location")
                    views.setTextViewText(R.id.widget_prayer_title, "Next Prayer: $prayerName")
                    views.setTextViewText(R.id.widget_countdown, "- $timeLeft")
                    appWidgetManager.updateAppWidget(widgetId, views)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
