package com.salah.tracker.data.calculator

import java.time.LocalTime
import kotlin.math.*

class PrayerTimeCalculator {

    enum class CalculationMethod(val fajrAngle: Double, val ishaAngle: Double, val isTimeBasedIsha: Boolean, val ishaIntervalMinutes: Int) {
        MWL(18.0, 17.0, false, 0),
        ISNA(15.0, 15.0, false, 0),
        EGYPT(19.5, 17.5, false, 0),
        KARACHI(18.0, 18.0, false, 0),
        UMM_AL_QURA(18.5, 0.0, true, 90),
        GULF(19.5, 0.0, true, 90),
        TEHRAN(17.7, 14.0, false, 0)
    }

    enum class JuristicMethod(val shadowRatio: Double) {
        STANDARD(1.0), // Shafi'i, Maliki, Ja'fari, Hanbali
        HANAFI(2.0)
    }

    data class PrayerTimes(
        val fajr: LocalTime,
        val sunrise: LocalTime,
        val dhuhr: LocalTime,
        val asr: LocalTime,
        val maghrib: LocalTime,
        val isha: LocalTime
    )

    private fun dToR(deg: Double): Double = deg * Math.PI / 180.0
    private fun rToD(rad: Double): Double = rad * 180.0 / Math.PI

    private fun fixHour(h: Double): Double = (h % 24.0 + 24.0) % 24.0
    private fun fixAngle(a: Double): Double = (a % 360.0 + 360.0) % 360.0

    private fun getJulianDate(year: Int, month: Int, day: Int): Double {
        var y = year
        var m = month
        if (m <= 2) {
            y -= 1
            m += 12
        }
        val a = floor(y / 100.0)
        val b = 2.0 - a + floor(a / 4.0)
        return floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + day + b - 1524.5
    }

    private data class SunPosition(val declination: Double, val equationOfTime: Double)

    private fun getSunPosition(jd: Double): SunPosition {
        val t = (jd - 2451545.0) / 36525.0
        
        // Solar mean longitude
        val l0 = fixAngle(280.46646 + 36000.76983 * t + 0.0003032 * t * t)
        
        // Solar mean anomaly
        val m = fixAngle(357.52911 + 35999.05029 * t - 0.0001537 * t * t)
        
        // Obliquity of the ecliptic
        val e0 = 23.439291 - 0.01300416 * t - 0.000000164 * t * t + 0.000000503 * t * t * t
        val ob = e0 + 0.00256 * cos(dToR(125.04 - 1934.136 * t))
        
        // Sun's equation of center
        val c = (1.914602 - 0.004817 * t - 0.000014 * t * t) * sin(dToR(m)) +
                (0.019993 - 0.000101 * t) * sin(dToR(2.0 * m)) +
                0.002893 * sin(dToR(3.0 * m))
        
        // Sun's true longitude
        val lambda = fixAngle(l0 + c)
        
        // Right ascension
        var ra = rToD(atan2(cos(dToR(ob)) * sin(dToR(lambda)), cos(dToR(lambda))))
        ra = fixAngle(ra)
        
        // Adjust RA to be in the same quadrant as lambda
        val lq = floor(lambda / 90.0) * 90.0
        val rq = floor(ra / 90.0) * 90.0
        val raAdjusted = ra + (lq - rq)
        
        // Sun's declination
        val dec = rToD(asin(sin(dToR(ob)) * sin(dToR(lambda))))
        
        // Equation of time (in hours)
        var diff = l0 - raAdjusted
        if (diff > 180.0) diff -= 360.0
        if (diff < -180.0) diff += 360.0
        val eqt = diff / 15.0
        
        return SunPosition(dec, eqt)
    }

