package com.salah.tracker.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.salah.tracker.data.database.entities.UserPreferences
import com.salah.tracker.data.repository.SalahRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repository: SalahRepository,
    private val context: Context
) : ViewModel() {

    val preferences = repository.getUserPreferencesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserPreferences())

    fun updateCoordinates(latitude: Double, longitude: Double, timezoneOffset: Double) {
        viewModelScope.launch {
            val current = repository.getUserPreferences() ?: UserPreferences()
            val updated = current.copy(
                latitude = latitude,
                longitude = longitude,
                timezoneOffset = timezoneOffset
            )
            repository.saveUserPreferences(updated)
            // Reschedule alarms
            com.salah.tracker.services.AlarmReceiver.rescheduleAlarms(context)
        }
    }

    fun updateCalculationMethod(method: String) {
        viewModelScope.launch {
            val current = repository.getUserPreferences() ?: UserPreferences()
            val updated = current.copy(calculationMethod = method)
            repository.saveUserPreferences(updated)
            com.salah.tracker.services.AlarmReceiver.rescheduleAlarms(context)
        }
    }

    fun updateJuristicMethod(method: String) {
        viewModelScope.launch {
            val current = repository.getUserPreferences() ?: UserPreferences()
            val updated = current.copy(juristicMethod = method)
            repository.saveUserPreferences(updated)
            com.salah.tracker.services.AlarmReceiver.rescheduleAlarms(context)
        }
    }

    fun updateNotificationToggles(
        fajr: Boolean,
        dhuhr: Boolean,
        asr: Boolean,
        maghrib: Boolean,
        isha: Boolean,
        missed: Boolean,
        postSalah: Boolean
    ) {
        viewModelScope.launch {
            val current = repository.getUserPreferences() ?: UserPreferences()
            val updated = current.copy(
                fajrNotifEnabled = fajr,
                dhuhrNotifEnabled = dhuhr,
                asrNotifEnabled = asr,
                maghribNotifEnabled = maghrib,
                ishaNotifEnabled = isha,
                missedPrayerRemindersEnabled = missed,
                postSalahRecitationEnabled = postSalah
            )
            repository.saveUserPreferences(updated)
            com.salah.tracker.services.AlarmReceiver.rescheduleAlarms(context)
        }
    }
}
