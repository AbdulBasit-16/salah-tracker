package com.salah.tracker.data.database.daos

import androidx.room.*
import com.salah.tracker.data.database.entities.QazaCounter
import kotlinx.coroutines.flow.Flow

@Dao
interface QazaCounterDao {
    @Query("SELECT * FROM qaza_counters")
    fun getAllQazaCountersFlow(): Flow<List<QazaCounter>>

    @Query("SELECT * FROM qaza_counters")
    suspend fun getAllQazaCounters(): List<QazaCounter>

    @Query("SELECT * FROM qaza_counters WHERE prayerName = :prayerName LIMIT 1")
    suspend fun getQazaCounter(prayerName: String): QazaCounter?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplace(qazaCounter: QazaCounter)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplaceAll(qazaCounters: List<QazaCounter>)

    @Query("UPDATE qaza_counters SET count = count + 1 WHERE prayerName = :prayerName")
    suspend fun incrementQaza(prayerName: String)

    @Query("UPDATE qaza_counters SET count = CASE WHEN count > 0 THEN count - 1 ELSE 0 END WHERE prayerName = :prayerName")
    suspend fun decrementQaza(prayerName: String)

    @Query("UPDATE qaza_counters SET count = :count WHERE prayerName = :prayerName")
    suspend fun setQazaCount(prayerName: String, count: Int)
}
