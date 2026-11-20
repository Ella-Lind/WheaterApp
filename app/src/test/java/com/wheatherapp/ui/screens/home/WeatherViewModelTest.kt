package com.wheatherapp.ui.screens.home

import com.wheatherapp.data.network.NetworkMonitor
import com.wheatherapp.domain.model.CurrentWeather
import com.wheatherapp.domain.model.LocationPlace
import com.wheatherapp.domain.model.UserSettings
import com.wheatherapp.domain.model.WeatherCondition
import com.wheatherapp.domain.model.WeatherForecast
import com.wheatherapp.domain.repository.WeatherRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class WeatherViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private class FakeWeatherRepository : WeatherRepository {
        var latestCachedForecastResult: Result<WeatherForecast> = Result.failure(Exception("No cache"))
        var forecastResult: Result<WeatherForecast> = Result.failure(Exception("No network"))
        var searchResult: Result<List<LocationPlace>> = Result.success(emptyList())

        val favouriteList = mutableListOf<LocationPlace>()
        val settingsFlow = MutableStateFlow(UserSettings())

        override suspend fun searchLocations(query: String): Result<List<LocationPlace>> = searchResult

        override suspend fun getForecastForLocation(location: LocationPlace): Result<WeatherForecast> = forecastResult

        override suspend fun getForecastForCoordinates(latitude: Double, longitude: Double, placeName: String): Result<WeatherForecast> = forecastResult

        override suspend fun getLatestCachedForecast(): Result<WeatherForecast> = latestCachedForecastResult

        override fun getFavouriteLocations(): Flow<List<LocationPlace>> = flowOf(favouriteList)

        override suspend fun addFavouriteLocation(location: LocationPlace): Result<Unit> {
            favouriteList.add(location.copy(isFavourite = true))
            return Result.success(Unit)
        }

        override suspend fun removeFavouriteLocation(location: LocationPlace): Result<Unit> {
            favouriteList.removeIf { it.name == location.name }
            return Result.success(Unit)
        }

        override suspend fun removeFavouriteLocation(latitude: Double, longitude: Double): Result<Unit> {
            favouriteList.removeIf { it.latitude == latitude && it.longitude == longitude }
            return Result.success(Unit)
        }

        override suspend fun isLocationFavourite(latitude: Double, longitude: Double): Boolean =
            favouriteList.any { it.latitude == latitude && it.longitude == longitude }

        override fun getUserSettings(): Flow<UserSettings> = settingsFlow

        override suspend fun updateUserSettings(settings: UserSettings): Result<Unit> {
            settingsFlow.value = settings
            return Result.success(Unit)
        }
    }

    private val fakeRepo = FakeWeatherRepository()
    private val fakeNetworkMonitor = NetworkMonitor(testOnline = true)

    private fun sampleForecast(name: String = "Stockholm"): WeatherForecast {
        val loc = LocationPlace(name, "Sweden", 59.3293, 18.0686)
        val current = CurrentWeather(18.0, 50, 17.5, 0.0, WeatherCondition.CLEAR_SKY, 10.0, 180)
        return WeatherForecast(
            location = loc,
            current = current,
            todayHourly = emptyList(),
            daily = emptyList(),
            fetchedTimestampMillis = System.currentTimeMillis()
        )
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun startup_loadsLiveForecast_whenOnline() = runTest(testDispatcher) {
        fakeRepo.forecastResult = Result.success(sampleForecast("Stockholm"))

        val viewModel = WeatherViewModel(fakeRepo, fakeNetworkMonitor)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value is WeatherUiState.Success)
        val state = viewModel.uiState.value as WeatherUiState.Success
        assertEquals("Stockholm", state.forecast.location.name)
    }

    @Test
    fun startup_loadsCachedForecast_whenOffline() = runTest(testDispatcher) {
        val offlineMonitor = NetworkMonitor(testOnline = false)
        fakeRepo.latestCachedForecastResult = Result.success(sampleForecast("Cached City").copy(isFromCache = true))

        val viewModel = WeatherViewModel(fakeRepo, offlineMonitor)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value is WeatherUiState.Offline)
        val state = viewModel.uiState.value as WeatherUiState.Offline
        assertEquals("Cached City", state.cachedForecast?.location?.name)
    }

    @Test
    fun forecastFailure_distinguishesIOExceptionAsNetworkError() = runTest(testDispatcher) {
        val offlineMonitor = NetworkMonitor(testOnline = true)
        fakeRepo.forecastResult = Result.failure(IOException("No route to host"))

        val viewModel = WeatherViewModel(fakeRepo, offlineMonitor)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value is WeatherUiState.Error)
        val errorState = viewModel.uiState.value as WeatherUiState.Error
        assertTrue(errorState.isNetworkError)
        assertEquals(null, errorState.httpCode)
    }

    @Test
    fun forecastFailure_distinguishesHttpExceptionWithHttpCode() = runTest(testDispatcher) {
        val response = Response.error<String>(404, "Not Found".toResponseBody(null))
        val httpException = HttpException(response)
        fakeRepo.forecastResult = Result.failure(httpException)

        val viewModel = WeatherViewModel(fakeRepo, fakeNetworkMonitor)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value is WeatherUiState.Error)
        val errorState = viewModel.uiState.value as WeatherUiState.Error
        assertEquals(false, errorState.isNetworkError)
        assertEquals(404, errorState.httpCode)
    }
}