    private fun getHourAngle(angle: Double, latitude: Double, declination: Double, isMorning: Boolean): Double {
        val latRad = dToR(latitude)
        val decRad = dToR(declination)
        val angleRad = dToR(angle)
        
        val cosH = (sin(-angleRad) - sin(latRad) * sin(decRad)) / (cos(latRad) * cos(decRad))
        // Clamp cosH to prevent NaN in extreme latitudes
        val clampedCosH = cosH.coerceIn(-1.0, 1.0)
        
        val h = rToD(acos(clampedCosH))
        return if (isMorning) -h else h
    }

    private fun getHourAngleForPositiveAltitude(altDeg: Double, latitude: Double, declination: Double): Double {
        val latRad = dToR(latitude)
        val decRad = dToR(declination)
        val altRad = dToR(altDeg)
        
        val cosH = (sin(altRad) - sin(latRad) * sin(decRad)) / (cos(latRad) * cos(decRad))
        val clampedCosH = cosH.coerceIn(-1.0, 1.0)
        
        return rToD(acos(clampedCosH))
    }

    fun calculate(
        latitude: Double,
        longitude: Double,
        timezoneOffset: Double,
        year: Int,
        month: Int,
        day: Int,
        method: CalculationMethod = CalculationMethod.MWL,
        juristic: JuristicMethod = JuristicMethod.STANDARD
    ): PrayerTimes {
        // Compute Julian date at noon (UTC)
        val jd = getJulianDate(year, month, day) - longitude / 360.0
        val sun = getSunPosition(jd)
        
        // Dhuhr (solar transit local time)
        // Midday (Dhuhr) is when sun reaches its highest transit point
        val dhuhrLocalHours = fixHour(12.0 - longitude / 15.0 - sun.equationOfTime + timezoneOffset)
        
        // Sunrise & Sunset (Maghrib)
        // Standard sunrise/sunset is 0.833 degrees below horizon (refraction + solar radius)
        val sunriseH = getHourAngle(0.833, latitude, sun.declination, isMorning = true)
        val sunsetH = getHourAngle(0.833, latitude, sun.declination, isMorning = false)
        
        val sunriseLocalHours = fixHour(dhuhrLocalHours + sunriseH / 15.0)
        val sunsetLocalHours = fixHour(dhuhrLocalHours + sunsetH / 15.0)
        
        // Fajr
        val fajrH = getHourAngle(method.fajrAngle, latitude, sun.declination, isMorning = true)
        val fajrLocalHours = fixHour(dhuhrLocalHours + fajrH / 15.0)
        
        // Asr shadow angle calculation
        val shadowLength = juristic.shadowRatio + tan(abs(dToR(latitude - sun.declination)))
        val altRad = atan(1.0 / shadowLength)
        val altDeg = rToD(altRad)
        val asrH = getHourAngleForPositiveAltitude(altDeg, latitude, sun.declination)
        val asrLocalHours = fixHour(dhuhrLocalHours + asrH / 15.0)
        
        // Isha
        val ishaLocalHours = if (method.isTimeBasedIsha) {
            // Isha is a fixed interval after Maghrib (e.g. 90 minutes)
            fixHour(sunsetLocalHours + method.ishaIntervalMinutes / 60.0)
        } else {
            val ishaH = getHourAngle(method.ishaAngle, latitude, sun.declination, isMorning = false)
            fixHour(dhuhrLocalHours + ishaH / 15.0)
        }

        // Add 1 minute safety buffer to Dhuhr
        val dhuhrFinal = fixHour(dhuhrLocalHours + 1.0 / 60.0)

        return PrayerTimes(
            fajr = doubleToTime(fajrLocalHours),
            sunrise = doubleToTime(sunriseLocalHours),
            dhuhr = doubleToTime(dhuhrFinal),
            asr = doubleToTime(asrLocalHours),
            maghrib = doubleToTime(sunsetLocalHours),
            isha = doubleToTime(ishaLocalHours)
        )
    }

    private fun doubleToTime(hoursDouble: Double): LocalTime {
        val totalMinutes = (hoursDouble * 60.0).roundToInt()
        val h = (totalMinutes / 60) % 24
        val m = totalMinutes % 60
        return LocalTime.of(h, m)
    }
}
