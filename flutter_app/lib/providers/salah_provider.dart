import 'dart:async';
import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import '../data/calculator/prayer_time_calculator.dart';
import '../data/database/database_helper.dart';
import '../data/models/prayer_log.dart';
import '../data/models/qaza_counter.dart';
import '../data/models/user_preferences.dart';
import 'package:geolocator/geolocator.dart';
import 'package:geocoding/geocoding.dart';
import 'package:home_widget/home_widget.dart';
import '../services/notification_service.dart';

class SalahProvider extends ChangeNotifier {
  final DatabaseHelper _dbHelper = DatabaseHelper.instance;
  final PrayerTimeCalculator _calculator = PrayerTimeCalculator();

  UserPreferences _userPreferences = UserPreferences();
  UserPreferences get userPreferences => _userPreferences;

  DateTime _currentDate = DateTime.now();
  DateTime get currentDate => _currentDate;

  List<PrayerLog> _todayPrayerLogs = [];
  List<PrayerLog> get todayPrayerLogs => _todayPrayerLogs;

  List<PrayerLog> _last7DaysPrayerLogs = [];
  List<PrayerLog> get last7DaysPrayerLogs => _last7DaysPrayerLogs;

  PrayerTimes? _todayPrayerTimes;
  PrayerTimes? get todayPrayerTimes => _todayPrayerTimes;

  List<QazaCounter> _qazaBalances = [];
  List<QazaCounter> get qazaBalances => _qazaBalances;

  String? _nextPrayerName;
  String? get nextPrayerName => _nextPrayerName;

  String? _timeLeftStr;
  String? get timeLeftStr => _timeLeftStr;

  String? _errorMessage;
  String? get errorMessage => _errorMessage;

  Timer? _timer;

  SalahProvider() {
    _init();
  }

  Future<void> _init() async {
    try {
      await _loadUserPreferences();
      await _loadDataForDate(_currentDate);
      await _loadQazaBalances();
      _startCountdownTicker();
      await _checkAndProcessMissedDays();
      // Try to fetch location if permissions are already granted
      await fetchCurrentLocation();
    } catch (e) {
      _errorMessage = e.toString();
      notifyListeners();
    }
  }

  Future<void> _loadUserPreferences() async {
    final prefs = await _dbHelper.getUserPreferences();
    if (prefs != null) {
      _userPreferences = prefs;
    } else {
      await _dbHelper.updateUserPreferences(_userPreferences);
    }
    notifyListeners();
  }

  Future<void> _loadDataForDate(DateTime date) async {
    final dateStr = DateFormat('yyyy-MM-dd').format(date);
    var logs = await _dbHelper.getPrayerLogsByDate(dateStr);
    
    if (logs.isEmpty) {
      await _initializePrayerLogsForDate(dateStr);
      logs = await _dbHelper.getPrayerLogsByDate(dateStr);
    }
    _todayPrayerLogs = logs;
    _calculateTimesForDate(date);
    notifyListeners();
  }

  Future<void> _initializePrayerLogsForDate(String dateStr) async {
    final defaultLogs = [
      PrayerLog(date: dateStr, prayerName: "Fajr", status: "Pending"),
      PrayerLog(date: dateStr, prayerName: "Dhuhr", status: "Pending"),
      PrayerLog(date: dateStr, prayerName: "Asr", status: "Pending"),
      PrayerLog(date: dateStr, prayerName: "Maghrib", status: "Pending"),
      PrayerLog(date: dateStr, prayerName: "Isha", status: "Pending"),
      PrayerLog(date: dateStr, prayerName: "Tahajjud", status: "Pending"),
      PrayerLog(date: dateStr, prayerName: "Sunnah", status: "Pending"),
      PrayerLog(date: dateStr, prayerName: "Nafl", status: "Pending"),
    ];
    for (var log in defaultLogs) {
      await _dbHelper.insertPrayerLog(log);
    }
  }

