package com.wheatherapp.domain.util

import com.wheatherapp.domain.model.SpeedUnit
import com.wheatherapp.domain.model.TemperatureUnit
import com.wheatherapp.domain.model.UserSettings
import org.junit.Assert.assertEquals
import org.junit.Test

class WeatherFormattersTest {

    @Test
    fun formatTemperature_celsius() {
        val settings = UserSettings(temperatureUnit = TemperatureUnit.CELSIUS)
        assertEquals("18°C", WeatherFormatters.formatTemperature(18.4, settings))
    }

    @Test
    fun formatTemperature_fahrenheit() {
        val settings = UserSettings(temperatureUnit = TemperatureUnit.FAHRENHEIT)
        assertEquals("65°F", WeatherFormatters.formatTemperature(18.4, settings))
    }

    @Test
    fun formatWindSpeed_kmh() {
        val settings = UserSettings(speedUnit = SpeedUnit.KMH)
        assertEquals("20 km/h", WeatherFormatters.formatWindSpeed(20.3, settings))
    }

    @Test
    fun formatWindSpeed_ms() {
        val settings = UserSettings(speedUnit = SpeedUnit.MS)
        assertEquals("10.0 m/s", WeatherFormatters.formatWindSpeed(36.0, settings))
    }

    @Test
    fun formatWindSpeed_mph() {
        val settings = UserSettings(speedUnit = SpeedUnit.MPH)
        assertEquals("12 mph", WeatherFormatters.formatWindSpeed(20.0, settings))
    }

    @Test
    fun formatWindDirection_cardinalPoints() {
        assertEquals("N", WeatherFormatters.formatWindDirection(0))
        assertEquals("NE", WeatherFormatters.formatWindDirection(45))
        assertEquals("E", WeatherFormatters.formatWindDirection(90))
        assertEquals("SE", WeatherFormatters.formatWindDirection(135))
        assertEquals("S", WeatherFormatters.formatWindDirection(180))
        assertEquals("SW", WeatherFormatters.formatWindDirection(225))
        assertEquals("W", WeatherFormatters.formatWindDirection(270))
        assertEquals("NW", WeatherFormatters.formatWindDirection(315))
        assertEquals("N", WeatherFormatters.formatWindDirection(360))
    }

    @Test
    fun formatCoordinates() {
        assertEquals("59.3293, 18.0686", WeatherFormatters.formatCoordinates(59.3293, 18.0686))
    }
}
