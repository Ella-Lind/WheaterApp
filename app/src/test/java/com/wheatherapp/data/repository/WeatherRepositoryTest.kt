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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class WeatherRepositoryTest {

    private val testDispatcher = StandardTestDispatcher()

    private class InMemoryCachedForecastDao : CachedForecastDao {
        val items = mutableMapOf<Pair<Double, Double>, CachedForecastEntity>()
        var cacheWriteFailure: Exception? = null

        override suspend fun getCachedForecast(
            latitude: Double,
            longitude: Double
        ): CachedForecastEntity? = items[Pair(latitude, longitude)]

        override suspend fun searchCachedForecasts(query: String): List<CachedForecastEntity> =
            items.values
                .filter {
                    it.locationName.contains(query, ignoreCase = true) ||
                        it.country?.contains(query, ignoreCase = true) == true
                }
                .sortedByDescending { it.cachedTimestampMillis }

        override suspend fun getLatestCachedForecast(): CachedForecastEntity? =
            items.values.maxByOrNull { it.cachedTimestampMillis }

        override suspend fun insertForecast(forecast: CachedForecastEntity) {
            cacheWriteFailure?.let { throw it }
            items[Pair(forecast.latitude, forecast.longitude)] = forecast
        }

        override suspend fun deleteOldForecasts(maxAgeMillis: Long) {
            items.entries.removeAll { it.value.cachedTimestampMillis < maxAgeMillis }
        }
    }

    private val fakeCachedForecastDao = InMemoryCachedForecastDao()

    private val fakeFavouriteLocationDao = object : FavouriteLocationDao {
        override fun getFavouriteLocations(): Flow<List<FavouriteLocationEntity>> =
            flowOf(emptyList())

        override suspend fun insertFavourite(favourite: FavouriteLocationEntity) = Unit

        override suspend fun removeFavourite(latitude: Double, longitude: Double) = Unit

        override suspend fun isFavourite(latitude: Double, longitude: Double): Int = 0
    }

    private val fakeUserSettingsDao = object : UserSettingsDao {
        override fun getUserSettings(): Flow<UserSettingsEntity?> = flowOf(null)

        override suspend fun getUserSettingsSync(): UserSettingsEntity? = null

        override suspend fun insertOrUpdateSettings(settings: UserSettingsEntity) = Unit
    }

    private val stockholm = LocationPlace(
        name = "Stockholm",
        country = "Sweden",
        latitude = 59.3293,
        longitude = 18.0686
    )

    private fun forecastJson(): String = """
        {
          "latitude": 59.3293,
          "longitude": 18.0686,
          "current": {
            "temperature_2m": 10.0,
            "relative_humidity_2m": 70,
            "apparent_temperature": 9.0,
            "precipitation": 0.0,
            "weather_code": 0,
            "wind_speed_10m": 5.0,
            "wind_direction_10m": 180
          },
          "hourly": {
            "time": ["2025-02-17T00:00"],
            "temperature_2m": [10.0],
            "precipitation_probability": [10],
            "weather_code": [0],
            "wind_speed_10m": [5.0]
          },
          "daily": {
            "time": ["2025-02-17"],
            "weather_code": [0],
            "temperature_2m_max": [12.0],
            "temperature_2m_min": [6.0],
            "precipitation_sum": [0.0],
            "precipitation_probability_max": [10],
            "wind_speed_10m_max": [8.0]
          }
        }
    """.trimIndent()

    private fun createRepository(
        isOnline: Boolean,
        openMeteoApi: OpenMeteoApi? = null,
        geocodingApi: GeocodingApi? = null
    ): WeatherRepositoryImpl {
        val forecastApi = openMeteoApi ?: object : OpenMeteoApi {
            override suspend fun getForecast(
                latitude: Double,
                longitude: Double,
                current: String,
                hourly: String,
                daily: String,
                timezone: String,
                forecastDays: Int
            ): String = forecastJson()
        }
        val locationApi = geocodingApi ?: object : GeocodingApi {
            override suspend fun searchLocation(
                url: String,
                name: String,
                count: Int,
                language: String,
                format: String
            ): String = """
                {
                  "results": [
                    {
                      "name": "Stockholm",
                      "latitude": 59.3293,
                      "longitude": 18.0686,
                      "country": "Sweden"
                    }
                  ]
                }
            """.trimIndent()
        }

        return WeatherRepositoryImpl(
            openMeteoApi = forecastApi,
            geocodingApi = locationApi,
            forecastJsonParser = ForecastJsonParser(),
            geocodingJsonParser = GeocodingJsonParser(),
            cachedForecastDao = fakeCachedForecastDao,
            favouriteLocationDao = fakeFavouriteLocationDao,
            userSettingsDao = fakeUserSettingsDao,
            networkMonitor = NetworkMonitor(testOnline = isOnline),
            ioDispatcher = testDispatcher
        )
    }

    private fun cacheStockholm(timestamp: Long = 1234L) {
        val forecast = ForecastJsonParser().parse(forecastJson(), stockholm)
        fakeCachedForecastDao.items[Pair(stockholm.latitude, stockholm.longitude)] =
            CachedForecastEntity(
                latitude = stockholm.latitude,
                longitude = stockholm.longitude,
                locationName = stockholm.name,
                country = stockholm.country,
                forecast = forecast,
                cachedTimestampMillis = timestamp
            )
    }

    @Test
    fun onlineFetch_persistsForecastAndLatestCacheRetrievesIt() = runTest(testDispatcher) {
        val repository = createRepository(isOnline = true)

        val fetchResult = repository.getForecastForLocation(stockholm)
        assertTrue(fetchResult.isSuccess)

        assertEquals(1, fakeCachedForecastDao.items.size)
        val cachedResult = repository.getLatestCachedForecast()
        assertTrue(cachedResult.isSuccess)
        assertTrue(cachedResult.getOrThrow().isFromCache)
        assertEquals(10.0, cachedResult.getOrThrow().current.temperatureCelsius, 0.01)
    }

    @Test
    fun offlineStartup_returnsLatestCacheOrClearFailure() = runTest(testDispatcher) {
        val repository = createRepository(isOnline = false)
        fakeCachedForecastDao.items.clear()

        val emptyResult = repository.getLatestCachedForecast()
        assertTrue(emptyResult.isFailure)
        assertTrue(
            emptyResult.exceptionOrNull()?.message
                ?.contains("No internet connection") == true
        )

        cacheStockholm()
        val cachedResult = repository.getLatestCachedForecast()
        assertTrue(cachedResult.isSuccess)
        assertTrue(cachedResult.getOrThrow().isFromCache)
    }

    @Test
    fun offlineCitySearch_returnsCachedLocationOrSafeFailure() = runTest(testDispatcher) {
        val repository = createRepository(isOnline = false)
        fakeCachedForecastDao.items.clear()

        val emptyResult = repository.searchLocations("Stockholm")
        assertTrue(emptyResult.isFailure)
        assertFalse(emptyResult.isSuccess)

        cacheStockholm()
        val cachedResult = repository.searchLocations("Stockholm")
        assertTrue(cachedResult.isSuccess)
        assertEquals(stockholm.name, cachedResult.getOrThrow().first().name)
    }

    @Test
    fun offlineCoordinateLookup_returnsCacheOrSafeFailure() = runTest(testDispatcher) {
        val repository = createRepository(isOnline = false)
        fakeCachedForecastDao.items.clear()

        val emptyResult = repository.getForecastForCoordinates(
            latitude = stockholm.latitude,
            longitude = stockholm.longitude,
            placeName = stockholm.name
        )
        assertTrue(emptyResult.isFailure)

        cacheStockholm()
        val cachedResult = repository.getForecastForCoordinates(
            latitude = stockholm.latitude,
            longitude = stockholm.longitude,
            placeName = stockholm.name
        )
        assertTrue(cachedResult.isSuccess)
        assertTrue(cachedResult.getOrThrow().isFromCache)
    }

    @Test
    fun httpError_isDistinctFromNetworkIOException() = runTest(testDispatcher) {
        val httpApi = object : OpenMeteoApi {
            override suspend fun getForecast(
                latitude: Double,
                longitude: Double,
                current: String,
                hourly: String,
                daily: String,
                timezone: String,
                forecastDays: Int
            ): String {
                throw HttpException(
                    Response.error<String>(
                        503,
                        "service unavailable".toResponseBody("text/plain".toMediaType())
                    )
                )
            }
        }
        cacheStockholm()
        val httpRepository = createRepository(isOnline = true, openMeteoApi = httpApi)
        val httpResult = httpRepository.getForecastForLocation(stockholm)
        assertTrue(httpResult.isFailure)
        assertTrue(httpResult.exceptionOrNull()?.message?.contains("HTTP error 503") == true)

        val networkApi = object : OpenMeteoApi {
            override suspend fun getForecast(
                latitude: Double,
                longitude: Double,
                current: String,
                hourly: String,
                daily: String,
                timezone: String,
                forecastDays: Int
            ): String = throw IOException("network unreachable")
        }
        val networkRepository = createRepository(isOnline = true, openMeteoApi = networkApi)
        val networkResult = networkRepository.getForecastForLocation(stockholm)
        assertTrue(networkResult.isSuccess)
        assertTrue(networkResult.getOrThrow().isFromCache)
    }

    @Test
    fun cacheWriteFailure_returnsResultFailure() = runTest(testDispatcher) {
        val repository = createRepository(isOnline = true)
        fakeCachedForecastDao.cacheWriteFailure = IOException("Room write failed")

        val result = repository.getForecastForLocation(stockholm)

        assertTrue(result.isFailure)
        assertTrue(
            result.exceptionOrNull()?.message
                ?.contains("could not be saved offline") == true
        )
        fakeCachedForecastDao.cacheWriteFailure = null
    }

    @Test
    fun searchLocations_blankQuery_returnsEmptySuccessList() = runTest(testDispatcher) {
        val repository = createRepository(isOnline = true)
        val result = repository.searchLocations("   ")
        assertTrue(result.isSuccess)
        assertTrue(result.getOrThrow().isEmpty())
    }

    @Test
    fun searchLocations_validCoordinatesQuery_returnsCoordinatesLocation() = runTest(testDispatcher) {
        val repository = createRepository(isOnline = true)
        val result = repository.searchLocations("59.3293, 18.0686")
        assertTrue(result.isSuccess)
        val places = result.getOrThrow()
        assertEquals(1, places.size)
        assertEquals(59.3293, places[0].latitude, 0.0001)
        assertEquals(18.0686, places[0].longitude, 0.0001)
    }

    @Test
    fun searchLocations_httpError_returnsFailureWithHttpDetails() = runTest(testDispatcher) {
        val httpGeocodingApi = object : GeocodingApi {
            override suspend fun searchLocation(
                url: String,
                name: String,
                count: Int,
                language: String,
                format: String
            ): String {
                throw HttpException(
                    Response.error<String>(
                        500,
                        "Internal Server Error".toResponseBody("text/plain".toMediaType())
                    )
                )
            }
        }
        val repository = createRepository(isOnline = true, geocodingApi = httpGeocodingApi)
        fakeCachedForecastDao.items.clear()
        val result = repository.searchLocations("Stockholm")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("HTTP error 500") == true)
    }

    @Test
    fun addFavouriteLocation_daoError_returnsFailureResult() = runTest(testDispatcher) {
        val failingFavDao = object : FavouriteLocationDao {
            override fun getFavouriteLocations(): Flow<List<FavouriteLocationEntity>> = flowOf(emptyList())
            override suspend fun insertFavourite(favourite: FavouriteLocationEntity) {
                throw IOException("DB disk full")
            }
            override suspend fun removeFavourite(latitude: Double, longitude: Double) {}
            override suspend fun isFavourite(latitude: Double, longitude: Double): Int = 0
        }

        val repository = WeatherRepositoryImpl(
            openMeteoApi = object : OpenMeteoApi {
                override suspend fun getForecast(latitude: Double, longitude: Double, current: String, hourly: String, daily: String, timezone: String, forecastDays: Int) = forecastJson()
            },
            geocodingApi = object : GeocodingApi {
                override suspend fun searchLocation(url: String, name: String, count: Int, language: String, format: String) = ""
            },
            forecastJsonParser = ForecastJsonParser(),
            geocodingJsonParser = GeocodingJsonParser(),
            cachedForecastDao = fakeCachedForecastDao,
            favouriteLocationDao = failingFavDao,
            userSettingsDao = fakeUserSettingsDao,
            networkMonitor = NetworkMonitor(testOnline = true),
            ioDispatcher = testDispatcher
        )

        val result = repository.addFavouriteLocation(stockholm)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("DB disk full") == true)
    }
}
