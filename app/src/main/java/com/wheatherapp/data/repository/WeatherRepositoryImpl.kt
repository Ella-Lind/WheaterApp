package com.wheatherapp.data.repository

import com.wheatherapp.data.parser.ForecastJsonParser
import com.wheatherapp.data.parser.GeocodingJsonParser
import com.wheatherapp.data.remote.GeocodingApi
import com.wheatherapp.data.remote.OpenMeteoApi
import com.wheatherapp.domain.model.LocationPlace
import com.wheatherapp.domain.model.WeatherForecast
import com.wheatherapp.domain.repository.WeatherRepository
import com.wheatherapp.domain.util.CoordinateValidator
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Implementation of [WeatherRepository] coordinating Retrofit API calls,
 * JSON parsers, and coordinate validation.
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
                geocodingJsonParser.parse(rawJsonString)
            }
        }

    override suspend fun getForecastForLocation(location: LocationPlace): Result<WeatherForecast> =
        withContext(ioDispatcher) {
            runCatching {
                val rawJsonString = openMeteoApi.getForecast(
                    latitude = location.latitude,
                    longitude = location.longitude
                )
                forecastJsonParser.parse(
                    jsonString = rawJsonString,
                    location = location
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
}
