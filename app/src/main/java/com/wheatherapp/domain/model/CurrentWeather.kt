package com.wheatherapp.domain.model

data class CurrentWeather(
    val temperatureCelsius: Double,
    val relativeHumidityPercent: Int,
    val apparentTemperatureCelsius: Double,
    val precipitationMm: Double,
    val weatherCondition: WeatherCondition,
    val windSpeedKmh: Double,
    val windDirectionDegrees: Int
)