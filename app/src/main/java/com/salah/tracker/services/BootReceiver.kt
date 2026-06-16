package com.salah.tracker.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d("BootReceiver", "Received action: $action, rescheduling alarms...")
        
        // Reschedule all prayer alarms
        AlarmReceiver.rescheduleAlarms(context)
    }
}
