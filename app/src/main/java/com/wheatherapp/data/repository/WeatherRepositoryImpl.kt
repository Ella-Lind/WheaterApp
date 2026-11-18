package com.wheatherapp.data.repository

import com.wheatherapp.data.parser.ForecastJsonParser
import com.wheatherapp.data.parser.GeocodingJsonParser
import com.wheatherapp.data.remote.GeocodingApi
import com.wheatherapp.data.remote.OpenMeteoApi
import com.wheatherapp.domain.model.LocationPlace
import com.wheatherapp.domain.model.UserSettings
import com.wheatherapp.domain.model.WeatherForecast
import com.wheatherapp.domain.repository.WeatherRepository
import com.wheatherapp.domain.util.CoordinateValidator
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

/**
 * Implementation of [WeatherRepository] coordinating Retrofit API calls,
 * JSON parsers, coordinate validation, favourite locations, and user settings.
 *
 * All operations are main-safe and execute off the main thread on [ioDispatcher] (defaults to [Dispatchers.IO]).
 */
class WeatherRepositoryImpl(
    private val openMeteoApi: OpenMeteoApi,
    private val geocodingApi: GeocodingApi,
    private val forecastJsonParser: ForecastJsonParser,
    private val geocodingJsonParser: GeocodingJsonParser,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : WeatherRepository {

    private val _favouriteLocations = MutableStateFlow<List<LocationPlace>>(emptyList())
    private val _userSettings = MutableStateFlow(UserSettings())

    override suspend fun searchLocations(query: String): Result<List<LocationPlace>> =
        withContext(ioDispatcher) {
            runCatching {
                val trimmedQuery = query.trim()
                if (trimmedQuery.isBlank()) return@runCatching emptyList()

                // Check if user entered raw coordinates "lat, lon"
                val coordPair = CoordinateValidator.parseCoordinates(trimmedQuery)
                if (coordPair != null) {
                    val (lat, lon) = coordPair
                    return@runCatching listOf(
                        LocationPlace(
                            name = "Lat: %.4f, Lon: %.4f".format(lat, lon),
                            country = "Coordinates Input",
                            latitude = lat,
                            longitude = lon
                        )
                    )
                }

                // Query Geocoding API for place name
                val rawJsonString = geocodingApi.searchLocation(name = trimmedQuery)
                val results = geocodingJsonParser.parse(rawJsonString)

                // Mark locations as favourite if already saved
                val currentFavourites = _favouriteLocations.value
                results.map { place ->
                    val isFav = currentFavourites.any { fav ->
                        Math.abs(fav.latitude - place.latitude) < 0.0001 &&
                                Math.abs(fav.longitude - place.longitude) < 0.0001
                    }
                    place.copy(isFavourite = isFav)
                }
            }
        }

    override suspend fun getForecastForLocation(location: LocationPlace): Result<WeatherForecast> =
        withContext(ioDispatcher) {
            runCatching {
                val rawJsonString = openMeteoApi.getForecast(
                    latitude = location.latitude,
                    longitude = location.longitude
                )
                val forecast = forecastJsonParser.parse(
                    jsonString = rawJsonString,
                    location = location
                )
                val isFav = isLocationFavourite(location.latitude, location.longitude)
                forecast.copy(
                    location = location.copy(isFavourite = isFav)
                )
            }
        }

    override suspend fun getForecastForCoordinates(
        latitude: Double,
        longitude: Double,
        placeName: String
    ): Result<WeatherForecast> {
        val isFav = isLocationFavourite(latitude, longitude)
        val location = LocationPlace(
            name = placeName,
            country = null,
            latitude = latitude,
            longitude = longitude,
            isFavourite = isFav
        )
        return getForecastForLocation(location)
    }

    override fun getFavouriteLocations(): Flow<List<LocationPlace>> =
        _favouriteLocations.asStateFlow()

    override suspend fun addFavouriteLocation(location: LocationPlace): Result<Unit> =
        withContext(ioDispatcher) {
            runCatching {
                _favouriteLocations.update { currentList ->
                    if (currentList.none { fav ->
                            Math.abs(fav.latitude - location.latitude) < 0.0001 &&
                                    Math.abs(fav.longitude - location.longitude) < 0.0001
                        }) {
                        currentList + location.copy(isFavourite = true)
                    } else {
                        currentList
                    }
                }
            }
        }

    override suspend fun removeFavouriteLocation(location: LocationPlace): Result<Unit> =
        withContext(ioDispatcher) {
            runCatching {
                _favouriteLocations.update { currentList ->
                    currentList.filterNot { fav ->
                        Math.abs(fav.latitude - location.latitude) < 0.0001 &&
                                Math.abs(fav.longitude - location.longitude) < 0.0001
                    }
                }
            }
        }

    override suspend fun isLocationFavourite(latitude: Double, longitude: Double): Boolean =
        withContext(ioDispatcher) {
            _favouriteLocations.value.any { fav ->
                Math.abs(fav.latitude - latitude) < 0.0001 &&
                        Math.abs(fav.longitude - longitude) < 0.0001
            }
        }

    override fun getUserSettings(): Flow<UserSettings> =
        _userSettings.asStateFlow()

    override suspend fun updateUserSettings(settings: UserSettings): Result<Unit> =
        withContext(ioDispatcher) {
            runCatching {
                _userSettings.value = settings
            }
        }
}

