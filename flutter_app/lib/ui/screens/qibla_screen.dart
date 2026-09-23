import 'package:flutter/material.dart';
import 'dart:math' as math;
import 'package:flutter_compass/flutter_compass.dart';
import 'package:permission_handler/permission_handler.dart';
import 'package:geolocator/geolocator.dart';

class QiblaScreen extends StatefulWidget {
  const QiblaScreen({super.key});

  @override
  State<QiblaScreen> createState() => _QiblaScreenState();
}

class _QiblaScreenState extends State<QiblaScreen> {
  bool _hasPermissions = false;
  bool _isLocationServiceEnabled = true;
  Position? _currentPosition;
  double _qiblaAngle = 0.0;
  bool _isLoadingLocation = false;

  @override
  void initState() {
    super.initState();
    _fetchPermissionStatus();
  }

  void _fetchPermissionStatus() async {
    bool serviceEnabled = await Geolocator.isLocationServiceEnabled();
    PermissionStatus status = await Permission.locationWhenInUse.status;
    
    if (mounted) {
      setState(() {
        _isLocationServiceEnabled = serviceEnabled;
        _hasPermissions = status == PermissionStatus.granted;
      });
      if (_hasPermissions && _isLocationServiceEnabled) {
        _getLocationAndCalculateQibla();
      }
    }
  }

  Future<void> _getLocationAndCalculateQibla() async {
    setState(() => _isLoadingLocation = true);
    try {
      Position? position;
      try {
        position = await Geolocator.getCurrentPosition(
          desiredAccuracy: LocationAccuracy.medium,
          timeLimit: const Duration(seconds: 5),
        );
      } catch (e) {
        position = await Geolocator.getLastKnownPosition();
      }
      
      if (position == null) {
        if (mounted) setState(() => _isLoadingLocation = false);
        return;
      }
      
      // Makkah coordinates
      const double makkahLat = 21.422487;
      const double makkahLng = 39.826206;
      
      double bearing = Geolocator.bearingBetween(
          position.latitude, position.longitude, makkahLat, makkahLng);
          
      if (mounted) {
        setState(() {
          _currentPosition = position;
          _qiblaAngle = bearing * (math.pi / 180);
          _isLoadingLocation = false;
        });
      }
    } catch (e) {
      print("Error getting location: $e");
      if (mounted) {
        setState(() => _isLoadingLocation = false);
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFF121212),
      appBar: AppBar(
        title: const Text('Qibla Compass', style: TextStyle(fontWeight: FontWeight.bold)),
        backgroundColor: const Color(0xFF121212),
        elevation: 0,
        centerTitle: true,
      ),
      body: Builder(builder: (context) {
        if (!_isLocationServiceEnabled) {
          return Center(
            child: Column(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                const Icon(Icons.location_off, size: 64, color: Colors.white54),
                const SizedBox(height: 16),
                const Text("GPS is disabled", style: TextStyle(color: Colors.white, fontSize: 18)),
                const SizedBox(height: 8),
                const Text("Please enable location services to use the compass.", style: TextStyle(color: Colors.white70)),
                const SizedBox(height: 24),
                ElevatedButton(
                  child: const Text('Open Location Settings'),
                  onPressed: () async {
                    await Geolocator.openLocationSettings();
                    _fetchPermissionStatus();
                  },
                ),
              ],
            ),
          );
        }

        if (_hasPermissions) {
          if (_isLoadingLocation) {
            return const Center(
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  CircularProgressIndicator(),
                  SizedBox(height: 16),
                  Text("Locating Qibla...", style: TextStyle(color: Colors.white70)),
                ],
              ),
            );
          }
          return Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              const Text(
                "Point your phone towards Makkah",
                style: TextStyle(color: Colors.white70, fontSize: 16),
              ),
              const SizedBox(height: 48),
              Center(
                child: StreamBuilder<CompassEvent>(
                  stream: FlutterCompass.events,
                  builder: (context, snapshot) {
                    if (snapshot.hasError) {
                      return Text('Error reading heading: ${snapshot.error}');
                    }
                    if (snapshot.connectionState == ConnectionState.waiting) {
                      return const Center(child: CircularProgressIndicator());
                    }
                    double? direction = snapshot.data?.heading;
                    if (direction == null) {
                      return const Center(child: Text("Device does not have sensors !", style: TextStyle(color: Colors.white)));
                    }
                    
                    return Transform.rotate(
                      angle: (direction * (math.pi / 180) * -1),
                      child: SizedBox(
                        width: 300,
                        height: 300,
                        child: CustomPaint(
                          painter: _CompassPainter(
                            qiblaAngle: _qiblaAngle,
                          ),
                        ),
                      ),
                    );
                  },
                ),
              ),
              const SizedBox(height: 64),
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 12),
                decoration: BoxDecoration(
                  color: const Color(0xFF3A9AD9).withOpacity(0.15),
                  borderRadius: BorderRadius.circular(24),
                ),
                child: Row(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    const Icon(Icons.explore, color: Color(0xFF3A9AD9)),
                    const SizedBox(width: 8),
                    Text(
                      "Qibla is ${(_qiblaAngle * 180 / math.pi).toStringAsFixed(1)}° from North",
                      style: const TextStyle(color: Color(0xFF3A9AD9), fontWeight: FontWeight.bold, fontSize: 16),
                    ),
                  ],
                ),
              ),
            ],
          );
        } else {
          return Center(
            child: ElevatedButton(
              child: const Text('Request Permissions'),
              onPressed: () {
                Permission.locationWhenInUse.request().then((ignored) {
                  _fetchPermissionStatus();
                });
              },
            ),
          );
        }
      }),
    );
  }
}

