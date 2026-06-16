package com.salah.tracker.data.database.daos

import androidx.room.*
import com.salah.tracker.data.database.entities.QuranLog
import kotlinx.coroutines.flow.Flow

@Dao
interface QuranLogDao {
    @Query("SELECT * FROM quran_logs ORDER BY timestamp DESC")
    fun getAllQuranLogsFlow(): Flow<List<QuranLog>>

    @Query("SELECT * FROM quran_logs ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentQuranLogsFlow(limit: Int): Flow<List<QuranLog>>

    @Query("SELECT * FROM quran_logs ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestQuranLog(): QuranLog?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuranLog(quranLog: QuranLog)

    @Delete
    suspend fun deleteQuranLog(quranLog: QuranLog)

    @Query("SELECT SUM(pagesRead) FROM quran_logs")
    fun getTotalPagesReadFlow(): Flow<Int?>
}
