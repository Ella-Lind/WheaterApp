package com.wheatherapp.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OpenMeteoResponseDto(
    @SerialName("latitude") val latitude: Double,
    @SerialName("longitude") val longitude: Double,
    @SerialName("current") val current: CurrentUnitsDto? = null,
    @SerialName("hourly") val hourly: HourlyUnitsDto? = null,
    @SerialName("daily") val daily: DailyUnitsDto? = null
)

@Serializable
data class CurrentUnitsDto(
    @SerialName("temperature_2m") val temperature2m: Double = 0.0,
    @SerialName("relative_humidity_2m") val relativeHumidity2m: Int = 0,
    @SerialName("apparent_temperature") val apparentTemperature: Double = 0.0,
    @SerialName("precipitation") val precipitation: Double = 0.0,
    @SerialName("weather_code") val weatherCode: Int = 0,
    @SerialName("wind_speed_10m") val windSpeed10m: Double = 0.0,
    @SerialName("wind_direction_10m") val windDirection10m: Int = 0
)

@Serializable
data class HourlyUnitsDto(
    @SerialName("time") val time: List<String> = emptyList(),
    @SerialName("temperature_2m") val temperature2m: List<Double> = emptyList(),
    @SerialName("precipitation_probability") val precipitationProbability: List<Int?> = emptyList(),
    @SerialName("weather_code") val weatherCode: List<Int> = emptyList(),
    @SerialName("wind_speed_10m") val windSpeed10m: List<Double> = emptyList()
)

@Serializable
data class DailyUnitsDto(
    @SerialName("time") val time: List<String> = emptyList(),
    @SerialName("weather_code") val weatherCode: List<Int> = emptyList(),
    @SerialName("temperature_2m_max") val temperature2mMax: List<Double> = emptyList(),
    @SerialName("temperature_2m_min") val temperature2mMin: List<Double> = emptyList(),
    @SerialName("precipitation_sum") val precipitationSum: List<Double> = emptyList(),
    @SerialName("precipitation_probability_max") val precipitationProbabilityMax: List<Int?> = emptyList(),
    @SerialName("wind_speed_10m_max") val windSpeed10mMax: List<Double> = emptyList()
)
