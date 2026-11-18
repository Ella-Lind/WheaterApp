package com.wheatherapp.domain.repository

import com.wheatherapp.domain.model.LocationPlace
import com.wheatherapp.domain.model.UserSettings
import com.wheatherapp.domain.model.WeatherForecast
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface defining data operations for weather forecasts,
 * location search, favourite places, and user settings.
 */
interface WeatherRepository {
    suspend fun searchLocations(query: String): Result<List<LocationPlace>>
    suspend fun getForecastForLocation(location: LocationPlace): Result<WeatherForecast>
    suspend fun getForecastForCoordinates(
        latitude: Double,
        longitude: Double,
        placeName: String = "Selected Coordinates"
    ): Result<WeatherForecast>
    suspend fun getLatestCachedForecast(): Result<WeatherForecast>

    /**
     * Observes the list of user favourite locations stored in Room.
     */
    fun getFavouriteLocations(): Flow<List<LocationPlace>>

    /**
     * Adds a location to the list of favourites in Room.
     */
    suspend fun addFavouriteLocation(location: LocationPlace): Result<Unit>

    /**
     * Removes a location from the list of favourites in Room by location place object.
     */
    suspend fun removeFavouriteLocation(location: LocationPlace): Result<Unit>

    /**
     * Removes a location from the list of favourites in Room by coordinates.
     */
    suspend fun removeFavouriteLocation(latitude: Double, longitude: Double): Result<Unit>

    /**
     * Checks if a location is marked as favourite in Room.
     */
    suspend fun isLocationFavourite(latitude: Double, longitude: Double): Boolean

    /**
     * Observes current user settings stored in Room (units, refresh interval, theme).
     */
    fun getUserSettings(): Flow<UserSettings>

    /**
     * Updates current user settings in Room.
     */
    suspend fun updateUserSettings(settings: UserSettings): Result<Unit>
}
