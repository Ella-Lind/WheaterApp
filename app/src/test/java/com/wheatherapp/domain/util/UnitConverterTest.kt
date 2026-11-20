package com.wheatherapp.domain.util

import com.wheatherapp.domain.model.SpeedUnit
import com.wheatherapp.domain.model.TemperatureUnit
import org.junit.Assert.assertEquals
import org.junit.Test

class UnitConverterTest {

    @Test
    fun formatTemperature_celsius_formatsOneDecimal() {
        assertEquals("18.4°C", UnitConverter.formatTemperature(18.4, TemperatureUnit.CELSIUS))
    }

    @Test
    fun formatTemperature_fahrenheit_convertsCorrectly() {
        assertEquals("65.1°F", UnitConverter.formatTemperature(18.4, TemperatureUnit.FAHRENHEIT))
    }

    @Test
    fun formatSpeed_kmh_formatsCorrectly() {
        assertEquals("20.3 km/h", UnitConverter.formatSpeed(20.3, SpeedUnit.KMH))
    }

    @Test
    fun formatSpeed_ms_convertsKmhToMs() {
        assertEquals("10.0 m/s", UnitConverter.formatSpeed(36.0, SpeedUnit.MS))
    }

    @Test
    fun formatSpeed_mph_convertsKmhToMph() {
        assertEquals("12.4 mph", UnitConverter.formatSpeed(20.0, SpeedUnit.MPH))
    }
}
