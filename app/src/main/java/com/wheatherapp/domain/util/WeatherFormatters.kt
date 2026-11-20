package com.wheatherapp.domain.util

import com.wheatherapp.domain.model.SpeedUnit
import com.wheatherapp.domain.model.TemperatureUnit
import com.wheatherapp.domain.model.UserSettings
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Pure Kotlin helper functions for formatting temperature values, wind speeds,
 * compass directions, and geographic coordinates based on [UserSettings].
 */
object WeatherFormatters {

    /**
     * Formats temperature [celsius] according to [UserSettings.temperatureUnit].
     * Returns e.g. "18°C" or "64°F".
     */
    fun formatTemperature(celsius: Double, settings: UserSettings): String {
        return formatTemperature(celsius, settings.temperatureUnit)
    }

    /**
     * Formats temperature [celsius] according to explicit [TemperatureUnit].
     */
    fun formatTemperature(celsius: Double, unit: TemperatureUnit): String {
        return when (unit) {
            TemperatureUnit.CELSIUS -> "${celsius.roundToInt()}°C"
            TemperatureUnit.FAHRENHEIT -> {
                val fahrenheit = (celsius * 9.0 / 5.0) + 32.0
                "${fahrenheit.roundToInt()}°F"
            }
        }
    }

    /**
     * Formats wind speed [speedKmh] according to [UserSettings.speedUnit].
     * Returns e.g. "12 km/h", "3.3 m/s", or "7 mph".
     */
    fun formatWindSpeed(speedKmh: Double, settings: UserSettings): String {
        return formatWindSpeed(speedKmh, settings.speedUnit)
    }

    /**
     * Formats wind speed [speedKmh] according to explicit [SpeedUnit].
     */
    fun formatWindSpeed(speedKmh: Double, unit: SpeedUnit): String {
        return when (unit) {
            SpeedUnit.KMH -> "${speedKmh.roundToInt()} km/h"
            SpeedUnit.MS -> {
                val ms = speedKmh / 3.6
                String.format(Locale.US, "%.1f m/s", ms)
            }
            SpeedUnit.MPH -> {
                val mph = speedKmh * 0.621371
                "${mph.roundToInt()} mph"
            }
        }
    }

    /**
     * Converts wind direction degrees (0..360) into a cardinal/intercardinal compass label
     * (e.g. "N", "NE", "E", "SE", "S", "SW", "W", "NW").
     */
    fun formatWindDirection(degrees: Int): String {
        val normalized = (degrees % 360 + 360) % 360
        val directions = arrayOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
        val index = ((normalized + 22.5) / 45.0).toInt() % 8
        return directions[index]
    }

    /**
     * Formats geographic latitude and longitude into standard string representation
     * (e.g. "59.3293, 18.0686").
     */
    fun formatCoordinates(latitude: Double, longitude: Double): String {
        return String.format(Locale.US, "%.4f, %.4f", latitude, longitude)
    }

    /**
     * Formats geographic latitude and longitude with cardinal directions
     * (e.g. "59.3293° N, 18.0686° E").
     */
    fun formatCoordinatesWithDirections(latitude: Double, longitude: Double): String {
        val latDirection = if (latitude >= 0) "N" else "S"
        val lonDirection = if (longitude >= 0) "E" else "W"
        return String.format(
            Locale.US,
            "%.4f° %s, %.4f° %s",
            Math.abs(latitude), latDirection,
            Math.abs(longitude), lonDirection
        )
    }

    fun formatDayTitle(dateIso: String, dayIndex: Int): String {
        return try {
            val parsed = LocalDate.parse(dateIso)
            val dayOfWeek = parsed.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.US)
            val month = parsed.month.getDisplayName(TextStyle.SHORT, Locale.US)
            "$dayOfWeek, $month ${parsed.dayOfMonth}"
        } catch (_: Exception) {
            "Day ${dayIndex + 1} Forecast"
        }
    }

    fun formatDayName(dateIso: String, index: Int): String {
        return try {
            LocalDate.parse(dateIso).dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.US)
        } catch (_: Exception) {
            "Day ${index + 1}"
        }
    }

    fun formatHourTime(timeIso: String): String {
        return timeIso.substringAfter('T').take(5)
    }

    fun getWindSpeedDescription(speedKmh: Double): String {
        return when {
            speedKmh < 12.0 -> "Light breeze"
            speedKmh < 29.0 -> "Gentle breeze"
            speedKmh < 50.0 -> "Strong wind"
            else -> "Gale warning"
        }
    }
}
