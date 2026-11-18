package com.wheatherapp.domain.model

data class HourlyWeather(
    val timeIso: String,
    val temperatureCelsius: Double,
    val precipitationProbabilityPercent: Int,
    val weatherCondition: WeatherCondition,
    val windSpeedKmh: Double
)