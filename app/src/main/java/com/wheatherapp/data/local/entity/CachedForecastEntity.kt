package com.wheatherapp.data.local.entity

import androidx.room.Entity
import com.wheatherapp.domain.model.WeatherForecast

/**
 * Room entity representing cached weather forecast data for offline persistence.
 */
@Entity(tableName = "cached_forecasts", primaryKeys = ["latitude", "longitude"])
data class CachedForecastEntity(
    val latitude: Double,
    val longitude: Double,
    val locationName: String,
    val country: String?,
    val forecast: WeatherForecast,
    val cachedTimestampMillis: Long
)
