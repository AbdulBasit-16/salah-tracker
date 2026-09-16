class QuranLog {
  final int? id;
  final int timestamp;
  final String surah;
  final int startAyah;
  final int endAyah;
  final int startPage;
  final int endPage;
  final int pagesRead;

  QuranLog({
    this.id,
    required this.timestamp,
    required this.surah,
    required this.startAyah,
    required this.endAyah,
    required this.startPage,
    required this.endPage,
    required this.pagesRead,
  });

  Map<String, dynamic> toMap() {
    return {
      'id': id,
      'timestamp': timestamp,
      'surah': surah,
      'startAyah': startAyah,
      'endAyah': endAyah,
      'startPage': startPage,
      'endPage': endPage,
      'pagesRead': pagesRead,
    };
  }

  factory QuranLog.fromMap(Map<String, dynamic> map) {
    return QuranLog(
      id: map['id'],
      timestamp: map['timestamp'],
      surah: map['surah'],
      startAyah: map['startAyah'],
      endAyah: map['endAyah'],
      startPage: map['startPage'],
      endPage: map['endPage'],
      pagesRead: map['pagesRead'],
    );
  }
}
