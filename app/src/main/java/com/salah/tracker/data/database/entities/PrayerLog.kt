package com.salah.tracker.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "prayer_logs")
data class PrayerLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String, // format: "yyyy-MM-dd"
    val prayerName: String, // Fajr, Dhuhr, Asr, Maghrib, Isha, Tahajjud, Sunnah, Nafl
    val status: String, // Offered On-Time, Offered Late, Missed/Qaza, Excused, Pending
    val loggedTimestamp: Long? = null
)
