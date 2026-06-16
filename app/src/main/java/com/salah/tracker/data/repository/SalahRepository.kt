package com.salah.tracker.data.repository

import com.salah.tracker.data.database.entities.PrayerLog
import com.salah.tracker.data.database.entities.QazaCounter
import com.salah.tracker.data.database.entities.QuranLog
import com.salah.tracker.data.database.entities.UserPreferences
import kotlinx.coroutines.flow.Flow

interface SalahRepository {
    // UserPreferences
    fun getUserPreferencesFlow(): Flow<UserPreferences?>
    suspend fun getUserPreferences(): UserPreferences?
    suspend fun saveUserPreferences(preferences: UserPreferences)

    // PrayerLog
    fun getPrayerLogsForDateFlow(date: String): Flow<List<PrayerLog>>
    suspend fun getPrayerLogsForDate(date: String): List<PrayerLog>
    suspend fun getPrayerLog(date: String, prayerName: String): PrayerLog?
    suspend fun insertOrUpdatePrayerLog(log: PrayerLog)
    suspend fun insertOrUpdatePrayerLogs(logs: List<PrayerLog>)
    fun getPrayerLogsInDateRangeFlow(startDate: String, endDate: String): Flow<List<PrayerLog>>
    suspend fun getPrayerLogsInDateRange(startDate: String, endDate: String): List<PrayerLog>
    fun getOnTimeCountFlow(): Flow<Int>

    // QazaCounter
    fun getAllQazaCountersFlow(): Flow<List<QazaCounter>>
    suspend fun getAllQazaCounters(): List<QazaCounter>
    suspend fun incrementQaza(prayerName: String)
    suspend fun decrementQaza(prayerName: String)
    suspend fun setQazaCount(prayerName: String, count: Int)

    // QuranLog
    fun getAllQuranLogsFlow(): Flow<List<QuranLog>>
    fun getRecentQuranLogsFlow(limit: Int): Flow<List<QuranLog>>
    suspend fun getLatestQuranLog(): QuranLog?
    suspend fun insertQuranLog(log: QuranLog)
    suspend fun deleteQuranLog(log: QuranLog)
    fun getTotalPagesReadFlow(): Flow<Int?>
}
