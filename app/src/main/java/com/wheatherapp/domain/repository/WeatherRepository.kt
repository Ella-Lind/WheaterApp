package com.wheatherapp.domain.repository

import com.wheatherapp.domain.model.LocationPlace
import com.wheatherapp.domain.model.UserSettings
import com.wheatherapp.domain.model.WeatherForecast
import kotlinx.coroutines.flow.Flow

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

    /**
     * Observes the list of user favourite locations.
     */
    fun getFavouriteLocations(): Flow<List<LocationPlace>>

    /**
     * Adds a location to the list of favourites.
     */
    suspend fun addFavouriteLocation(location: LocationPlace): Result<Unit>

    /**
     * Removes a location from the list of favourites.
     */
    suspend fun removeFavouriteLocation(location: LocationPlace): Result<Unit>

    /**
     * Checks if a location is marked as favourite.
     */
    suspend fun isLocationFavourite(latitude: Double, longitude: Double): Boolean

    /**
     * Observes current user settings (units, refresh interval, theme).
     */
    fun getUserSettings(): Flow<UserSettings>

    /**
     * Updates current user settings.
     */
    suspend fun updateUserSettings(settings: UserSettings): Result<Unit>
}

