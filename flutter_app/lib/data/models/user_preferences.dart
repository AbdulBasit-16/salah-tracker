class UserPreferences {
  final int id;
  final String calculationMethod;
  final String juristicMethod;
  final String themeName;
  final double latitude;
  final double longitude;
  final double timezoneOffset;
  final bool fajrNotifEnabled;
  final bool dhuhrNotifEnabled;
  final bool asrNotifEnabled;
  final bool maghribNotifEnabled;
  final bool ishaNotifEnabled;
  final bool missedPrayerRemindersEnabled;
  final int missedPrayerWindowMinutes;
  final bool postSalahRecitationEnabled;
  final int postSalahDelayMinutes;
  final String lastActiveDate;
  final String selectedCity;
  final String quranScript;
  final bool showEnglishTranslation;
  final bool showUrduTranslation;
  final int hijriAdjustment;
  final int currentUserId;
  final String currentUsername;

  UserPreferences({
    this.id = 1,
    this.calculationMethod = "KARACHI",
    this.juristicMethod = "HANAFI",
    this.themeName = "MINIMALIST",
    this.latitude = 21.4225,
    this.longitude = 39.8262,
    this.timezoneOffset = 3.0,
    this.fajrNotifEnabled = true,
    this.dhuhrNotifEnabled = true,
    this.asrNotifEnabled = true,
    this.maghribNotifEnabled = true,
    this.ishaNotifEnabled = true,
    this.missedPrayerRemindersEnabled = true,
    this.missedPrayerWindowMinutes = 45,
    this.postSalahRecitationEnabled = true,
    this.postSalahDelayMinutes = 15,
    this.lastActiveDate = "",
    this.selectedCity = "Custom",
    this.quranScript = "UTHMANI",
    this.showEnglishTranslation = true,
    this.showUrduTranslation = true,
    this.hijriAdjustment = 0,
    this.currentUserId = -1,
    this.currentUsername = "",
  });

  Map<String, dynamic> toMap() {
    return {
      'id': id,
      'calculationMethod': calculationMethod,
      'juristicMethod': juristicMethod,
      'themeName': themeName,
      'latitude': latitude,
      'longitude': longitude,
      'timezoneOffset': timezoneOffset,
      'fajrNotifEnabled': fajrNotifEnabled ? 1 : 0,
      'dhuhrNotifEnabled': dhuhrNotifEnabled ? 1 : 0,
      'asrNotifEnabled': asrNotifEnabled ? 1 : 0,
      'maghribNotifEnabled': maghribNotifEnabled ? 1 : 0,
      'ishaNotifEnabled': ishaNotifEnabled ? 1 : 0,
      'missedPrayerRemindersEnabled': missedPrayerRemindersEnabled ? 1 : 0,
      'missedPrayerWindowMinutes': missedPrayerWindowMinutes,
      'postSalahRecitationEnabled': postSalahRecitationEnabled ? 1 : 0,
      'postSalahDelayMinutes': postSalahDelayMinutes,
      'lastActiveDate': lastActiveDate,
      'selectedCity': selectedCity,
      'quranScript': quranScript,
      'showEnglishTranslation': showEnglishTranslation ? 1 : 0,
      'showUrduTranslation': showUrduTranslation ? 1 : 0,
      'hijriAdjustment': hijriAdjustment,
      'currentUserId': currentUserId,
      'currentUsername': currentUsername,
    };
  }

  factory UserPreferences.fromMap(Map<String, dynamic> map) {
    return UserPreferences(
      id: map['id'],
      calculationMethod: map['calculationMethod'],
      juristicMethod: map['juristicMethod'],
      themeName: map['themeName'],
      latitude: map['latitude'],
      longitude: map['longitude'],
      timezoneOffset: map['timezoneOffset'],
      fajrNotifEnabled: map['fajrNotifEnabled'] == 1,
      dhuhrNotifEnabled: map['dhuhrNotifEnabled'] == 1,
      asrNotifEnabled: map['asrNotifEnabled'] == 1,
      maghribNotifEnabled: map['maghribNotifEnabled'] == 1,
      ishaNotifEnabled: map['ishaNotifEnabled'] == 1,
      missedPrayerRemindersEnabled: map['missedPrayerRemindersEnabled'] == 1,
      missedPrayerWindowMinutes: map['missedPrayerWindowMinutes'],
      postSalahRecitationEnabled: map['postSalahRecitationEnabled'] == 1,
      postSalahDelayMinutes: map['postSalahDelayMinutes'],
      lastActiveDate: map['lastActiveDate'],
      selectedCity: map['selectedCity'],
      quranScript: map['quranScript'],
      showEnglishTranslation: map['showEnglishTranslation'] == 1,
      showUrduTranslation: map['showUrduTranslation'] == 1,
      hijriAdjustment: map['hijriAdjustment'],
      currentUserId: map['currentUserId'],
      currentUsername: map['currentUsername'],
    );
  }
}
