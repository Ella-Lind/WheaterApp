package com.wheatherapp.domain.repository

import com.wheatherapp.domain.model.LocationPlace
import com.wheatherapp.domain.model.WeatherForecast

interface WeatherRepository {
    /**
     * Searches for location places matching the query string, or parses raw "lat, lon" coordinates.
     */
    suspend fun searchLocations(query: String): Result<List<LocationPlace>>

    /**
     * Fetches forecast data for a specific location.
     */
    suspend fun getForecastForLocation(location: LocationPlace): Result<WeatherForecast>

    /**
     * Fetches forecast data directly for latitude and longitude coordinates.
     */
    suspend fun getForecastForCoordinates(
        latitude: Double,
        longitude: Double,
        placeName: String = "Selected Coordinates"
    ): Result<WeatherForecast>
}
