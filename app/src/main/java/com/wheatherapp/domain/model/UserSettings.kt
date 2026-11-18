package com.wheatherapp.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class TemperatureUnit { CELSIUS, FAHRENHEIT }
@Serializable
enum class SpeedUnit { KMH, MS, MPH }
@Serializable
enum class ThemeMode { SYSTEM, LIGHT, DARK }

@Serializable
data class UserSettings(
    val temperatureUnit: TemperatureUnit = TemperatureUnit.CELSIUS,
    val speedUnit: SpeedUnit = SpeedUnit.KMH,
    val refreshIntervalMinutes: Int = 30,
    val themeMode: ThemeMode = ThemeMode.SYSTEM
)
