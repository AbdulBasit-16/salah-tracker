import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:intl/intl.dart';
import 'dart:math' as math;
import 'package:hijri/hijri_calendar.dart';
import '../../providers/salah_provider.dart';
import '../../data/models/prayer_log.dart';

class DashboardScreen extends StatelessWidget {
  const DashboardScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFF121212),
      appBar: AppBar(
        backgroundColor: const Color(0xFF121212),
        elevation: 0,
        title: Consumer<SalahProvider>(
          builder: (context, provider, child) {
            final city = provider.userPreferences.selectedCity.isEmpty ? "Makkah" : provider.userPreferences.selectedCity;
            return Column(
              children: [
                Row(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    const Icon(Icons.place, color: Color(0xFF3A9AD9), size: 18),
                    const SizedBox(width: 4),
                    Text(city, style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 18, color: Colors.white)),
                  ],
                ),
                const Text("Location detected automatically", style: TextStyle(fontSize: 12, color: Colors.white54)),
              ],
            );
          },
        ),
        centerTitle: true,
      ),
      body: Consumer<SalahProvider>(
        builder: (context, provider, child) {
          if (provider.errorMessage != null) {
            return Center(child: Text(provider.errorMessage!, style: const TextStyle(color: Colors.red)));
          }

          final todayLogs = provider.todayPrayerLogs;
          final obligatoryLogs = todayLogs.where((l) => ["Fajr", "Dhuhr", "Asr", "Maghrib", "Isha"].contains(l.prayerName)).toList();
          final optionalLogs = todayLogs.where((l) => ["Tahajjud", "Sunnah", "Nafl"].contains(l.prayerName)).toList();

          return ListView(
            padding: const EdgeInsets.all(16),
            children: [
              // Greeting
              const Text("Salam, Guest", style: TextStyle(fontSize: 24, fontWeight: FontWeight.bold, color: Colors.white)),
              const SizedBox(height: 16),

              // Analog Clock
              AspectRatio(
                aspectRatio: 1,
                child: CustomPaint(
                  painter: _AnalogClockPainter(
                    currentTime: DateTime.now(),
                    primaryColor: const Color(0xFF3A9AD9),
                  ),
                ),
              ),

              // Countdown
              if (provider.nextPrayerName != null) ...[
                const SizedBox(height: 16),
                Center(
                  child: Column(
                    children: [
                      Text(provider.nextPrayerName!, style: const TextStyle(fontSize: 18, fontWeight: FontWeight.bold, color: Colors.white)),
                      const SizedBox(height: 4),
                      Text("- ${provider.timeLeftStr}", style: const TextStyle(fontSize: 34, fontWeight: FontWeight.bold, fontFamily: 'monospace', color: Colors.white, letterSpacing: 1)),
                    ],
                  ),
                ),
              ],

              const SizedBox(height: 24),

              // Date
              Center(
                child: Column(
                  children: [
                    Text(DateFormat('EEEE, d MMMM yyyy').format(provider.currentDate), style: const TextStyle(color: Colors.white70, fontWeight: FontWeight.w600)),
                    const SizedBox(height: 4),
                    Container(
                      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 4),
                      decoration: BoxDecoration(
                        color: const Color(0xFF3A9AD9).withOpacity(0.12),
                        borderRadius: BorderRadius.circular(12),
                      ),
                      child: Builder(
                        builder: (context) {
                          final hDate = HijriCalendar.fromDate(provider.currentDate);
                          return Text("${hDate.hDay} ${hDate.getLongMonthName()} ${hDate.hYear} AH", style: const TextStyle(color: Color(0xFF3A9AD9), fontWeight: FontWeight.bold));
                        }
                      ),
                    ),
                  ],
                ),
              ),

              const SizedBox(height: 24),

              // Obligatory Prayers
              const Text("Obligatory Prayers", style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold, color: Color(0xFF3A9AD9))),
              const SizedBox(height: 8),
              Card(
                color: const Color(0xFF1E1E1E),
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
                child: Column(
                  children: obligatoryLogs.map((log) => _PrayerRow(log: log, provider: provider)).toList(),
                ),
              ),

              const SizedBox(height: 24),

              // Optional Prayers
              const Text("Optional Prayers", style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold, color: Color(0xFF3A9AD9))),
              const SizedBox(height: 8),
              Card(
                color: const Color(0xFF1E1E1E),
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
                child: Column(
                  children: optionalLogs.map((log) => _PrayerRow(log: log, provider: provider)).toList(),
                ),
              ),
            ],
          );
        },
      ),
    );
  }
}

class _PrayerRow extends StatelessWidget {
  final PrayerLog log;
  final SalahProvider provider;

  const _PrayerRow({required this.log, required this.provider});

