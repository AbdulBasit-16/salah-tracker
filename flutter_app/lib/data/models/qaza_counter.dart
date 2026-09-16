class QazaCounter {
  final String prayerName;
  final int count;

  QazaCounter({
    required this.prayerName,
    this.count = 0,
  });

  Map<String, dynamic> toMap() {
    return {
      'prayerName': prayerName,
      'count': count,
    };
  }

  factory QazaCounter.fromMap(Map<String, dynamic> map) {
    return QazaCounter(
      prayerName: map['prayerName'],
      count: map['count'],
    );
  }
}
