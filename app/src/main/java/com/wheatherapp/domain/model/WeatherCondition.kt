package com.wheatherapp.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class WeatherCondition(
    val description: String,
    val wmoCodes: List<Int>
) {
    CLEAR_SKY("Clear Sky", listOf(0)),
    MAINLY_CLEAR("Mainly Clear", listOf(1)),
    PARTLY_CLOUDY("Partly Cloudy", listOf(2)),
    OVERCAST("Overcast", listOf(3)),
    FOG("Foggy", listOf(45, 48)),
    DRIZZLE("Drizzle", listOf(51, 53, 55, 56, 57)),
    RAIN("Rain", listOf(61, 63, 65, 66, 67)),
    SNOW("Snow", listOf(71, 73, 75, 77)),
    RAIN_SHOWERS("Rain Showers", listOf(80, 81, 82)),
    SNOW_SHOWERS("Snow Showers", listOf(85, 86)),
    THUNDERSTORM("Thunderstorm", listOf(95, 96, 99)),
    UNKNOWN("Unknown", emptyList());

    companion object {
        fun fromWmoCode(code: Int): WeatherCondition {
            return entries.find { code in it.wmoCodes } ?: UNKNOWN
        }
    }
}
