package salahtracker.app

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.SharedPreferences
import android.widget.RemoteViews
import es.antonborri.home_widget.HomeWidgetProvider

class PrayerWidgetProvider : HomeWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray, widgetData: SharedPreferences) {
        appWidgetIds.forEach { widgetId ->
            val views = RemoteViews(context.packageName, R.layout.prayer_widget).apply {
                
                val prayerName = widgetData.getString("prayer_name", "Loading...")
                val timeLeft = widgetData.getString("time_left", "--:--:--")

                setTextViewText(R.id.widget_prayer_name, prayerName)
                setTextViewText(R.id.widget_time_left, timeLeft)
            }

            appWidgetManager.updateAppWidget(widgetId, views)
        }
    }
}
