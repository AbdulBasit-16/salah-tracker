package com.salah.tracker.data.repository

import com.salah.tracker.data.database.daos.PrayerLogDao
import com.salah.tracker.data.database.daos.QazaCounterDao
import com.salah.tracker.data.database.daos.QuranLogDao
import com.salah.tracker.data.database.daos.UserPreferencesDao
import com.salah.tracker.data.database.entities.PrayerLog
import com.salah.tracker.data.database.entities.QazaCounter
import com.salah.tracker.data.database.entities.QuranLog
import com.salah.tracker.data.database.entities.UserPreferences
import kotlinx.coroutines.flow.Flow

class SalahRepositoryImpl(
    private val preferencesDao: UserPreferencesDao,
    private val prayerLogDao: PrayerLogDao,
    private val qazaCounterDao: QazaCounterDao,
    private val quranLogDao: QuranLogDao
) : SalahRepository {

    override fun getUserPreferencesFlow(): Flow<UserPreferences?> = preferencesDao.getUserPreferencesFlow()

    override suspend fun getUserPreferences(): UserPreferences? = preferencesDao.getUserPreferences()

    override suspend fun saveUserPreferences(preferences: UserPreferences) {
        preferencesDao.insertUserPreferences(preferences)
    }

    override fun getPrayerLogsForDateFlow(date: String): Flow<List<PrayerLog>> = prayerLogDao.getPrayerLogsForDateFlow(date)

    override suspend fun getPrayerLogsForDate(date: String): List<PrayerLog> = prayerLogDao.getPrayerLogsForDate(date)

    override suspend fun getPrayerLog(date: String, prayerName: String): PrayerLog? = prayerLogDao.getPrayerLog(date, prayerName)

    override suspend fun insertOrUpdatePrayerLog(log: PrayerLog) {
        prayerLogDao.insertOrReplace(log)
    }

    override suspend fun insertOrUpdatePrayerLogs(logs: List<PrayerLog>) {
        prayerLogDao.insertOrReplaceAll(logs)
    }

    override fun getPrayerLogsInDateRangeFlow(startDate: String, endDate: String): Flow<List<PrayerLog>> {
        return prayerLogDao.getPrayerLogsInDateRangeFlow(startDate, endDate)
    }

    override suspend fun getPrayerLogsInDateRange(startDate: String, endDate: String): List<PrayerLog> {
        return prayerLogDao.getPrayerLogsInDateRange(startDate, endDate)
    }

    override fun getOnTimeCountFlow(): Flow<Int> = prayerLogDao.getOnTimeCountFlow()

    override fun getAllQazaCountersFlow(): Flow<List<QazaCounter>> = qazaCounterDao.getAllQazaCountersFlow()

    override suspend fun getAllQazaCounters(): List<QazaCounter> = qazaCounterDao.getAllQazaCounters()

    override suspend fun incrementQaza(prayerName: String) {
        qazaCounterDao.incrementQaza(prayerName)
    }

    override suspend fun decrementQaza(prayerName: String) {
        qazaCounterDao.decrementQaza(prayerName)
    }

    override suspend fun setQazaCount(prayerName: String, count: Int) {
        qazaCounterDao.setQazaCount(prayerName, count)
    }

    override fun getAllQuranLogsFlow(): Flow<List<QuranLog>> = quranLogDao.getAllQuranLogsFlow()

    override fun getRecentQuranLogsFlow(limit: Int): Flow<List<QuranLog>> = quranLogDao.getRecentQuranLogsFlow(limit)

    override suspend fun getLatestQuranLog(): QuranLog? = quranLogDao.getLatestQuranLog()

    override suspend fun insertQuranLog(log: QuranLog) {
        quranLogDao.insertQuranLog(log)
    }

    override suspend fun deleteQuranLog(log: QuranLog) {
        quranLogDao.deleteQuranLog(log)
    }

    override fun getTotalPagesReadFlow(): Flow<Int?> = quranLogDao.getTotalPagesReadFlow()
}
