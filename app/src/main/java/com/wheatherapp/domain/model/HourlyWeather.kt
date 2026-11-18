package com.wheatherapp.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class HourlyWeather(
    val timeIso: String,
    val temperatureCelsius: Double,
    val precipitationProbabilityPercent: Int,
    val weatherCondition: WeatherCondition,
    val windSpeedKmh: Double
)
