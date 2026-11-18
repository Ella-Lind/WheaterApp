package com.wheatherapp.data.repository

import com.wheatherapp.data.parser.ForecastJsonParser
import com.wheatherapp.data.parser.GeocodingJsonParser
import com.wheatherapp.data.remote.GeocodingApi
import com.wheatherapp.data.remote.OpenMeteoApi
import com.wheatherapp.domain.model.LocationPlace
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WeatherRepositoryTest {

    private val testDispatcher = StandardTestDispatcher()

    private val fakeOpenMeteoApi = object : OpenMeteoApi {
        override suspend fun getForecast(
            latitude: Double,
            longitude: Double,
            current: String,
            hourly: String,
            daily: String,
            timezone: String,
            forecastDays: Int
        ): String {
            return """
            {
              "latitude": 59.3293,
              "longitude": 18.0686,
              "current": { "temperature_2m": 10.0, "weather_code": 0 },
              "hourly": { "time": ["2025-02-17T00:00"], "temperature_2m": [10.0] },
              "daily": { "time": ["2025-02-17"], "weather_code": [0] }
            }
            """.trimIndent()
        }
    }

    private val fakeGeocodingApi = object : GeocodingApi {
        override suspend fun searchLocation(
            name: String,
            count: Int,
            language: String,
            format: String
        ): String {
            return """
            {
              "results": [
                { "name": "Stockholm", "latitude": 59.3293, "longitude": 18.0686, "country": "Sweden" }
              ]
            }
            """.trimIndent()
        }
    }

    private val repository = WeatherRepositoryImpl(
        openMeteoApi = fakeOpenMeteoApi,
        geocodingApi = fakeGeocodingApi,
        forecastJsonParser = ForecastJsonParser(),
        geocodingJsonParser = GeocodingJsonParser(),
        ioDispatcher = testDispatcher
    )

    @Test
    fun searchLocations_runsOnIoDispatcher_andReturnsResults() = runTest(testDispatcher) {
        val result = repository.searchLocations("Stockholm")
        assertTrue(result.isSuccess)
        val locations = result.getOrThrow()
        assertEquals(1, locations.size)
        assertEquals("Stockholm", locations[0].name)
    }

    @Test
    fun getForecastForLocation_runsOnIoDispatcher_andReturnsParsedForecast() = runTest(testDispatcher) {
        val location = LocationPlace(
            name = "Stockholm",
            country = "Sweden",
            latitude = 59.3293,
            longitude = 18.0686
        )
        val result = repository.getForecastForLocation(location)
        assertTrue(result.isSuccess)
        val forecast = result.getOrThrow()
        assertEquals(10.0, forecast.current.temperatureCelsius, 0.01)
    }
}
