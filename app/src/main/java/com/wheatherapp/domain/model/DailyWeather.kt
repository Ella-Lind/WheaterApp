package com.wheatherapp.domain.model

data class DailyWeather(
    val dateIso: String,
    val weatherCondition: WeatherCondition,
    val maxTemperatureCelsius: Double,
    val minTemperatureCelsius: Double,
    val precipitationSumMm: Double,
    val maxPrecipitationProbabilityPercent: Int,
    val maxWindSpeedKmh: Double,
    val hourlyForecast: List<HourlyWeather> = emptyList()
)