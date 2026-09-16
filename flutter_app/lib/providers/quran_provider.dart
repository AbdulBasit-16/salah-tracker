import 'package:flutter/material.dart';
import 'dart:convert';
import 'package:flutter/services.dart' show rootBundle;
import '../data/database/database_helper.dart';
import '../data/models/quran_log.dart';
import '../data/models/user_preferences.dart';

class Verse {
  final int chapter;
  final int verse;
  final String arabicText;
  final String? englishText;
  final String? urduText;

  Verse({
    required this.chapter,
    required this.verse,
    required this.arabicText,
    this.englishText,
    this.urduText,
  });
}

class JuzProgressState {
  final int juzNumber;
  final double completionPercentage;
  final int startPage;
  final int endPage;

  JuzProgressState({
    required this.juzNumber,
    required this.completionPercentage,
    required this.startPage,
    required this.endPage,
  });
}

class QuranProvider extends ChangeNotifier {
  final DatabaseHelper _dbHelper = DatabaseHelper.instance;

  final List<List<int>> juzPageRanges = [
    [1, 21], [22, 41], [42, 61], [62, 81], [82, 101],
    [102, 121], [122, 141], [142, 161], [162, 181], [182, 201],
    [202, 221], [222, 241], [242, 261], [262, 281], [282, 301],
    [302, 321], [322, 341], [342, 361], [362, 381], [382, 401],
    [402, 421], [422, 441], [442, 461], [462, 481], [482, 501],
    [502, 521], [522, 541], [542, 561], [562, 581], [582, 604]
  ];

  List<QuranLog> _quranLogs = [];
  List<QuranLog> get quranLogs => _quranLogs;

  List<QuranLog> get recentQuranLogs => _quranLogs.take(5).toList();

  int get currentReadPage {
    if (_quranLogs.isEmpty) return 0;
    return _quranLogs.map((l) => l.endPage).reduce((a, b) => a > b ? a : b);
  }

  double get overallProgressPercentage {
    return (currentReadPage / 604.0 * 100).clamp(0.0, 100.0);
  }

  JuzProgressState get currentJuzState => _calculateJuzProgress(currentReadPage);

  UserPreferences _preferences = UserPreferences();
  UserPreferences get preferences => _preferences;

  List<Verse> _activeSurahVerses = [];
  List<Verse> get activeSurahVerses => _activeSurahVerses;

  bool _isLoadingSurah = false;
  bool get isLoadingSurah => _isLoadingSurah;

  QuranProvider() {
    _init();
  }

  Future<void> _init() async {
    await _loadPreferences();
    await _loadLogs();
  }

  Future<void> _loadPreferences() async {
    final prefs = await _dbHelper.getUserPreferences();
    if (prefs != null) {
      _preferences = prefs;
      notifyListeners();
    }
  }

  Future<void> _loadLogs() async {
    // We don't have a direct method for getting all quran logs in DatabaseHelper yet,
    // but we can add it or just use a dummy list for now if it's not implemented.
    // Let's assume we will add getAllQuranLogs to DatabaseHelper.
    try {
      // _quranLogs = await _dbHelper.getAllQuranLogs();
      _quranLogs = []; // Placeholder until we add the method to DatabaseHelper
      _quranLogs.sort((a, b) => b.timestamp.compareTo(a.timestamp));
    } catch (e) {
      _quranLogs = [];
    }
    notifyListeners();
  }

