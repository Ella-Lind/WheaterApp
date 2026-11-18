package com.wheatherapp.data.repository

import com.wheatherapp.data.local.CachedForecastDao
import com.wheatherapp.data.local.FavouriteLocationDao
import com.wheatherapp.data.local.UserSettingsDao
import com.wheatherapp.data.local.entity.CachedForecastEntity
import com.wheatherapp.data.local.entity.FavouriteLocationEntity
import com.wheatherapp.data.local.entity.UserSettingsEntity
import com.wheatherapp.data.network.NetworkMonitor
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException

/**
 * Implementation of [WeatherRepository] coordinating Retrofit API calls,
 * JSON parsers, coordinate validation, and Room persistence caching.
 */
class WeatherRepositoryImpl(
    private val openMeteoApi: OpenMeteoApi,
    private val geocodingApi: GeocodingApi,
    private val forecastJsonParser: ForecastJsonParser,
    private val geocodingJsonParser: GeocodingJsonParser,
    private val cachedForecastDao: CachedForecastDao,
    private val favouriteLocationDao: FavouriteLocationDao,
    private val userSettingsDao: UserSettingsDao,
    private val networkMonitor: NetworkMonitor,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : WeatherRepository {

    override suspend fun searchLocations(query: String): Result<List<LocationPlace>> =
        withContext(ioDispatcher) {
            searchLocationsOnIo(query)
        }

    private suspend fun searchLocationsOnIo(query: String): Result<List<LocationPlace>> {
        val trimmedQuery = query.trim()
        try {
            if (trimmedQuery.isBlank()) return Result.success(emptyList())

            val coordPair = CoordinateValidator.parseCoordinates(trimmedQuery)
            if (coordPair != null) {
                val (lat, lon) = coordPair
                val isFav = favouriteLocationDao.isFavourite(lat, lon) > 0
                return Result.success(
                    listOf(
                        LocationPlace(
                            name = "Lat: %.4f, Lon: %.4f".format(lat, lon),
                            country = "Coordinates Input",
                            latitude = lat,
                            longitude = lon,
                            isFavourite = isFav
                        )
                    )
                )
            }

            if (!networkMonitor.isNetworkAvailable()) {
                return cachedSearchResult(trimmedQuery)
            }

            val rawJsonString = geocodingApi.searchLocation(
                url = "https://geocoding-api.open-meteo.com/v1/search",
                name = trimmedQuery
            )
            val places = geocodingJsonParser.parse(rawJsonString)

            return Result.success(places.map { place ->
                val isFav = favouriteLocationDao.isFavourite(place.latitude, place.longitude) > 0
                place.copy(isFavourite = isFav)
            })
        } catch (e: CancellationException) {
            throw e
        } catch (e: HttpException) {
            return Result.failure(
                IllegalStateException(
                    "HTTP error ${e.code()} while searching locations: ${e.message()}",
                    e
                )
            )
        } catch (e: IOException) {
            val cachedResult = cachedSearchResult(trimmedQuery)
            return if (cachedResult.isSuccess) cachedResult else Result.failure(
                IllegalStateException(
                    "Network unavailable while searching locations.",
                    e
                )
            )
        } catch (e: Exception) {
            val trimmedQuery = query.trim()
            if (trimmedQuery.isNotBlank()) {
                try {
                    val cachedResult = cachedSearchResult(trimmedQuery)
                    if (cachedResult.isSuccess) return cachedResult
                } catch (cacheException: CancellationException) {
                    throw cacheException
                } catch (_: Exception) {
                    // Return the original network/search error below.
                }
            }
            return Result.failure(
                IllegalStateException(
                    "Unable to search locations. Check your internet connection and try again.",
                    e
                )
            )
        }
    }

    override suspend fun getForecastForLocation(location: LocationPlace): Result<WeatherForecast> =
        withContext(ioDispatcher) {
            if (!networkMonitor.isNetworkAvailable()) {
                return@withContext cachedForecastResult(
                    location,
                    IllegalStateException("No internet connection is available.")
                )
            }

            val forecast = try {
                val rawJsonString = openMeteoApi.getForecast(
                    latitude = location.latitude,
                    longitude = location.longitude
                )
                forecastJsonParser.parse(
                    jsonString = rawJsonString,
                    location = location
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: HttpException) {
                return@withContext Result.failure(
                    IllegalStateException(
                        "HTTP error ${e.code()} while loading the forecast: ${e.message()}",
                        e
                    )
                )
            } catch (e: IOException) {
                return@withContext cachedForecastResult(location, e)
            } catch (e: Exception) {
                return@withContext cachedForecastResult(location, e)
            }

            try {
                cachedForecastDao.insertForecast(
                    CachedForecastEntity(
                        latitude = location.latitude,
                        longitude = location.longitude,
                        locationName = location.name,
                        country = location.country,
                        forecast = forecast,
                        cachedTimestampMillis = System.currentTimeMillis()
                    )
                )
                Result.success(forecast)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Result.failure(
                    IllegalStateException(
                        "Forecast fetched successfully, but could not be saved offline.",
                        e
                    )
                )
            }
        }

    override suspend fun getLatestCachedForecast(): Result<WeatherForecast> =
        withContext(ioDispatcher) {
            try {
                val cachedEntity = cachedForecastDao.getLatestCachedForecast()
                if (cachedEntity == null) {
                    val message = if (!networkMonitor.isNetworkAvailable()) {
                        "No internet connection and no cached forecast available"
                    } else {
                        "No cached forecast available"
                    }
                    Result.failure(
                        IllegalStateException(
                            message
                        )
                    )
                } else {
                    Result.success(cachedEntity.forecast.copy(isFromCache = true))
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Result.failure(
                    IllegalStateException(
                        "Unable to read the latest cached forecast. Try again.",
                        e
                    )
                )
            }
        }

    private suspend fun cachedForecastResult(
        location: LocationPlace,
        cause: Throwable
    ): Result<WeatherForecast> {
        return try {
            val cachedEntity = cachedForecastDao.getCachedForecast(location.latitude, location.longitude)
            if (cachedEntity != null) {
                Result.success(cachedEntity.forecast.copy(isFromCache = true))
            } else {
                Result.failure(
                    IllegalStateException(
                        "No cached forecast is available for ${location.name}. " +
                            "Connect to the internet and try again.",
                        cause
                    )
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(
                IllegalStateException(
                    "Unable to read cached forecast data for ${location.name}. Try again.",
                    e
                )
            )
        }
    }

    private suspend fun cachedSearchResult(query: String): Result<List<LocationPlace>> {
        return try {
            val cachedEntities = cachedForecastDao.searchCachedForecasts(query)
            if (cachedEntities.isEmpty()) {
                Result.failure(
                    IllegalStateException(
                        "No internet connection is available and no cached location matches \"$query\"."
                    )
                )
            } else {
                Result.success(
                    cachedEntities.map { entity ->
                        LocationPlace(
                            name = entity.locationName,
                            country = entity.country,
                            latitude = entity.latitude,
                            longitude = entity.longitude,
                            isFavourite = favouriteLocationDao.isFavourite(
                                entity.latitude,
                                entity.longitude
                            ) > 0
                        )
                    }
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(
                IllegalStateException(
                    "Unable to read cached location data. Try again.",
                    e
                )
            )
        }
    }

    override suspend fun getForecastForCoordinates(
        latitude: Double,
        longitude: Double,
        placeName: String
    ): Result<WeatherForecast> {
        val location = LocationPlace(
            name = placeName,
            country = null,
            latitude = latitude,
            longitude = longitude
        )
        return getForecastForLocation(location)
    }

    override fun getFavouriteLocations(): Flow<List<LocationPlace>> {
        return favouriteLocationDao.getFavouriteLocations().map { entities ->
            entities.map { entity ->
                LocationPlace(
                    name = entity.name,
                    country = entity.country,
                    latitude = entity.latitude,
                    longitude = entity.longitude,
                    isFavourite = true
                )
            }
        }
    }

    override suspend fun addFavouriteLocation(location: LocationPlace) =
        withContext(ioDispatcher) {
            favouriteLocationDao.insertFavourite(
                FavouriteLocationEntity(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    name = location.name,
                    country = location.country,
                    addedTimestampMillis = System.currentTimeMillis()
                )
            )
        }

    override suspend fun removeFavouriteLocation(latitude: Double, longitude: Double) =
        withContext(ioDispatcher) {
            favouriteLocationDao.removeFavourite(latitude, longitude)
        }

    override suspend fun isFavouriteLocation(latitude: Double, longitude: Double): Boolean =
        withContext(ioDispatcher) {
            favouriteLocationDao.isFavourite(latitude, longitude) > 0
        }

    override fun getUserSettings(): Flow<UserSettings> {
        return userSettingsDao.getUserSettings().map { entity ->
            if (entity != null) {
                UserSettings(
                    temperatureUnit = entity.temperatureUnit,
                    speedUnit = entity.speedUnit,
                    refreshIntervalMinutes = entity.refreshIntervalMinutes,
                    themeMode = entity.themeMode
                )
            } else {
                UserSettings()
            }
        }
    }

    override suspend fun updateUserSettings(settings: UserSettings) =
        withContext(ioDispatcher) {
            userSettingsDao.insertOrUpdateSettings(
                UserSettingsEntity(
                    id = 1,
                    temperatureUnit = settings.temperatureUnit,
                    speedUnit = settings.speedUnit,
                    refreshIntervalMinutes = settings.refreshIntervalMinutes,
                    themeMode = settings.themeMode
                )
            )
        }
}
