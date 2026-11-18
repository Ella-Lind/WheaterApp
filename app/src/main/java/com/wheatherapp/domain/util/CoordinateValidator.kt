package com.wheatherapp.domain.util

/**
 * Pure Kotlin validator for checking floating point latitude/longitude values
 * and parsing raw string coordinate inputs (e.g. "59.3293, 18.0686").
 */
object CoordinateValidator {

    fun isValidLatitude(lat: Double): Boolean = lat in -90.0..90.0

    fun isValidLongitude(lon: Double): Boolean = lon in -180.0..180.0

    /**
     * Parses a string input that may be formatted as "lat, lon" or "lat,lon".
     * Returns a [Pair] of (latitude, longitude) if valid, or null if it's a place name string.
     */
    fun parseCoordinates(input: String): Pair<Double, Double>? {
        val parts = input.split(",").map { it.trim() }
        if (parts.size != 2) return null

        val lat = parts[0].toDoubleOrNull() ?: return null
        val lon = parts[1].toDoubleOrNull() ?: return null

        return if (isValidLatitude(lat) && isValidLongitude(lon)) {
            Pair(lat, lon)
        } else {
            null
        }
    }
}