  void _calculateTimesForDate(DateTime date) {
    CalculationMethod method = CalculationMethod.values.firstWhere(
      (e) => e.name.toUpperCase() == _userPreferences.calculationMethod,
      orElse: () => CalculationMethod.mwl,
    );
    JuristicMethod juristic = JuristicMethod.values.firstWhere(
      (e) => e.name.toUpperCase() == _userPreferences.juristicMethod,
      orElse: () => JuristicMethod.standard,
    );

    _todayPrayerTimes = _calculator.calculate(
      latitude: _userPreferences.latitude,
      longitude: _userPreferences.longitude,
      timezoneOffset: _userPreferences.timezoneOffset,
      year: date.year,
      month: date.month,
      day: date.day,
      method: method,
      juristic: juristic,
    );
    
    // Only schedule if it's calculating for today
    if (date.year == DateTime.now().year && date.month == DateTime.now().month && date.day == DateTime.now().day) {
      _scheduleUpcomingAlarms();
    }
  }

  Future<void> fetchCurrentLocation() async {
    try {
      LocationPermission permission = await Geolocator.checkPermission();
      if (permission == LocationPermission.always || permission == LocationPermission.whileInUse) {
        Position position = await Geolocator.getCurrentPosition(desiredAccuracy: LocationAccuracy.low);
        List<Placemark> placemarks = await placemarkFromCoordinates(position.latitude, position.longitude);
        
        String city = "Unknown";
        if (placemarks.isNotEmpty) {
          city = placemarks.first.locality ?? placemarks.first.subAdministrativeArea ?? placemarks.first.administrativeArea ?? "Unknown";
        }

        _userPreferences = UserPreferences(
          id: _userPreferences.id,
          calculationMethod: _userPreferences.calculationMethod,
          juristicMethod: _userPreferences.juristicMethod,
          latitude: position.latitude,
          longitude: position.longitude,
          timezoneOffset: _userPreferences.timezoneOffset,
          selectedCity: city,
          fajrNotifEnabled: _userPreferences.fajrNotifEnabled,
          dhuhrNotifEnabled: _userPreferences.dhuhrNotifEnabled,
          asrNotifEnabled: _userPreferences.asrNotifEnabled,
          maghribNotifEnabled: _userPreferences.maghribNotifEnabled,
          ishaNotifEnabled: _userPreferences.ishaNotifEnabled,
          missedPrayerRemindersEnabled: _userPreferences.missedPrayerRemindersEnabled,
          postSalahRecitationEnabled: _userPreferences.postSalahRecitationEnabled,
          themeName: _userPreferences.themeName,
          quranScript: _userPreferences.quranScript,
          showEnglishTranslation: _userPreferences.showEnglishTranslation,
          showUrduTranslation: _userPreferences.showUrduTranslation,
          hijriAdjustment: _userPreferences.hijriAdjustment,
          lastActiveDate: _userPreferences.lastActiveDate,
        );
        
        await _dbHelper.updateUserPreferences(_userPreferences);
        _calculateTimesForDate(_currentDate);
        notifyListeners();
      }
    } catch (e) {
      print("Could not fetch location: \$e");
    }
  }

  Future<void> _scheduleUpcomingAlarms() async {
    List<Map<String, dynamic>> alarms = [];
    DateTime now = DateTime.now();
    
    CalculationMethod method = CalculationMethod.values.firstWhere(
      (e) => e.name.toUpperCase() == _userPreferences.calculationMethod,
      orElse: () => CalculationMethod.mwl,
    );
    JuristicMethod juristic = JuristicMethod.values.firstWhere(
      (e) => e.name.toUpperCase() == _userPreferences.juristicMethod,
      orElse: () => JuristicMethod.standard,
    );

    // Schedule for the next 7 days
    for (int i = 0; i < 7; i++) {
      DateTime targetDate = now.add(Duration(days: i));
      PrayerTimes pt = _calculator.calculate(
        latitude: _userPreferences.latitude,
        longitude: _userPreferences.longitude,
        timezoneOffset: _userPreferences.timezoneOffset,
        year: targetDate.year,
        month: targetDate.month,
        day: targetDate.day,
        method: method,
        juristic: juristic,
      );

      if (_userPreferences.fajrNotifEnabled) alarms.add({'name': 'Fajr', 'time': DateTime(targetDate.year, targetDate.month, targetDate.day, pt.fajr.hour, pt.fajr.minute)});
      if (_userPreferences.dhuhrNotifEnabled) alarms.add({'name': 'Dhuhr', 'time': DateTime(targetDate.year, targetDate.month, targetDate.day, pt.dhuhr.hour, pt.dhuhr.minute)});
      if (_userPreferences.asrNotifEnabled) alarms.add({'name': 'Asr', 'time': DateTime(targetDate.year, targetDate.month, targetDate.day, pt.asr.hour, pt.asr.minute)});
      if (_userPreferences.maghribNotifEnabled) alarms.add({'name': 'Maghrib', 'time': DateTime(targetDate.year, targetDate.month, targetDate.day, pt.maghrib.hour, pt.maghrib.minute)});
      if (_userPreferences.ishaNotifEnabled) alarms.add({'name': 'Isha', 'time': DateTime(targetDate.year, targetDate.month, targetDate.day, pt.isha.hour, pt.isha.minute)});
    }

    await NotificationService().schedulePrayerAlarms(alarms);
  }

