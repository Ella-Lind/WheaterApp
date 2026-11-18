package com.wheatherapp.domain.util

import com.wheatherapp.domain.model.SpeedUnit
import com.wheatherapp.domain.model.TemperatureUnit
import java.util.Locale

/**
 * Pure Kotlin utility for formatting temperatures and wind speeds according to [UserSettings].
 */
object UnitConverter {

    fun formatTemperature(celsius: Double, unit: TemperatureUnit): String {
        val value = when (unit) {
            TemperatureUnit.CELSIUS -> celsius
            TemperatureUnit.FAHRENHEIT -> (celsius * 9 / 5) + 32
        }
        val symbol = when (unit) {
            TemperatureUnit.CELSIUS -> "°C"
            TemperatureUnit.FAHRENHEIT -> "°F"
        }
        return String.format(Locale.US, "%.1f%s", value, symbol)
    }

    fun formatSpeed(speedKmh: Double, unit: SpeedUnit): String {
        val value = when (unit) {
            SpeedUnit.KMH -> speedKmh
            SpeedUnit.MS -> speedKmh / 3.6
            SpeedUnit.MPH -> speedKmh * 0.621371
        }
        val label = when (unit) {
            SpeedUnit.KMH -> "km/h"
            SpeedUnit.MS -> "m/s"
            SpeedUnit.MPH -> "mph"
        }
        return String.format(Locale.US, "%.1f %s", value, label)
    }
}