  Future<void> loadSurahVerses(
    BuildContext context,
    int surahId,
    String scriptType,
    bool showEnglish,
    bool showUrdu,
  ) async {
    _isLoadingSurah = true;
    notifyListeners();

    try {
      final scriptAsset = scriptType == "INDOPAK" ? "assets/quran_indopak.json" : "assets/quran.json";
      final scriptString = await rootBundle.loadString(scriptAsset);
      final scriptObj = json.decode(scriptString) as Map<String, dynamic>;
      final scriptArray = scriptObj[surahId.toString()] as List<dynamic>?;

      List<dynamic>? englishArray;
      if (showEnglish) {
        final enStr = await rootBundle.loadString("assets/translation_en.json");
        final englishObj = json.decode(enStr) as Map<String, dynamic>;
        englishArray = englishObj[surahId.toString()] as List<dynamic>?;
      }

      List<dynamic>? urduArray;
      if (showUrdu) {
        final urStr = await rootBundle.loadString("assets/translation_ur.json");
        final urduObj = json.decode(urStr) as Map<String, dynamic>;
        urduArray = urduObj[surahId.toString()] as List<dynamic>?;
      }

      final versesList = <Verse>[];
      if (scriptArray != null) {
        for (int i = 0; i < scriptArray.length; i++) {
          final verseObj = scriptArray[i] as Map<String, dynamic>;
          final chapter = verseObj["chapter"] as int;
          final verseNum = verseObj["verse"] as int;
          final arabicText = verseObj["text"] as String;

          String? englishText;
          if (englishArray != null && i < englishArray.length) {
            englishText = (englishArray[i] as Map<String, dynamic>)["text"] as String?;
          }

          String? urduText;
          if (urduArray != null && i < urduArray.length) {
            urduText = (urduArray[i] as Map<String, dynamic>)["text"] as String?;
          }

          versesList.add(Verse(
            chapter: chapter,
            verse: verseNum,
            arabicText: arabicText,
            englishText: englishText,
            urduText: urduText,
          ));
        }
      }
      _activeSurahVerses = versesList;
    } catch (e) {
      print("Error loading surah: $e");
      _activeSurahVerses = [];
    } finally {
      _isLoadingSurah = false;
      notifyListeners();
    }
  }

  Future<void> updateQuranPreferences(String script, bool showEnglish, bool showUrdu) async {
    _preferences = UserPreferences(
      id: _preferences.id,
      calculationMethod: _preferences.calculationMethod,
      juristicMethod: _preferences.juristicMethod,
      latitude: _preferences.latitude,
      longitude: _preferences.longitude,
      timezoneOffset: _preferences.timezoneOffset,
      selectedCity: _preferences.selectedCity,
      fajrNotifEnabled: _preferences.fajrNotifEnabled,
      dhuhrNotifEnabled: _preferences.dhuhrNotifEnabled,
      asrNotifEnabled: _preferences.asrNotifEnabled,
      maghribNotifEnabled: _preferences.maghribNotifEnabled,
      ishaNotifEnabled: _preferences.ishaNotifEnabled,
      missedPrayerRemindersEnabled: _preferences.missedPrayerRemindersEnabled,
      postSalahRecitationEnabled: _preferences.postSalahRecitationEnabled,
      themeName: _preferences.themeName,
      quranScript: script,
      showEnglishTranslation: showEnglish,
      showUrduTranslation: showUrdu,
      hijriAdjustment: _preferences.hijriAdjustment,
      lastActiveDate: _preferences.lastActiveDate,
    );
    await _dbHelper.updateUserPreferences(_preferences);
    notifyListeners();
  }

  Future<void> logRecitation(String surah, int startAyah, int endAyah, int startPage, int endPage) async {
    final pagesCount = (endPage - startPage + 1).clamp(1, 604);
    final log = QuranLog(
      id: DateTime.now().millisecondsSinceEpoch,
      timestamp: DateTime.now().millisecondsSinceEpoch,
      surah: surah,
      startAyah: startAyah,
      endAyah: endAyah,
      startPage: startPage,
      endPage: endPage,
      pagesRead: pagesCount,
    );
    // await _dbHelper.insertQuranLog(log);
    _quranLogs.insert(0, log);
    notifyListeners();
  }

  Future<void> deleteLog(QuranLog log) async {
    // await _dbHelper.deleteQuranLog(log.id!);
    _quranLogs.removeWhere((l) => l.id == log.id);
    notifyListeners();
  }

  JuzProgressState _calculateJuzProgress(int page) {
    if (page <= 0) return JuzProgressState(juzNumber: 1, completionPercentage: 0.0, startPage: 1, endPage: 21);
    if (page >= 604) return JuzProgressState(juzNumber: 30, completionPercentage: 100.0, startPage: 582, endPage: 604);

    for (int i = 0; i < juzPageRanges.length; i++) {
      final start = juzPageRanges[i][0];
      final end = juzPageRanges[i][1];
      if (page >= start && page <= end) {
        final juzNum = i + 1;
        final totalJuzPages = end - start + 1;
        final pagesReadInJuz = page - start + 1;
        final percentage = (pagesReadInJuz / totalJuzPages * 100.0).clamp(0.0, 100.0);
        return JuzProgressState(juzNumber: juzNum, completionPercentage: percentage, startPage: start, endPage: end);
      }
    }
    return JuzProgressState(juzNumber: 1, completionPercentage: 0.0, startPage: 1, endPage: 21);
  }
}