  Future<void> _loadQazaBalances() async {
    _qazaBalances = await _dbHelper.getAllQazaCounters();
    notifyListeners();
  }

  void setDate(DateTime date) {
    _currentDate = date;
    _loadDataForDate(date);
  }

  Future<void> updatePrayerStatus(PrayerLog log, String newStatus) async {
    final oldStatus = log.status;
    final updatedLog = PrayerLog(
      id: log.id,
      date: log.date,
      prayerName: log.prayerName,
      status: newStatus,
      loggedTimestamp: newStatus.startsWith("Offered") ? DateTime.now().millisecondsSinceEpoch : null,
    );

    await _dbHelper.updatePrayerLog(updatedLog);

    if (newStatus == "Missed/Qaza" && oldStatus != "Missed/Qaza") {
      if (_isObligatory(log.prayerName)) {
        await _incrementQaza(log.prayerName);
      }
    }
    if (oldStatus == "Missed/Qaza" && newStatus != "Missed/Qaza") {
      if (_isObligatory(log.prayerName)) {
        await _decrementQaza(log.prayerName);
      }
    }

    // Refresh data
    await _loadDataForDate(_currentDate);
    await _loadQazaBalances();
  }

  bool _isObligatory(String name) {
    return ["Fajr", "Dhuhr", "Asr", "Maghrib", "Isha"].contains(name);
  }

  Future<void> _incrementQaza(String prayerName) async {
    final existing = _qazaBalances.firstWhere(
      (q) => q.prayerName == prayerName,
      orElse: () => QazaCounter(prayerName: prayerName, count: 0),
    );
    await _dbHelper.updateQazaCounter(QazaCounter(
      prayerName: prayerName,
      count: existing.count + 1,
    ));
  }

  Future<void> _decrementQaza(String prayerName) async {
    final existing = _qazaBalances.firstWhere(
      (q) => q.prayerName == prayerName,
      orElse: () => QazaCounter(prayerName: prayerName, count: 0),
    );
    if (existing.count > 0) {
      await _dbHelper.updateQazaCounter(QazaCounter(
        prayerName: prayerName,
        count: existing.count - 1,
      ));
    }
  }

