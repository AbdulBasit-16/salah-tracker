package com.salah.tracker.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "qaza_counters")
data class QazaCounter(
    @PrimaryKey val prayerName: String, // Fajr, Dhuhr, Asr, Maghrib, Isha
    val count: Int = 0
)
