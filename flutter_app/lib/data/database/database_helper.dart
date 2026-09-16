import 'dart:convert';
import 'package:sqflite/sqflite.dart';
import 'package:path/path.dart';
import 'package:flutter/foundation.dart';
import '../models/prayer_log.dart';
import '../models/qaza_counter.dart';
import '../models/quran_log.dart';
import '../models/user.dart';
import '../models/user_preferences.dart';

// Conditional import for web localStorage
import 'web_storage_stub.dart' if (dart.library.html) 'web_storage.dart' as web_storage;

class DatabaseHelper {
  static final DatabaseHelper instance = DatabaseHelper._init();
  static Database? _database;

  DatabaseHelper._init();

  Future<void> init() async {
    if (!kIsWeb) {
      _database = await _initDB('salah_tracker.db');
    }
  }

  Future<Database> get database async {
    if (_database != null) return _database!;
    _database = await _initDB('salah_tracker.db');
    return _database!;
  }

  Future<Database> _initDB(String filePath) async {
    final dbPath = await getDatabasesPath();
    final path = join(dbPath, filePath);
    return await openDatabase(path, version: 1, onCreate: _createDB);
  }

  Future _createDB(Database db, int version) async {
    await db.execute('''
      CREATE TABLE prayer_logs (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        date TEXT NOT NULL,
        prayerName TEXT NOT NULL,
        status TEXT NOT NULL,
        loggedTimestamp INTEGER
      )
    ''');
    await db.execute('''
      CREATE TABLE qaza_counters (
        prayerName TEXT PRIMARY KEY,
        count INTEGER NOT NULL
      )
    ''');
    await db.execute('''
      CREATE TABLE quran_logs (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        timestamp INTEGER NOT NULL,
        surah TEXT NOT NULL,
        startAyah INTEGER NOT NULL,
        endAyah INTEGER NOT NULL,
        startPage INTEGER NOT NULL,
        endPage INTEGER NOT NULL,
        pagesRead INTEGER NOT NULL
      )
    ''');
    await db.execute('''
      CREATE TABLE users (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        username TEXT NOT NULL,
        email TEXT NOT NULL,
        passwordHash TEXT NOT NULL,
        createdAt INTEGER NOT NULL
      )
    ''');
    await db.execute('''
      CREATE TABLE user_preferences (
        id INTEGER PRIMARY KEY,
        calculationMethod TEXT NOT NULL,
        juristicMethod TEXT NOT NULL,
        themeName TEXT NOT NULL,
        latitude REAL NOT NULL,
        longitude REAL NOT NULL,
        timezoneOffset REAL NOT NULL,
        fajrNotifEnabled INTEGER NOT NULL,
        dhuhrNotifEnabled INTEGER NOT NULL,
        asrNotifEnabled INTEGER NOT NULL,
        maghribNotifEnabled INTEGER NOT NULL,
        ishaNotifEnabled INTEGER NOT NULL,
        missedPrayerRemindersEnabled INTEGER NOT NULL,
        missedPrayerWindowMinutes INTEGER NOT NULL,
        postSalahRecitationEnabled INTEGER NOT NULL,
        postSalahDelayMinutes INTEGER NOT NULL,
        lastActiveDate TEXT NOT NULL,
        selectedCity TEXT NOT NULL,
        quranScript TEXT NOT NULL,
        showEnglishTranslation INTEGER NOT NULL,
        showUrduTranslation INTEGER NOT NULL,
        hijriAdjustment INTEGER NOT NULL,
        currentUserId INTEGER NOT NULL,
        currentUsername TEXT NOT NULL
      )
    ''');
  }