  void _startCountdownTicker() {
    _timer = Timer.periodic(const Duration(seconds: 1), (timer) {
      if (_todayPrayerTimes == null) return;
      
      final now = DateTime.now();
      
      // We need tomorrow's times for Isha -> Fajr wrap around
      final tomorrow = now.add(const Duration(days: 1));
      CalculationMethod method = CalculationMethod.values.firstWhere(
        (e) => e.name.toUpperCase() == _userPreferences.calculationMethod,
        orElse: () => CalculationMethod.mwl,
      );
      JuristicMethod juristic = JuristicMethod.values.firstWhere(
        (e) => e.name.toUpperCase() == _userPreferences.juristicMethod,
        orElse: () => JuristicMethod.standard,
      );
      final tomorrowTimes = _calculator.calculate(
        latitude: _userPreferences.latitude,
        longitude: _userPreferences.longitude,
        timezoneOffset: _userPreferences.timezoneOffset,
        year: tomorrow.year,
        month: tomorrow.month,
        day: tomorrow.day,
        method: method,
        juristic: juristic,
      );

      final prayerList = [
        {"name": "Fajr", "time": DateTime(now.year, now.month, now.day, _todayPrayerTimes!.fajr.hour, _todayPrayerTimes!.fajr.minute)},
        {"name": "Sunrise", "time": DateTime(now.year, now.month, now.day, _todayPrayerTimes!.sunrise.hour, _todayPrayerTimes!.sunrise.minute)},
        {"name": "Dhuhr", "time": DateTime(now.year, now.month, now.day, _todayPrayerTimes!.dhuhr.hour, _todayPrayerTimes!.dhuhr.minute)},
        {"name": "Asr", "time": DateTime(now.year, now.month, now.day, _todayPrayerTimes!.asr.hour, _todayPrayerTimes!.asr.minute)},
        {"name": "Maghrib", "time": DateTime(now.year, now.month, now.day, _todayPrayerTimes!.maghrib.hour, _todayPrayerTimes!.maghrib.minute)},
        {"name": "Isha", "time": DateTime(now.year, now.month, now.day, _todayPrayerTimes!.isha.hour, _todayPrayerTimes!.isha.minute)},
        {"name": "Fajr (Tomorrow)", "time": DateTime(tomorrow.year, tomorrow.month, tomorrow.day, tomorrowTimes.fajr.hour, tomorrowTimes.fajr.minute)},
      ];

      Map<String, dynamic>? nextPrayer;
      for (var p in prayerList) {
        if ((p["time"] as DateTime).isAfter(now)) {
          nextPrayer = p;
          break;
        }
      }

      if (nextPrayer != null) {
        final diff = (nextPrayer["time"] as DateTime).difference(now);
        final h = diff.inHours;
        final m = diff.inMinutes % 60;
        final s = diff.inSeconds % 60;
        _nextPrayerName = nextPrayer["name"] as String;
        _timeLeftStr = "${h.toString().padLeft(2, '0')}:${m.toString().padLeft(2, '0')}:${s.toString().padLeft(2, '0')}";
        notifyListeners();

        // Update home widget every minute to save battery
        if (s == 0 || s == 30) {
           HomeWidget.saveWidgetData<String>('prayer_name', _nextPrayerName!);
           HomeWidget.saveWidgetData<String>('time_left', "${h.toString().padLeft(2, '0')}h ${m.toString().padLeft(2, '0')}m");
           HomeWidget.updateWidget(androidName: 'PrayerWidgetProvider');
        }
      }
    });
  }

