package com.wheatherapp.ui.screens.detail

import com.wheatherapp.data.repository.FakeWeatherRepository
import com.wheatherapp.util.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DayDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var fakeRepository: FakeWeatherRepository
    private lateinit var viewModel: DayDetailViewModel

    @Before
    fun setUp() {
        fakeRepository = FakeWeatherRepository()
        viewModel = DayDetailViewModel(fakeRepository)
    }

    @Test
    fun loadFromForecast_populatesState() = runTest {
        viewModel.loadFromForecast(fakeRepository.sampleForecast, "2025-02-17")

        val state = viewModel.uiState.value
        assertEquals("Stockholm", state.location?.name)
        assertEquals("2025-02-17", state.selectedDateIso)
        assertNotNull(state.dailyWeather)
        assertEquals(1, state.hourlyForecast.size)
    }

    @Test
    fun loadDayDetail_fetchesForecastAndPopulatesState() = runTest {
        viewModel.loadDayDetail(fakeRepository.sampleLocation, "2025-02-17")

        val state = viewModel.uiState.value
        assertEquals("Stockholm", state.location?.name)
        assertEquals("2025-02-17", state.selectedDateIso)
        assertNotNull(state.dailyWeather)
    }

    @Test
    fun loadDayDetail_failure_showsErrorMessage() = runTest {
        fakeRepository.shouldReturnError = true
        fakeRepository.errorMessage = "Failed to load"

        viewModel.loadDayDetail(fakeRepository.sampleLocation, "2025-02-17")

        val state = viewModel.uiState.value
        assertEquals("Failed to load", state.errorMessage)
    }

    @Test
    fun clearError_clearsErrorMessage() = runTest {
        fakeRepository.shouldReturnError = true
        viewModel.loadDayDetail(fakeRepository.sampleLocation, "2025-02-17")
        viewModel.clearError()
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun loadFromForecast_unknownDate_fallsBackToTodayDaily() = runTest {
        viewModel.loadFromForecast(fakeRepository.sampleForecast, "2099-12-31")

        val state = viewModel.uiState.value
        assertEquals("Stockholm", state.location?.name)
        assertEquals("2099-12-31", state.selectedDateIso)
        assertNotNull(state.dailyWeather)
        assertEquals("2025-02-17", state.dailyWeather?.dateIso)
    }
}
