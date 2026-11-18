package com.wheatherapp.data.repository

import com.wheatherapp.domain.model.CurrentWeather
import com.wheatherapp.domain.model.DailyWeather
import com.wheatherapp.domain.model.HourlyWeather
import com.wheatherapp.domain.model.LocationPlace
import com.wheatherapp.domain.model.UserSettings
import com.wheatherapp.domain.model.WeatherCondition
import com.wheatherapp.domain.model.WeatherForecast
import com.wheatherapp.domain.repository.WeatherRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class FakeWeatherRepository : WeatherRepository {

    var shouldReturnError = false
    var errorMessage = "Fake repository error"

    val sampleLocation = LocationPlace(
        name = "Stockholm",
        country = "Sweden",
        latitude = 59.3293,
        longitude = 18.0686
    )

    val sampleForecast = WeatherForecast(
        location = sampleLocation,
        current = CurrentWeather(
            temperatureCelsius = 15.0,
            relativeHumidityPercent = 60,
            apparentTemperatureCelsius = 14.0,
            precipitationMm = 0.0,
            weatherCondition = WeatherCondition.CLEAR_SKY,
            windSpeedKmh = 10.0,
            windDirectionDegrees = 180
        ),
        todayHourly = listOf(
            HourlyWeather("2025-02-17T12:00", 15.0, 0, WeatherCondition.CLEAR_SKY, 10.0)
        ),
        daily = listOf(
            DailyWeather(
                dateIso = "2025-02-17",
                weatherCondition = WeatherCondition.CLEAR_SKY,
                maxTemperatureCelsius = 18.0,
                minTemperatureCelsius = 10.0,
                precipitationSumMm = 0.0,
                maxPrecipitationProbabilityPercent = 0,
                maxWindSpeedKmh = 12.0,
                hourlyForecast = listOf(
                    HourlyWeather("2025-02-17T12:00", 15.0, 0, WeatherCondition.CLEAR_SKY, 10.0)
                )
            )
        ),
        fetchedTimestampMillis = System.currentTimeMillis()
    )

    private val favouritesFlow = MutableStateFlow<List<LocationPlace>>(emptyList())
    private val settingsFlow = MutableStateFlow(UserSettings())

    override suspend fun searchLocations(query: String): Result<List<LocationPlace>> {
        if (shouldReturnError) return Result.failure(RuntimeException(errorMessage))
        return Result.success(listOf(sampleLocation))
    }

    override suspend fun getForecastForLocation(location: LocationPlace): Result<WeatherForecast> {
        if (shouldReturnError) return Result.failure(RuntimeException(errorMessage))
        return Result.success(sampleForecast.copy(location = location))
    }

    override suspend fun getForecastForCoordinates(
        latitude: Double,
        longitude: Double,
        placeName: String
    ): Result<WeatherForecast> {
        if (shouldReturnError) return Result.failure(RuntimeException(errorMessage))
        val loc = LocationPlace(placeName, null, latitude, longitude)
        return Result.success(sampleForecast.copy(location = loc))
    }

    override suspend fun getLatestCachedForecast(): Result<WeatherForecast> {
        if (shouldReturnError) return Result.failure(RuntimeException(errorMessage))
        return Result.success(sampleForecast.copy(isFromCache = true))
    }

    override fun getFavouriteLocations(): Flow<List<LocationPlace>> = favouritesFlow.asStateFlow()

    override suspend fun addFavouriteLocation(location: LocationPlace): Result<Unit> {
        if (shouldReturnError) return Result.failure(RuntimeException(errorMessage))
        favouritesFlow.update { it + location.copy(isFavourite = true) }
        return Result.success(Unit)
    }

    override suspend fun removeFavouriteLocation(location: LocationPlace): Result<Unit> {
        return removeFavouriteLocation(location.latitude, location.longitude)
    }

    override suspend fun removeFavouriteLocation(latitude: Double, longitude: Double): Result<Unit> {
        if (shouldReturnError) return Result.failure(RuntimeException(errorMessage))
        favouritesFlow.update { list ->
            list.filterNot { it.latitude == latitude && it.longitude == longitude }
        }
        return Result.success(Unit)
    }

    override suspend fun isLocationFavourite(latitude: Double, longitude: Double): Boolean {
        return favouritesFlow.value.any { it.latitude == latitude && it.longitude == longitude }
    }

    override fun getUserSettings(): Flow<UserSettings> = settingsFlow.asStateFlow()

    override suspend fun updateUserSettings(settings: UserSettings): Result<Unit> {
        if (shouldReturnError) return Result.failure(RuntimeException(errorMessage))
        settingsFlow.value = settings
        return Result.success(Unit)
    }
}
