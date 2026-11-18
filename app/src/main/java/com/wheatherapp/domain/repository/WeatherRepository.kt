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
    suspend fun getForecastForCoordinates(latitude: Double, longitude: Double, placeName: String): Result<WeatherForecast>
    suspend fun getLatestCachedForecast(): Result<WeatherForecast>

    fun getFavouriteLocations(): Flow<List<LocationPlace>>
    suspend fun addFavouriteLocation(location: LocationPlace)
    suspend fun removeFavouriteLocation(latitude: Double, longitude: Double)
    suspend fun isFavouriteLocation(latitude: Double, longitude: Double): Boolean

    fun getUserSettings(): Flow<UserSettings>
    suspend fun updateUserSettings(settings: UserSettings)
}
