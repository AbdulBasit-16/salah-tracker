package com.salah.tracker.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_preferences")
data class UserPreferences(
    @PrimaryKey val id: Int = 1,
    val calculationMethod: String = "MWL",
    val juristicMethod: String = "STANDARD",
    val themeName: String = "FOREST_GREEN",
    val latitude: Double = 21.4225, // Default: Makkah
    val longitude: Double = 39.8262,
    val timezoneOffset: Double = 3.0,
    val fajrNotifEnabled: Boolean = true,
    val dhuhrNotifEnabled: Boolean = true,
    val asrNotifEnabled: Boolean = true,
    val maghribNotifEnabled: Boolean = true,
    val ishaNotifEnabled: Boolean = true,
    val missedPrayerRemindersEnabled: Boolean = true,
    val missedPrayerWindowMinutes: Int = 45,
    val postSalahRecitationEnabled: Boolean = true,
    val postSalahDelayMinutes: Int = 15,
    val lastActiveDate: String = "",
    val selectedCity: String = "Custom",
    val quranScript: String = "UTHMANI",
    val showEnglishTranslation: Boolean = true,
    val showUrduTranslation: Boolean = true
)
