package com.salah.tracker.data.database.daos

import androidx.room.*
import com.salah.tracker.data.database.entities.PrayerLog
import kotlinx.coroutines.flow.Flow

@Dao
interface PrayerLogDao {
    @Query("SELECT * FROM prayer_logs WHERE date = :date")
    fun getPrayerLogsForDateFlow(date: String): Flow<List<PrayerLog>>

    @Query("SELECT * FROM prayer_logs WHERE date = :date")
    suspend fun getPrayerLogsForDate(date: String): List<PrayerLog>

    @Query("SELECT * FROM prayer_logs WHERE date = :date AND prayerName = :prayerName LIMIT 1")
    suspend fun getPrayerLog(date: String, prayerName: String): PrayerLog?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplace(prayerLog: PrayerLog)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplaceAll(prayerLogs: List<PrayerLog>)

    @Query("SELECT * FROM prayer_logs WHERE date BETWEEN :startDate AND :endDate")
    fun getPrayerLogsInDateRangeFlow(startDate: String, endDate: String): Flow<List<PrayerLog>>

    @Query("SELECT * FROM prayer_logs WHERE date BETWEEN :startDate AND :endDate")
    suspend fun getPrayerLogsInDateRange(startDate: String, endDate: String): List<PrayerLog>

    @Query("SELECT COUNT(*) FROM prayer_logs WHERE status = 'Offered On-Time'")
    fun getOnTimeCountFlow(): Flow<Int>
}