  Future<void> _checkAndProcessMissedDays() async {
    final todayStr = DateFormat('yyyy-MM-dd').format(DateTime.now());
    final lastActiveStr = _userPreferences.lastActiveDate;

    if (lastActiveStr.isNotEmpty && lastActiveStr != todayStr) {
      try {
        final lastActive = DateFormat('yyyy-MM-dd').parse(lastActiveStr);
        final today = DateTime.now();
        
        var checkDate = lastActive.add(const Duration(days: 1));
        while (checkDate.isBefore(DateTime(today.year, today.month, today.day))) {
          final checkDateStr = DateFormat('yyyy-MM-dd').format(checkDate);
          final logs = await _dbHelper.getPrayerLogsByDate(checkDateStr);
          
          if (logs.isEmpty) {
            final missedLogs = [
              PrayerLog(date: checkDateStr, prayerName: "Fajr", status: "Missed/Qaza"),
              PrayerLog(date: checkDateStr, prayerName: "Dhuhr", status: "Missed/Qaza"),
              PrayerLog(date: checkDateStr, prayerName: "Asr", status: "Missed/Qaza"),
              PrayerLog(date: checkDateStr, prayerName: "Maghrib", status: "Missed/Qaza"),
              PrayerLog(date: checkDateStr, prayerName: "Isha", status: "Missed/Qaza"),
              PrayerLog(date: checkDateStr, prayerName: "Tahajjud", status: "Pending"),
              PrayerLog(date: checkDateStr, prayerName: "Sunnah", status: "Pending"),
              PrayerLog(date: checkDateStr, prayerName: "Nafl", status: "Pending"),
            ];
            for (var log in missedLogs) {
              await _dbHelper.insertPrayerLog(log);
            }
            
            await _incrementQaza("Fajr");
            await _incrementQaza("Dhuhr");
            await _incrementQaza("Asr");
            await _incrementQaza("Maghrib");
            await _incrementQaza("Isha");
          } else {
            for (var log in logs) {
              if (_isObligatory(log.prayerName) && log.status == "Pending") {
                await _dbHelper.updatePrayerLog(PrayerLog(
                  id: log.id,
                  date: log.date,
                  prayerName: log.prayerName,
                  status: "Missed/Qaza",
                  loggedTimestamp: log.loggedTimestamp,
                ));
                await _incrementQaza(log.prayerName);
              }
            }
          }
          checkDate = checkDate.add(const Duration(days: 1));
        }
      } catch (e) {
        // Ignore parsing errors
      }
    }
    
    _userPreferences = UserPreferences(
      id: _userPreferences.id,
      calculationMethod: _userPreferences.calculationMethod,
      juristicMethod: _userPreferences.juristicMethod,
      latitude: _userPreferences.latitude,
      longitude: _userPreferences.longitude,
      timezoneOffset: _userPreferences.timezoneOffset,
      selectedCity: _userPreferences.selectedCity,
      fajrNotifEnabled: _userPreferences.fajrNotifEnabled,
      dhuhrNotifEnabled: _userPreferences.dhuhrNotifEnabled,
      asrNotifEnabled: _userPreferences.asrNotifEnabled,
      maghribNotifEnabled: _userPreferences.maghribNotifEnabled,
      ishaNotifEnabled: _userPreferences.ishaNotifEnabled,
      missedPrayerRemindersEnabled: _userPreferences.missedPrayerRemindersEnabled,
      postSalahRecitationEnabled: _userPreferences.postSalahRecitationEnabled,
      themeName: _userPreferences.themeName,
      quranScript: _userPreferences.quranScript,
      showEnglishTranslation: _userPreferences.showEnglishTranslation,
      showUrduTranslation: _userPreferences.showUrduTranslation,
      hijriAdjustment: _userPreferences.hijriAdjustment,
      lastActiveDate: todayStr,
    );
    await _dbHelper.updateUserPreferences(_userPreferences);
  }

  bool isPrayerTimePassed(String prayerName) {
    if (_todayPrayerTimes == null || DateFormat('yyyy-MM-dd').format(_currentDate) != DateFormat('yyyy-MM-dd').format(DateTime.now())) {
      return true; // Allow logging for past or future dates selected explicitly
    }
    
    DateTime now = DateTime.now();
    DateTime prayerTime;

    switch (prayerName) {
      case 'Fajr':
        prayerTime = DateTime(now.year, now.month, now.day, _todayPrayerTimes!.fajr.hour, _todayPrayerTimes!.fajr.minute);
        break;
      case 'Dhuhr':
        prayerTime = DateTime(now.year, now.month, now.day, _todayPrayerTimes!.dhuhr.hour, _todayPrayerTimes!.dhuhr.minute);
        break;
      case 'Asr':
        prayerTime = DateTime(now.year, now.month, now.day, _todayPrayerTimes!.asr.hour, _todayPrayerTimes!.asr.minute);
        break;
      case 'Maghrib':
        prayerTime = DateTime(now.year, now.month, now.day, _todayPrayerTimes!.maghrib.hour, _todayPrayerTimes!.maghrib.minute);
        break;
      case 'Isha':
        prayerTime = DateTime(now.year, now.month, now.day, _todayPrayerTimes!.isha.hour, _todayPrayerTimes!.isha.minute);
        break;
      default:
        return true;
    }

    return now.isAfter(prayerTime);
  }

  @override
  void dispose() {
    _timer?.cancel();
    super.dispose();
  }
}
