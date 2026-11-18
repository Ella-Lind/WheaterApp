package com.wheatherapp.domain.model

enum class TemperatureUnit { CELSIUS, FAHRENHEIT }
enum class SpeedUnit { KMH, MS, MPH }
enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class UserSettings(
    val temperatureUnit: TemperatureUnit = TemperatureUnit.CELSIUS,
    val speedUnit: SpeedUnit = SpeedUnit.KMH,
    val refreshIntervalMinutes: Int = 30,
    val themeMode: ThemeMode = ThemeMode.SYSTEM
)