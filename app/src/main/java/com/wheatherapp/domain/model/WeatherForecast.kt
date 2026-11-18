package com.wheatherapp.domain.model

import kotlinx.serialization.Serializable

/**
 * Domain aggregate holding the complete weather forecast for a location.
 *
 * @param location Target location details.
 * @param current Current real-time weather metrics.
 * @param todayHourly Hourly prognosis entries for today's date (24 hours).
 * @param daily Complete 7-day daily forecast list (including today at index 0).
 * @param fetchedTimestampMillis Timestamp when the forecast was fetched/cached.
 * @param isFromCache Indicates if the data was loaded from Room offline storage.
 */
@Serializable
data class WeatherForecast(
    val location: LocationPlace,
    val current: CurrentWeather,
    val todayHourly: List<HourlyWeather>,
    val daily: List<DailyWeather>,
    val fetchedTimestampMillis: Long,
    val isFromCache: Boolean = false
) {
    /**
     * Today's daily summary (day 0 of the 7-day forecast).
     */
    val todayDaily: DailyWeather?
        get() = daily.firstOrNull()

    /**
     * Daily forecast for the next 6 days to come (excluding today).
     */
    val remainingDaily: List<DailyWeather>
        get() = daily.drop(1)

    /**
     * Returns the detailed hourly prognosis for a specific date (Format: "YYYY-MM-DD").
     */
    fun getHourlyForDate(dateIso: String): List<HourlyWeather> {
        val dailyHourly = daily.find { it.dateIso == dateIso }?.hourlyForecast
        if (!dailyHourly.isNullOrEmpty()) return dailyHourly
        if (daily.firstOrNull()?.dateIso == dateIso) return todayHourly
        return emptyList()
    }
}