  // --- PrayerLog Methods ---
  Future<PrayerLog> insertPrayerLog(PrayerLog log) async {
    if (kIsWeb) {
      final logs = _getWebList<PrayerLog>('prayer_logs', PrayerLog.fromMap);
      final newId = logs.isEmpty ? 1 : (logs.last.id ?? 0) + 1;
      final newLog = PrayerLog(
        id: newId, date: log.date,
        prayerName: log.prayerName, status: log.status,
        loggedTimestamp: log.loggedTimestamp,
      );
      logs.add(newLog);
      _saveWebList('prayer_logs', logs.map((l) => l.toMap()).toList());
      return newLog;
    } else {
      final db = await instance.database;
      final id = await db.insert('prayer_logs', log.toMap());
      return PrayerLog(id: id, date: log.date, prayerName: log.prayerName,
          status: log.status, loggedTimestamp: log.loggedTimestamp);
    }
  }

  Future<List<PrayerLog>> getPrayerLogsByDate(String date) async {
    if (kIsWeb) {
      final logs = _getWebList<PrayerLog>('prayer_logs', PrayerLog.fromMap);
      return logs.where((l) => l.date == date).toList();
    } else {
      final db = await instance.database;
      final maps = await db.query('prayer_logs', where: 'date = ?', whereArgs: [date]);
      return maps.map((map) => PrayerLog.fromMap(map)).toList();
    }
  }

  Future<void> updatePrayerLog(PrayerLog log) async {
    if (kIsWeb) {
      final logs = _getWebList<PrayerLog>('prayer_logs', PrayerLog.fromMap);
      final index = logs.indexWhere((l) => l.id == log.id);
      if (index != -1) {
        logs[index] = log;
        _saveWebList('prayer_logs', logs.map((l) => l.toMap()).toList());
      }
    } else {
      final db = await instance.database;
      await db.update('prayer_logs', log.toMap(), where: 'id = ?', whereArgs: [log.id]);
    }
  }

  // --- QazaCounter Methods ---
  Future<void> updateQazaCounter(QazaCounter counter) async {
    if (kIsWeb) {
      final counters = _getWebList<QazaCounter>('qaza_counters', QazaCounter.fromMap);
      final index = counters.indexWhere((c) => c.prayerName == counter.prayerName);
      if (index != -1) { counters[index] = counter; } else { counters.add(counter); }
      _saveWebList('qaza_counters', counters.map((c) => c.toMap()).toList());
    } else {
      final db = await instance.database;
      await db.insert('qaza_counters', counter.toMap(), conflictAlgorithm: ConflictAlgorithm.replace);
    }
  }

  Future<List<QazaCounter>> getAllQazaCounters() async {
    if (kIsWeb) {
      return _getWebList<QazaCounter>('qaza_counters', QazaCounter.fromMap);
    } else {
      final db = await instance.database;
      final maps = await db.query('qaza_counters');
      return maps.map((map) => QazaCounter.fromMap(map)).toList();
    }
  }

  // --- UserPreferences Methods ---
  Future<void> updateUserPreferences(UserPreferences prefs) async {
    if (kIsWeb) {
      web_storage.setItem('user_preferences', jsonEncode(prefs.toMap()));
    } else {
      final db = await instance.database;
      await db.insert('user_preferences', prefs.toMap(), conflictAlgorithm: ConflictAlgorithm.replace);
    }
  }

  Future<UserPreferences?> getUserPreferences() async {
    if (kIsWeb) {
      final str = web_storage.getItem('user_preferences');
      if (str == null) return null;
      return UserPreferences.fromMap(jsonDecode(str));
    } else {
      final db = await instance.database;
      final maps = await db.query('user_preferences', where: 'id = 1');
      if (maps.isNotEmpty) return UserPreferences.fromMap(maps.first);
      return null;
    }
  }

  // --- Web helpers using localStorage ---
  List<T> _getWebList<T>(String key, T Function(Map<String, dynamic>) fromMap) {
    final str = web_storage.getItem(key);
    if (str == null) return [];
    final List<dynamic> jsonList = jsonDecode(str);
    return jsonList.map((map) => fromMap(Map<String, dynamic>.from(map))).toList();
  }

  void _saveWebList(String key, List<Map<String, dynamic>> list) {
    web_storage.setItem(key, jsonEncode(list));
  }

  Future<void> close() async {
    if (!kIsWeb) {
      final db = await instance.database;
      db.close();
    }
  }
}
