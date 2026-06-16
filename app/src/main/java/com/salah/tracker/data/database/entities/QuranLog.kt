package com.salah.tracker.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "quran_logs")
data class QuranLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val surah: String,
    val startAyah: Int,
    val endAyah: Int,
    val startPage: Int,
    val endPage: Int,
    val pagesRead: Int
)
