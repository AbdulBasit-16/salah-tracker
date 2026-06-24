package com.salah.tracker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.salah.tracker.data.calculator.PrayerTimeCalculator
import com.salah.tracker.data.database.entities.PrayerLog
import com.salah.tracker.data.database.entities.QazaCounter
import com.salah.tracker.data.database.entities.UserPreferences
import com.salah.tracker.data.repository.SalahRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class SalahViewModel(private val repository: SalahRepository) : ViewModel() {

    private val calculator = PrayerTimeCalculator()

    val userPreferences = repository.getUserPreferencesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserPreferences())

    private val _currentDate = MutableStateFlow(LocalDate.now())
    val currentDate: StateFlow<LocalDate> = _currentDate.asStateFlow()

    // Today's prayer logs (obligatory and optional)
    val todayPrayerLogs: StateFlow<List<PrayerLog>> = _currentDate
        .flatMapLatest { date ->
            repository.getPrayerLogsForDateFlow(date.toString())
        }
        .onEach { logs ->
            // If logs are empty, initialize them for today
            if (logs.isEmpty()) {
                initializePrayerLogsForDate(_currentDate.value)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @kotlinx.coroutines.ExperimentalCoroutinesApi
    val last7DaysPrayerLogs: StateFlow<List<PrayerLog>> = _currentDate
        .flatMapLatest { currentDate ->
            val start = currentDate.minusDays(6)
            repository.getPrayerLogsInDateRangeFlow(start.toString(), currentDate.toString())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Calculated prayer times for today
    val todayPrayerTimes = combine(userPreferences, _currentDate) { prefs, date ->
        calculateTimesForDate(prefs, date)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Running Qaza balances
    val qazaBalances: StateFlow<List<QazaCounter>> = repository.getAllQazaCountersFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Countdown state: Pair of (Next Prayer Name, Time Left String)
    private val _countdownState = MutableStateFlow<Pair<String, String>?>(null)
    val countdownState: StateFlow<Pair<String, String>?> = _countdownState.asStateFlow()

    init {
        // Start countdown ticking
        startCountdownTicker()
        
        // Run daily check for missed days to auto-increment Qaza
        viewModelScope.launch {
            checkAndProcessMissedDays()
        }
    }

    fun setDate(date: LocalDate) {
        _currentDate.value = date
    }

    private fun calculateTimesForDate(prefs: UserPreferences?, date: LocalDate): PrayerTimeCalculator.PrayerTimes {
        val p = prefs ?: UserPreferences()
        val method = try {
            PrayerTimeCalculator.CalculationMethod.valueOf(p.calculationMethod)
        } catch (e: Exception) {
            PrayerTimeCalculator.CalculationMethod.MWL
        }
        val juristic = try {
            PrayerTimeCalculator.JuristicMethod.valueOf(p.juristicMethod)
        } catch (e: Exception) {
            PrayerTimeCalculator.JuristicMethod.STANDARD
        }
        return calculator.calculate(
            latitude = p.latitude,
            longitude = p.longitude,
            timezoneOffset = p.timezoneOffset,
            year = date.year,
            month = date.monthValue,
            day = date.dayOfMonth,
            method = method,
            juristic = juristic
        )
    }

    private suspend fun initializePrayerLogsForDate(date: LocalDate) {
        val dateStr = date.toString()
        val defaultLogs = listOf(
            PrayerLog(date = dateStr, prayerName = "Fajr", status = "Pending"),
            PrayerLog(date = dateStr, prayerName = "Dhuhr", status = "Pending"),
            PrayerLog(date = dateStr, prayerName = "Asr", status = "Pending"),
            PrayerLog(date = dateStr, prayerName = "Maghrib", status = "Pending"),
            PrayerLog(date = dateStr, prayerName = "Isha", status = "Pending"),
            // Optional prayers
            PrayerLog(date = dateStr, prayerName = "Tahajjud", status = "Pending"),
            PrayerLog(date = dateStr, prayerName = "Sunnah", status = "Pending"),
            PrayerLog(date = dateStr, prayerName = "Nafl", status = "Pending")
        )
        repository.insertOrUpdatePrayerLogs(defaultLogs)
    }

    fun updatePrayerStatus(log: PrayerLog, newStatus: String) {
        viewModelScope.launch {
            val oldStatus = log.status
            val updatedLog = log.copy(
                status = newStatus,
                loggedTimestamp = if (newStatus.startsWith("Offered")) System.currentTimeMillis() else null
            )
            repository.insertOrUpdatePrayerLog(updatedLog)

            // Qaza counter adjustments:
            // If moving to Missed/Qaza, increment Qaza counter
            if (newStatus == "Missed/Qaza" && oldStatus != "Missed/Qaza") {
                if (isObligatory(log.prayerName)) {
                    repository.incrementQaza(log.prayerName)
                }
            }
            // If moving away from Missed/Qaza to Offered/Excused, decrement Qaza counter if it was marked as Qaza previously
            if (oldStatus == "Missed/Qaza" && newStatus != "Missed/Qaza") {
                if (isObligatory(log.prayerName)) {
                    repository.decrementQaza(log.prayerName)
                }
            }
        }
    }

    fun offerQazaMakeup(prayerName: String) {
        viewModelScope.launch {
            repository.decrementQaza(prayerName)
        }
    }

    fun incrementQazaBalance(prayerName: String) {
        viewModelScope.launch {
            repository.incrementQaza(prayerName)
        }
    }

    fun updateQazaBalance(prayerName: String, count: Int) {
        viewModelScope.launch {
            repository.setQazaCount(prayerName, count)
        }
    }

    fun updateSelectedCity(context: android.content.Context, cityName: String, latitude: Double, longitude: Double, timezoneOffset: Double) {
        viewModelScope.launch {
            val current = repository.getUserPreferences() ?: UserPreferences()
            val updated = current.copy(
                selectedCity = cityName,
                latitude = latitude,
                longitude = longitude,
                timezoneOffset = timezoneOffset
            )
            repository.saveUserPreferences(updated)
            // Reschedule alarms for the new location
            com.salah.tracker.services.AlarmReceiver.rescheduleAlarms(context.applicationContext)
        }
    }

    private fun isObligatory(name: String): Boolean {
        return name in listOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha")
    }

    private fun startCountdownTicker() {
        viewModelScope.launch {
            while (true) {
                val prefs = userPreferences.value
                val now = LocalDateTime.now()
                val todayTimes = calculateTimesForDate(prefs, now.toLocalDate())
                val tomorrowTimes = calculateTimesForDate(prefs, now.toLocalDate().plusDays(1))

                val prayerList = listOf(
                    "Fajr" to LocalDateTime.of(now.toLocalDate(), todayTimes.fajr),
                    "Sunrise" to LocalDateTime.of(now.toLocalDate(), todayTimes.sunrise),
                    "Dhuhr" to LocalDateTime.of(now.toLocalDate(), todayTimes.dhuhr),
                    "Asr" to LocalDateTime.of(now.toLocalDate(), todayTimes.asr),
                    "Maghrib" to LocalDateTime.of(now.toLocalDate(), todayTimes.maghrib),
                    "Isha" to LocalDateTime.of(now.toLocalDate(), todayTimes.isha),
                    // Wrap around to tomorrow's Fajr
                    "Fajr (Tomorrow)" to LocalDateTime.of(now.toLocalDate().plusDays(1), tomorrowTimes.fajr)
                )

                // Find next prayer (first prayer that is in the future)
                val nextPrayer = prayerList.firstOrNull { it.second.isAfter(now) }
                if (nextPrayer != null) {
                    val seconds = ChronoUnit.SECONDS.between(now, nextPrayer.second)
                    val h = seconds / 3600
                    val m = (seconds % 3600) / 60
                    val s = seconds % 60
                    val timeLeftStr = String.format("%02d:%02d:%02d", h, m, s)
                    _countdownState.value = Pair(nextPrayer.first, timeLeftStr)
                } else {
                    _countdownState.value = null
                }
                delay(1000L)
            }
        }
    }

    private suspend fun checkAndProcessMissedDays() {
        val prefs = repository.getUserPreferences() ?: return
        val todayStr = LocalDate.now().toString()
        val lastActiveStr = prefs.lastActiveDate

        if (lastActiveStr.isNotEmpty() && lastActiveStr != todayStr) {
            val lastActive = LocalDate.parse(lastActiveStr)
            val today = LocalDate.now()
            
            // Loop from last active date up to yesterday
            var checkDate = lastActive.plusDays(1)
            while (checkDate.isBefore(today)) {
                val checkDateStr = checkDate.toString()
                val logs = repository.getPrayerLogsForDate(checkDateStr)
                
                // If logs are empty, initialize them as Missed for the 5 daily prayers
                if (logs.isEmpty()) {
                    val missedLogs = listOf(
                        PrayerLog(date = checkDateStr, prayerName = "Fajr", status = "Missed/Qaza"),
                        PrayerLog(date = checkDateStr, prayerName = "Dhuhr", status = "Missed/Qaza"),
                        PrayerLog(date = checkDateStr, prayerName = "Asr", status = "Missed/Qaza"),
                        PrayerLog(date = checkDateStr, prayerName = "Maghrib", status = "Missed/Qaza"),
                        PrayerLog(date = checkDateStr, prayerName = "Isha", status = "Missed/Qaza"),
                        // Optionals are pending/skipped, not counted towards Qaza
                        PrayerLog(date = checkDateStr, prayerName = "Tahajjud", status = "Pending"),
                        PrayerLog(date = checkDateStr, prayerName = "Sunnah", status = "Pending"),
                        PrayerLog(date = checkDateStr, prayerName = "Nafl", status = "Pending")
                    )
                    repository.insertOrUpdatePrayerLogs(missedLogs)
                    
                    // Increment Qaza counter
                    repository.incrementQaza("Fajr")
                    repository.incrementQaza("Dhuhr")
                    repository.incrementQaza("Asr")
                    repository.incrementQaza("Maghrib")
                    repository.incrementQaza("Isha")
                } else {
                    // Logs exist, but check if any of the 5 daily ones are still "Pending"
                    for (log in logs) {
                        if (isObligatory(log.prayerName) && log.status == "Pending") {
                            repository.insertOrUpdatePrayerLog(log.copy(status = "Missed/Qaza"))
                            repository.incrementQaza(log.prayerName)
                        }
                    }
                }
                checkDate = checkDate.plusDays(1)
            }
        }
        
        // Update last active date to today
        repository.saveUserPreferences(prefs.copy(lastActiveDate = todayStr))
    }
}
