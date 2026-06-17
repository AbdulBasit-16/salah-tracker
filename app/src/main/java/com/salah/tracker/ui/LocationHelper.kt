package com.salah.tracker.ui

import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.widget.Toast
import com.salah.tracker.data.model.PakistanCities
import java.util.Locale
import java.util.TimeZone

fun detectLocation(context: Context, onLocationDetected: (Double, Double, Double, String) -> Unit) {
    try {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

        if (!isGpsEnabled && !isNetworkEnabled) {
            Toast.makeText(context, "Location services are disabled in system settings.", Toast.LENGTH_LONG).show()
            onLocationDetected(21.4225, 39.8262, TimeZone.getDefault().rawOffset / 3600000.0, "Makkah (Default)")
            return
        }

        // Try getting last known locations safely
        val gpsLoc = try {
            if (isGpsEnabled) locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER) else null
        } catch (e: SecurityException) { null }
        
        val netLoc = try {
            if (isNetworkEnabled) locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER) else null
        } catch (e: SecurityException) { null }

        // Use the freshest / best last known location
        val bestLocation = getBestLocation(gpsLoc, netLoc)

        if (bestLocation != null) {
            processLocation(context, bestLocation, onLocationDetected)
        } else {
            Toast.makeText(context, "Requesting fresh location fix...", Toast.LENGTH_SHORT).show()
            
            val listener = object : LocationListener {
                private var isFinished = false
                
                override fun onLocationChanged(loc: Location) {
                    if (!isFinished) {
                        isFinished = true
                        try {
                            locationManager.removeUpdates(this)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        processLocation(context, loc, onLocationDetected)
                    }
                }
            }

            try {
                if (isNetworkEnabled) {
                    locationManager.requestLocationUpdates(
                        LocationManager.NETWORK_PROVIDER,
                        0L,
                        0f,
                        listener,
                        context.mainLooper
                    )
                }
                if (isGpsEnabled) {
                    locationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        0L,
                        0f,
                        listener,
                        context.mainLooper
                    )
                }
            } catch (e: SecurityException) {
                Toast.makeText(context, "Location permission missing.", Toast.LENGTH_SHORT).show()
            }
        }
    } catch (e: SecurityException) {
        Toast.makeText(context, "Location permission missing.", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Location detection failed: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

private fun getBestLocation(loc1: Location?, loc2: Location?): Location? {
    if (loc1 == null) return loc2
    if (loc2 == null) return loc1
    // Choose the location with the more recent timestamp
    return if (loc1.time > loc2.time) loc1 else loc2
}

fun processLocation(
    context: Context,
    location: Location,
    onLocationDetected: (Double, Double, Double, String) -> Unit
) {
    val tzOffset = TimeZone.getDefault().rawOffset / 3600000.0
    val geocoder = Geocoder(context, Locale.getDefault())
    var cityName = ""
    try {
        @Suppress("DEPRECATION")
        val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
        val address = addresses?.firstOrNull()
        if (address != null) {
            // Prioritize village/neighborhood name (subLocality) or city (locality) over featureName (which can be a house number/street)
            cityName = address.subLocality ?: address.locality ?: address.subAdminArea ?: address.adminArea ?: address.featureName ?: ""
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }

    if (cityName.isEmpty() || cityName == "Detected Location" || cityName == "Custom") {
        val closest = findClosestPakistanCity(location.latitude, location.longitude)
        cityName = if (closest != null) {
            "$closest (Auto-Detected)"
        } else {
            String.format("Location (%.3f, %.3f)", location.latitude, location.longitude)
        }
    }
    onLocationDetected(location.latitude, location.longitude, tzOffset, cityName)
}

private fun findClosestPakistanCity(latitude: Double, longitude: Double): String? {
    var closestCity: String? = null
    var minDistance = Double.MAX_VALUE
    for (city in PakistanCities.cities) {
        val dist = calculateDistance(latitude, longitude, city.latitude, city.longitude)
        if (dist < minDistance) {
            minDistance = dist
            closestCity = city.name
        }
    }
    return if (minDistance < 150.0) closestCity else null
}

private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val r = 6371.0 // Earth radius in km
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
            Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
            Math.sin(dLon / 2) * Math.sin(dLon / 2)
    val c = 2 * Math.asin(Math.sqrt(a))
    return r * c
}
