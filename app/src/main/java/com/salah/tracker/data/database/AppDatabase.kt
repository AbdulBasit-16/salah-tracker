package com.salah.tracker.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.salah.tracker.data.database.daos.PrayerLogDao
import com.salah.tracker.data.database.daos.QazaCounterDao
import com.salah.tracker.data.database.daos.QuranLogDao
import com.salah.tracker.data.database.daos.UserPreferencesDao
import com.salah.tracker.data.database.entities.PrayerLog
import com.salah.tracker.data.database.entities.QazaCounter
import com.salah.tracker.data.database.entities.QuranLog
import com.salah.tracker.data.database.entities.UserPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        UserPreferences::class,
        PrayerLog::class,
        QazaCounter::class,
        QuranLog::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userPreferencesDao(): UserPreferencesDao
    abstract fun prayerLogDao(): PrayerLogDao
    abstract fun qazaCounterDao(): QazaCounterDao
    abstract fun quranLogDao(): QuranLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "salah_tracker_database"
                )
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Initialize database on first launch
                        val database = INSTANCE
                        if (database != null) {
                            CoroutineScope(Dispatchers.IO).launch {
                                // Default preferences
                                database.userPreferencesDao().insertUserPreferences(UserPreferences())
                                // Default Qaza counters
                                val initialQaza = listOf(
                                    QazaCounter("Fajr", 0),
                                    QazaCounter("Dhuhr", 0),
                                    QazaCounter("Asr", 0),
                                    QazaCounter("Maghrib", 0),
                                    QazaCounter("Isha", 0)
                                )
                                database.qazaCounterDao().insertOrReplaceAll(initialQaza)
                            }
                        } else {
                            // If INSTANCE is not populated yet, we can write a SQL transaction on the db directly
                            // to ensure standard integrity
                            db.execSQL("INSERT OR IGNORE INTO user_preferences (id, calculationMethod, juristicMethod, latitude, longitude, timezoneOffset, fajrNotifEnabled, dhuhrNotifEnabled, asrNotifEnabled, maghribNotifEnabled, ishaNotifEnabled, missedPrayerRemindersEnabled, missedPrayerWindowMinutes, postSalahRecitationEnabled, postSalahDelayMinutes, lastActiveDate) VALUES (1, 'MWL', 'STANDARD', 21.4225, 39.8262, 3.0, 1, 1, 1, 1, 1, 1, 45, 1, 15, '')")
                            db.execSQL("INSERT OR IGNORE INTO qaza_counters (prayerName, count) VALUES ('Fajr', 0)")
                            db.execSQL("INSERT OR IGNORE INTO qaza_counters (prayerName, count) VALUES ('Dhuhr', 0)")
                            db.execSQL("INSERT OR IGNORE INTO qaza_counters (prayerName, count) VALUES ('Asr', 0)")
                            db.execSQL("INSERT OR IGNORE INTO qaza_counters (prayerName, count) VALUES ('Maghrib', 0)")
                            db.execSQL("INSERT OR IGNORE INTO qaza_counters (prayerName, count) VALUES ('Isha', 0)")
                        }
                    }
                })
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
