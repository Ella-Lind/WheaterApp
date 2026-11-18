package com.wheatherapp.data.local

import androidx.room.TypeConverter
import com.wheatherapp.domain.model.WeatherForecast
import kotlinx.serialization.json.Json

/**
 * Room type converters using kotlinx.serialization Json for complex data types.
 */
class WeatherTypeConverters {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    @TypeConverter
    fun fromWeatherForecast(forecast: WeatherForecast): String {
        return json.encodeToString(WeatherForecast.serializer(), forecast)
    }

    @TypeConverter
    fun toWeatherForecast(forecastJson: String): WeatherForecast {
        return json.decodeFromString(WeatherForecast.serializer(), forecastJson)
    }
}
