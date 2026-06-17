package com.salah.tracker.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.salah.tracker.data.database.entities.QuranLog
import com.salah.tracker.data.repository.SalahRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class Verse(
    val chapter: Int,
    val verse: Int,
    val arabicText: String,
    val englishText: String? = null,
    val urduText: String? = null
)

class QuranViewModel(private val repository: SalahRepository) : ViewModel() {

    // 1-indexed Juz page boundaries (Start Page to End Page)
    val juzPageRanges = listOf(
        1 to 21,    // Juz 1
        22 to 41,   // Juz 2
        42 to 61,   // Juz 3
        62 to 81,   // Juz 4
        82 to 101,  // Juz 5
        102 to 121, // Juz 6
        122 to 141, // Juz 7
        142 to 161, // Juz 8
        162 to 181, // Juz 9
        182 to 201, // Juz 10
        202 to 221, // Juz 11
        222 to 241, // Juz 12
        242 to 261, // Juz 13
        262 to 281, // Juz 14
        282 to 301, // Juz 15
        302 to 321, // Juz 16
        322 to 341, // Juz 17
        342 to 361, // Juz 18
        362 to 381, // Juz 19
        382 to 401, // Juz 20
        402 to 421, // Juz 21
        422 to 441, // Juz 22
        442 to 461, // Juz 23
        462 to 481, // Juz 24
        482 to 501, // Juz 25
        502 to 521, // Juz 26
        522 to 541, // Juz 27
        542 to 561, // Juz 28
        562 to 581, // Juz 29
        582 to 604  // Juz 30
    )

    // Complete history of reading logs
    val quranLogs = repository.getAllQuranLogsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Recent 5 logs for simple dashboard previews
    val recentQuranLogs = repository.getRecentQuranLogsFlow(5)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // The highest page number read so far (current progress)
    val currentReadPage = quranLogs.map { logs ->
        logs.maxOfOrNull { it.endPage } ?: 0
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Overall completion percentage (out of 604 pages)
    val overallProgressPercentage = currentReadPage.map { page ->
        (page.toDouble() / 604.0 * 100.0).coerceIn(0.0, 100.0)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Calculated current Juz and percentage completed within that Juz
    val currentJuzState = currentReadPage.map { page ->
        calculateJuzProgress(page)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), JuzProgressState(1, 0.0, 1, 21))

    // User preferences flow
    val preferences = repository.getUserPreferencesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), com.salah.tracker.data.database.entities.UserPreferences())

    // Active Surah verses loaded from assets
    private val _activeSurahVerses = MutableStateFlow<List<Verse>>(emptyList())
    val activeSurahVerses: StateFlow<List<Verse>> = _activeSurahVerses.asStateFlow()

    // Loading status
    private val _isLoadingSurah = MutableStateFlow(false)
    val isLoadingSurah: StateFlow<Boolean> = _isLoadingSurah.asStateFlow()

    fun loadSurahVerses(
        context: Context,
        surahId: Int,
        scriptType: String,
        showEnglish: Boolean,
        showUrdu: Boolean
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoadingSurah.value = true
            try {
                // Determine script asset
                val scriptAsset = if (scriptType == "INDOPAK") "quran_indopak.json" else "quran.json"
                val scriptString = context.assets.open(scriptAsset).bufferedReader().use { it.readText() }
                val scriptObj = org.json.JSONObject(scriptString)
                val scriptArray = scriptObj.optJSONArray(surahId.toString())

                // Load translations if enabled
                val englishObj = if (showEnglish) {
                    val enStr = context.assets.open("translation_en.json").bufferedReader().use { it.readText() }
                    org.json.JSONObject(enStr)
                } else null
                val englishArray = englishObj?.optJSONArray(surahId.toString())

                val urduObj = if (showUrdu) {
                    val urStr = context.assets.open("translation_ur.json").bufferedReader().use { it.readText() }
                    org.json.JSONObject(urStr)
                } else null
                val urduArray = urduObj?.optJSONArray(surahId.toString())

                val versesList = mutableListOf<Verse>()
                if (scriptArray != null) {
                    for (i in 0 until scriptArray.length()) {
                        val verseObj = scriptArray.getJSONObject(i)
                        val chapter = verseObj.optInt("chapter")
                        val verseNum = verseObj.optInt("verse")
                        val arabicText = verseObj.optString("text")

                        val englishText = if (englishArray != null && i < englishArray.length()) {
                            englishArray.getJSONObject(i).optString("text")
                        } else null

                        val urduText = if (urduArray != null && i < urduArray.length()) {
                            urduArray.getJSONObject(i).optString("text")
                        } else null

                        versesList.add(Verse(chapter, verseNum, arabicText, englishText, urduText))
                    }
                }
                _activeSurahVerses.value = versesList
            } catch (e: Exception) {
                e.printStackTrace()
                _activeSurahVerses.value = emptyList()
            } finally {
                _isLoadingSurah.value = false
            }
        }
    }

    fun updateQuranPreferences(script: String, showEnglish: Boolean, showUrdu: Boolean) {
        viewModelScope.launch {
            val current = repository.getUserPreferences() ?: com.salah.tracker.data.database.entities.UserPreferences()
            val updated = current.copy(
                quranScript = script,
                showEnglishTranslation = showEnglish,
                showUrduTranslation = showUrdu
            )
            repository.saveUserPreferences(updated)
        }
    }

    fun logRecitation(surah: String, startAyah: Int, endAyah: Int, startPage: Int, endPage: Int) {
        viewModelScope.launch {
            val pagesCount = (endPage - startPage + 1).coerceAtLeast(1)
            val log = QuranLog(
                timestamp = System.currentTimeMillis(),
                surah = surah,
                startAyah = startAyah,
                endAyah = endAyah,
                startPage = startPage,
                endPage = endPage,
                pagesRead = pagesCount
            )
            repository.insertQuranLog(log)
        }
    }

    fun deleteLog(log: QuranLog) {
        viewModelScope.launch {
            repository.deleteQuranLog(log)
        }
    }

    private fun calculateJuzProgress(page: Int): JuzProgressState {
        if (page <= 0) return JuzProgressState(1, 0.0, 1, 21)
        if (page >= 604) return JuzProgressState(30, 100.0, 582, 604)

        // Find which Juz the current page falls into
        for (i in juzPageRanges.indices) {
            val (start, end) = juzPageRanges[i]
            if (page in start..end) {
                val juzNum = i + 1
                val totalJuzPages = end - start + 1
                val pagesReadInJuz = page - start + 1
                val percentage = (pagesReadInJuz.toDouble() / totalJuzPages.toDouble() * 100.0).coerceIn(0.0, 100.0)
                return JuzProgressState(juzNum, percentage, start, end)
            }
        }
        return JuzProgressState(1, 0.0, 1, 21)
    }

    data class JuzProgressState(
        val juzNumber: Int,
        val completionPercentage: Double,
        val startPage: Int,
        val endPage: Int
    )
}
