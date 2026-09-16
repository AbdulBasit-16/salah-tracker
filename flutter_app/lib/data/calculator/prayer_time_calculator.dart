import 'dart:math';
import 'package:flutter/material.dart';

enum CalculationMethod {
  mwl(18.0, 17.0, false, 0),
  isna(15.0, 15.0, false, 0),
  egypt(19.5, 17.5, false, 0),
  karachi(18.0, 18.0, false, 0),
  ummAlQura(18.5, 0.0, true, 90),
  gulf(19.5, 0.0, true, 90),
  tehran(17.7, 14.0, false, 0);

  final double fajrAngle;
  final double ishaAngle;
  final bool isTimeBasedIsha;
  final int ishaIntervalMinutes;

  const CalculationMethod(this.fajrAngle, this.ishaAngle, this.isTimeBasedIsha, this.ishaIntervalMinutes);
}

enum JuristicMethod {
  standard(1.0),
  hanafi(2.0);

  final double shadowRatio;

  const JuristicMethod(this.shadowRatio);
}

class PrayerTimes {
  final TimeOfDay fajr;
  final TimeOfDay sunrise;
  final TimeOfDay dhuhr;
  final TimeOfDay asr;
  final TimeOfDay maghrib;
  final TimeOfDay isha;

  PrayerTimes({
    required this.fajr,
    required this.sunrise,
    required this.dhuhr,
    required this.asr,
    required this.maghrib,
    required this.isha,
  });
}

class SunPosition {
  final double declination;
  final double equationOfTime;

  SunPosition(this.declination, this.equationOfTime);
}

class PrayerTimeCalculator {
  double _dToR(double deg) => deg * pi / 180.0;
  double _rToD(double rad) => rad * 180.0 / pi;

  double _fixHour(double h) => (h % 24.0 + 24.0) % 24.0;
  double _fixAngle(double a) => (a % 360.0 + 360.0) % 360.0;

  double _getJulianDate(int year, int month, int day) {
    int y = year;
    int m = month;
    if (m <= 2) {
      y -= 1;
      m += 12;
    }
    double a = (y / 100.0).floorToDouble();
    double b = 2.0 - a + (a / 4.0).floorToDouble();
    return (365.25 * (y + 4716)).floorToDouble() + (30.6001 * (m + 1)).floorToDouble() + day + b - 1524.5;
  }

  SunPosition _getSunPosition(double jd) {
    double t = (jd - 2451545.0) / 36525.0;
    
    double l0 = _fixAngle(280.46646 + 36000.76983 * t + 0.0003032 * t * t);
    double m = _fixAngle(357.52911 + 35999.05029 * t - 0.0001537 * t * t);
    
    double e0 = 23.439291 - 0.01300416 * t - 0.000000164 * t * t + 0.000000503 * t * t * t;
    double ob = e0 + 0.00256 * cos(_dToR(125.04 - 1934.136 * t));
    
    double c = (1.914602 - 0.004817 * t - 0.000014 * t * t) * sin(_dToR(m)) +
            (0.019993 - 0.000101 * t) * sin(_dToR(2.0 * m)) +
            0.002893 * sin(_dToR(3.0 * m));
            
    double lambda = _fixAngle(l0 + c);
    
    double ra = _rToD(atan2(cos(_dToR(ob)) * sin(_dToR(lambda)), cos(_dToR(lambda))));
    ra = _fixAngle(ra);
    
    double lq = (lambda / 90.0).floorToDouble() * 90.0;
    double rq = (ra / 90.0).floorToDouble() * 90.0;
    double raAdjusted = ra + (lq - rq);
    
    double dec = _rToD(asin(sin(_dToR(ob)) * sin(_dToR(lambda))));
    
    double diff = l0 - raAdjusted;
    if (diff > 180.0) diff -= 360.0;
    if (diff < -180.0) diff += 360.0;
    double eqt = diff / 15.0;
    
    return SunPosition(dec, eqt);
  }

  double _getHourAngle(double angle, double latitude, double declination, bool isMorning) {
    double latRad = _dToR(latitude);
    double decRad = _dToR(declination);
    double angleRad = _dToR(angle);
    
    double cosH = (sin(-angleRad) - sin(latRad) * sin(decRad)) / (cos(latRad) * cos(decRad));
    double clampedCosH = cosH.clamp(-1.0, 1.0);
    
    double h = _rToD(acos(clampedCosH));
    return isMorning ? -h : h;
  }

  double _getHourAngleForPositiveAltitude(double altDeg, double latitude, double declination) {
    double latRad = _dToR(latitude);
    double decRad = _dToR(declination);
    double altRad = _dToR(altDeg);
    
    double cosH = (sin(altRad) - sin(latRad) * sin(decRad)) / (cos(latRad) * cos(decRad));
    double clampedCosH = cosH.clamp(-1.0, 1.0);
    
    return _rToD(acos(clampedCosH));
  }

  PrayerTimes calculate({
    required double latitude,
    required double longitude,
    required double timezoneOffset,
    required int year,
    required int month,
    required int day,
    CalculationMethod method = CalculationMethod.mwl,
    JuristicMethod juristic = JuristicMethod.standard,
  }) {
    double jd = _getJulianDate(year, month, day) - longitude / 360.0;
    SunPosition sun = _getSunPosition(jd);
    
    double dhuhrLocalHours = _fixHour(12.0 - longitude / 15.0 - sun.equationOfTime + timezoneOffset);
    
    double sunriseH = _getHourAngle(0.833, latitude, sun.declination, true);
    double sunsetH = _getHourAngle(0.833, latitude, sun.declination, false);
    
    double sunriseLocalHours = _fixHour(dhuhrLocalHours + sunriseH / 15.0);
    double sunsetLocalHours = _fixHour(dhuhrLocalHours + sunsetH / 15.0);
    
    double fajrH = _getHourAngle(method.fajrAngle, latitude, sun.declination, true);
    double fajrLocalHours = _fixHour(dhuhrLocalHours + fajrH / 15.0);
    
    double shadowLength = juristic.shadowRatio + tan((_dToR(latitude - sun.declination)).abs());
    double altRad = atan(1.0 / shadowLength);
    double altDeg = _rToD(altRad);
    double asrH = _getHourAngleForPositiveAltitude(altDeg, latitude, sun.declination);
    double asrLocalHours = _fixHour(dhuhrLocalHours + asrH / 15.0);
    
    double ishaLocalHours;
    if (method.isTimeBasedIsha) {
      ishaLocalHours = _fixHour(sunsetLocalHours + method.ishaIntervalMinutes / 60.0);
    } else {
      double ishaH = _getHourAngle(method.ishaAngle, latitude, sun.declination, false);
      ishaLocalHours = _fixHour(dhuhrLocalHours + ishaH / 15.0);
    }

    double dhuhrFinal = _fixHour(dhuhrLocalHours + 1.0 / 60.0);

    return PrayerTimes(
      fajr: _doubleToTime(fajrLocalHours),
      sunrise: _doubleToTime(sunriseLocalHours),
      dhuhr: _doubleToTime(dhuhrFinal),
      asr: _doubleToTime(asrLocalHours),
      maghrib: _doubleToTime(sunsetLocalHours),
      isha: _doubleToTime(ishaLocalHours),
    );
  }

  TimeOfDay _doubleToTime(double hoursDouble) {
    int totalMinutes = (hoursDouble * 60.0).round();
    int h = (totalMinutes ~/ 60) % 24;
    int m = totalMinutes % 60;
    return TimeOfDay(hour: h, minute: m);
  }
}
