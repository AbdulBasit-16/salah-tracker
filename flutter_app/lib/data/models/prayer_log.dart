class PrayerLog {
  final int? id;
  final String date;
  final String prayerName;
  final String status;
  final int? loggedTimestamp;

  PrayerLog({
    this.id,
    required this.date,
    required this.prayerName,
    required this.status,
    this.loggedTimestamp,
  });

  Map<String, dynamic> toMap() {
    return {
      'id': id,
      'date': date,
      'prayerName': prayerName,
      'status': status,
      'loggedTimestamp': loggedTimestamp,
    };
  }

  factory PrayerLog.fromMap(Map<String, dynamic> map) {
    return PrayerLog(
      id: map['id'],
      date: map['date'],
      prayerName: map['prayerName'],
      status: map['status'],
      loggedTimestamp: map['loggedTimestamp'],
    );
  }
}
