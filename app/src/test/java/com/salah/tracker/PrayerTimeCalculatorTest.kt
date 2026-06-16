package com.salah.tracker

import com.salah.tracker.data.calculator.PrayerTimeCalculator
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime

class PrayerTimeCalculatorTest {

    private val calculator = PrayerTimeCalculator()

    @Test
    fun testMakkahPrayerTimes() {
        // Makkah coordinates
        val latitude = 21.4225
        val longitude = 39.8262
        val timezoneOffset = 3.0
        
        // Date: 2026-06-16
        val times = calculator.calculate(
            latitude = latitude,
            longitude = longitude,
            timezoneOffset = timezoneOffset,
            year = 2026,
            month = 6,
            day = 16,
            method = PrayerTimeCalculator.CalculationMethod.UMM_AL_QURA,
            juristic = PrayerTimeCalculator.JuristicMethod.STANDARD
        )

        assertNotNull(times)
        
        // Assert chronological order
        assertTrue("Fajr must be before Sunrise", times.fajr.isBefore(times.sunrise))
        assertTrue("Sunrise must be before Dhuhr", times.sunrise.isBefore(times.dhuhr))
        assertTrue("Dhuhr must be before Asr", times.dhuhr.isBefore(times.asr))
        assertTrue("Asr must be before Maghrib", times.asr.isBefore(times.maghrib))
        assertTrue("Maghrib must be before Isha", times.maghrib.isBefore(times.isha))

        println("Calculated Makkah Times (2026-06-16):")
        println("Fajr: ${times.fajr}")
        println("Sunrise: ${times.sunrise}")
        println("Dhuhr: ${times.dhuhr}")
        println("Asr: ${times.asr}")
        println("Maghrib: ${times.maghrib}")
        println("Isha: ${times.isha}")
    }

    @Test
    fun testLondonPrayerTimes() {
        // London coordinates
        val latitude = 51.5074
        val longitude = -0.1278
        val timezoneOffset = 1.0 // BST
        
        val times = calculator.calculate(
            latitude = latitude,
            longitude = longitude,
            timezoneOffset = timezoneOffset,
            year = 2026,
            month = 6,
            day = 16,
            method = PrayerTimeCalculator.CalculationMethod.MWL,
            juristic = PrayerTimeCalculator.JuristicMethod.STANDARD
        )

        assertNotNull(times)
        assertTrue(times.fajr.isBefore(times.sunrise))
        assertTrue(times.sunrise.isBefore(times.dhuhr))
        assertTrue(times.dhuhr.isBefore(times.asr))
        assertTrue(times.asr.isBefore(times.maghrib))
        assertTrue(times.maghrib.isBefore(times.isha))

        println("Calculated London Times (2026-06-16):")
        println("Fajr: ${times.fajr}")
        println("Sunrise: ${times.sunrise}")
        println("Dhuhr: ${times.dhuhr}")
        println("Asr: ${times.asr}")
        println("Maghrib: ${times.maghrib}")
        println("Isha: ${times.isha}")
    }
}