  @override
  Widget build(BuildContext context) {
    Color badgeColor;
    Color textColor;
    String textVal;
    IconData icon;

    switch (log.status) {
      case "Offered On-Time":
        badgeColor = Colors.teal.withOpacity(0.15);
        textColor = Colors.teal;
        textVal = "On-Time";
        icon = Icons.done_all;
        break;
      case "Offered Late":
        badgeColor = Colors.orange.withOpacity(0.15);
        textColor = Colors.orange;
        textVal = "Late";
        icon = Icons.done;
        break;
      case "Missed/Qaza":
        badgeColor = Colors.grey.withOpacity(0.15);
        textColor = Colors.grey;
        textVal = "Qaza";
        icon = Icons.close;
        break;
      case "Excused":
        badgeColor = Colors.blue.withOpacity(0.15);
        textColor = Colors.blue;
        textVal = "Excused";
        icon = Icons.block;
        break;
      default:
        badgeColor = Colors.transparent;
        textColor = Colors.white54;
        textVal = "Pending";
        icon = Icons.access_time;
    }

    return InkWell(
      onTap: () {
        if (!provider.isPrayerTimePassed(log.prayerName)) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(
              content: Text("You cannot log ${log.prayerName} before its Adhan time."),
              backgroundColor: Colors.orange,
              duration: const Duration(seconds: 2),
            ),
          );
          return;
        }

        showDialog(
          context: context,
          builder: (context) => AlertDialog(
            backgroundColor: const Color(0xFF1E1E1E),
            title: Text("Update ${log.prayerName}", style: const TextStyle(color: Colors.white)),
            content: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                _StatusButton("Offered On-Time", Colors.teal, () { provider.updatePrayerStatus(log, "Offered On-Time"); Navigator.pop(context); }),
                _StatusButton("Offered Late", Colors.orange, () { provider.updatePrayerStatus(log, "Offered Late"); Navigator.pop(context); }),
                _StatusButton("Missed (Increments Qaza)", Colors.grey, () { provider.updatePrayerStatus(log, "Missed/Qaza"); Navigator.pop(context); }),
                _StatusButton("Excused", Colors.blue, () { provider.updatePrayerStatus(log, "Excused"); Navigator.pop(context); }),
                _StatusButton("Mark Pending", Colors.white54, () { provider.updatePrayerStatus(log, "Pending"); Navigator.pop(context); }),
              ],
            ),
          ),
        );
      },
      child: Padding(
        padding: const EdgeInsets.symmetric(vertical: 16, horizontal: 16),
        child: Row(
          children: [
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(log.prayerName, style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16, color: Colors.white)),
                  // Time would go here
                ],
              ),
            ),
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
              decoration: BoxDecoration(
                color: badgeColor,
                borderRadius: BorderRadius.circular(8),
              ),
              child: Row(
                children: [
                  Icon(icon, color: textColor, size: 16),
                  const SizedBox(width: 6),
                  Text(textVal, style: TextStyle(color: textColor, fontWeight: FontWeight.bold)),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _StatusButton extends StatelessWidget {
  final String label;
  final Color color;
  final VoidCallback onTap;

  const _StatusButton(this.label, this.color, this.onTap);

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 8.0),
      child: OutlinedButton(
        onPressed: onTap,
        style: OutlinedButton.styleFrom(
          foregroundColor: color,
          side: BorderSide(color: color),
          minimumSize: const Size.fromHeight(48),
        ),
        child: Text(label),
      ),
    );
  }
}

class _AnalogClockPainter extends CustomPainter {
  final DateTime currentTime;
  final Color primaryColor;

  _AnalogClockPainter({required this.currentTime, required this.primaryColor});

  @override
  void paint(Canvas canvas, Size size) {
    final center = Offset(size.width / 2, size.height / 2);
    final radius = math.min(size.width / 2, size.height / 2);

    // Background
    final bgPaint = Paint()
      ..shader = RadialGradient(
        colors: [const Color(0xFF1E1E1E), const Color(0xFF121212)],
      ).createShader(Rect.fromCircle(center: center, radius: radius));
    canvas.drawCircle(center, radius, bgPaint);

    // Ticks
    final tickPaint = Paint()..strokeWidth = 1;
    for (int i = 0; i < 60; i++) {
      final angle = i * 6 * math.pi / 180;
      final isMajor = i % 5 == 0;
      final tickLength = isMajor ? 12.0 : 6.0;
      tickPaint.color = isMajor ? primaryColor : Colors.white24;
      tickPaint.strokeWidth = isMajor ? 3 : 1;
      
      final startX = center.dx + (radius - tickLength) * math.cos(angle);
      final startY = center.dy + (radius - tickLength) * math.sin(angle);
      final endX = center.dx + radius * math.cos(angle);
      final endY = center.dy + radius * math.sin(angle);
      
      canvas.drawLine(Offset(startX, startY), Offset(endX, endY), tickPaint);
    }

    // Inner circle
    canvas.drawCircle(center, radius * 0.76, Paint()..color = const Color(0xFF161616));

    // Hands
    final hourAngle = (currentTime.hour % 12 + currentTime.minute / 60) * 30 * math.pi / 180 - math.pi / 2;
    final minuteAngle = (currentTime.minute * 6) * math.pi / 180 - math.pi / 2;
    final secondAngle = (currentTime.second * 6) * math.pi / 180 - math.pi / 2;

    // Hour hand
    final hourPaint = Paint()..color = Colors.white..strokeWidth = 5..strokeCap = StrokeCap.round;
    canvas.drawLine(center, Offset(center.dx + radius * 0.42 * math.cos(hourAngle), center.dy + radius * 0.42 * math.sin(hourAngle)), hourPaint);

    // Minute hand
    final minutePaint = Paint()..color = primaryColor..strokeWidth = 3.5..strokeCap = StrokeCap.round;
    canvas.drawLine(center, Offset(center.dx + radius * 0.60 * math.cos(minuteAngle), center.dy + radius * 0.60 * math.sin(minuteAngle)), minutePaint);

    // Second hand
    final secondPaint = Paint()..color = primaryColor.withOpacity(0.8)..strokeWidth = 1.5..strokeCap = StrokeCap.round;
    canvas.drawLine(center, Offset(center.dx + radius * 0.68 * math.cos(secondAngle), center.dy + radius * 0.68 * math.sin(secondAngle)), secondPaint);

    // Center dot
    canvas.drawCircle(center, 5, Paint()..color = Colors.white);
  }

  @override
  bool shouldRepaint(covariant CustomPainter oldDelegate) => true;
}