class _CompassPainter extends CustomPainter {
  final double qiblaAngle;

  _CompassPainter({required this.qiblaAngle});

  @override
  void paint(Canvas canvas, Size size) {
    final center = Offset(size.width / 2, size.height / 2);
    final radius = size.width / 2;

    // Outer ring
    final outerRingPaint = Paint()
      ..color = const Color(0xFF1E1E1E)
      ..style = PaintingStyle.stroke
      ..strokeWidth = 12;
    canvas.drawCircle(center, radius - 6, outerRingPaint);

    // Inner background
    final bgPaint = Paint()
      ..shader = RadialGradient(
        colors: [const Color(0xFF1A1A1A), const Color(0xFF121212)],
      ).createShader(Rect.fromCircle(center: center, radius: radius));
    canvas.drawCircle(center, radius - 12, bgPaint);

    // Ticks
    final tickPaint = Paint()..strokeWidth = 2;
    for (int i = 0; i < 360; i += 15) {
      final angle = i * math.pi / 180 - math.pi / 2;
      final isMajor = i % 90 == 0;
      final tickLength = isMajor ? 16.0 : 8.0;
      tickPaint.color = isMajor ? Colors.white : Colors.white24;
      tickPaint.strokeWidth = isMajor ? 3 : 1.5;
      
      final startX = center.dx + (radius - 12 - tickLength) * math.cos(angle);
      final startY = center.dy + (radius - 12 - tickLength) * math.sin(angle);
      final endX = center.dx + (radius - 12) * math.cos(angle);
      final endY = center.dy + (radius - 12) * math.sin(angle);
      
      canvas.drawLine(Offset(startX, startY), Offset(endX, endY), tickPaint);
    }

    // N, E, S, W Labels
    _drawText(canvas, "N", center.dx, center.dy - radius + 36, Colors.redAccent, 24);
    _drawText(canvas, "E", center.dx + radius - 36, center.dy, Colors.white70, 20);
    _drawText(canvas, "S", center.dx, center.dy + radius - 36, Colors.white70, 20);
    _drawText(canvas, "W", center.dx - radius + 36, center.dy, Colors.white70, 20);

    // Qibla Indicator (Kaaba icon or pointer)
    final qiblaPaint = Paint()
      ..color = const Color(0xFF3A9AD9)
      ..style = PaintingStyle.fill;
    
    final qX = center.dx + (radius - 12) * math.cos(qiblaAngle - math.pi / 2);
    final qY = center.dy + (radius - 12) * math.sin(qiblaAngle - math.pi / 2);
    canvas.drawCircle(Offset(qX, qY), 6, qiblaPaint);

    // Compass Needle
    final needlePath = Path();
    
    // North pointing needle (Red)
    needlePath.moveTo(center.dx - 10, center.dy);
    needlePath.lineTo(center.dx, center.dy - radius * 0.6);
    needlePath.lineTo(center.dx + 10, center.dy);
    needlePath.close();
    canvas.drawPath(needlePath, Paint()..color = Colors.redAccent);

    // South pointing needle (White)
    final southPath = Path();
    southPath.moveTo(center.dx - 10, center.dy);
    southPath.lineTo(center.dx, center.dy + radius * 0.6);
    southPath.lineTo(center.dx + 10, center.dy);
    southPath.close();
    canvas.drawPath(southPath, Paint()..color = Colors.white);

    // Center pin
    canvas.drawCircle(center, 12, Paint()..color = const Color(0xFF2A2A2A));
    canvas.drawCircle(center, 6, Paint()..color = Colors.white);
  }

  void _drawText(Canvas canvas, String text, double x, double y, Color color, double size) {
    final textSpan = TextSpan(
      text: text,
      style: TextStyle(color: color, fontSize: size, fontWeight: FontWeight.bold),
    );
    final textPainter = TextPainter(
      text: textSpan,
      textDirection: TextDirection.ltr,
    );
    textPainter.layout();
    textPainter.paint(canvas, Offset(x - textPainter.width / 2, y - textPainter.height / 2));
  }

  @override
  bool shouldRepaint(covariant CustomPainter oldDelegate) => true;
}
