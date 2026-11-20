package com.wheatherapp.ui.screens.home

import com.wheatherapp.data.repository.FakeWeatherRepository
import com.wheatherapp.domain.model.LocationPlace
import com.wheatherapp.util.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var fakeRepository: FakeWeatherRepository
    private lateinit var viewModel: HomeViewModel

    @Before
    fun setUp() {
        fakeRepository = FakeWeatherRepository()
        viewModel = HomeViewModel(fakeRepository)
    }

    @Test
    fun initialLoading_loadsForecastForDefaultLocation() = runTest {
        val state = viewModel.uiState.value
        assertTrue(state.forecastState is ForecastUiState.Success)
        val forecast = (state.forecastState as ForecastUiState.Success).forecast
        assertEquals("Stockholm", forecast.location.name)
    }

    @Test
    fun onSearchQueryChanged_updatesQueryInState() = runTest {
        viewModel.onSearchQueryChanged("Gothenburg")
        assertEquals("Gothenburg", viewModel.uiState.value.searchQuery)
    }

    @Test
    fun onSearchSubmitted_updatesSearchResults() = runTest {
        viewModel.onSearchSubmitted("Stockholm")
        val state = viewModel.uiState.value
        assertEquals(1, state.searchResults.size)
        assertEquals("Stockholm", state.searchResults[0].name)
    }

    @Test
    fun onSelectLocation_updatesSelectedLocationAndLoadsForecast() = runTest {
        val newLoc = LocationPlace("London", "UK", 51.5074, -0.1278)
        viewModel.onSelectLocation(newLoc)
        val state = viewModel.uiState.value
        assertEquals(newLoc, state.selectedLocation)
        assertTrue(state.forecastState is ForecastUiState.Success)
        val forecast = (state.forecastState as ForecastUiState.Success).forecast
        assertEquals("London", forecast.location.name)
    }

    @Test
    fun onToggleFavourite_addsAndRemovesFavourite() = runTest {
        assertFalse(viewModel.uiState.value.isFavourite)

        viewModel.onToggleFavourite()
        assertTrue(viewModel.uiState.value.isFavourite)

        viewModel.onToggleFavourite()
        assertFalse(viewModel.uiState.value.isFavourite)
    }

    @Test
    fun loadForecast_failure_showsErrorInForecastState() = runTest {
        fakeRepository.shouldReturnError = true
        fakeRepository.errorMessage = "Network timeout"

        viewModel.loadForecastForLocation(fakeRepository.sampleLocation)

        val state = viewModel.uiState.value
        assertTrue(state.forecastState is ForecastUiState.Error)
        assertEquals("Network timeout", (state.forecastState as ForecastUiState.Error).message)
    }

    @Test
    fun clearUserMessage_resetsUserMessage() = runTest {
        viewModel.onToggleFavourite()
        viewModel.clearUserMessage()
        assertNull(viewModel.uiState.value.userMessage)
    }

    @Test
    fun onSearchSubmitted_failure_setsUserMessageAndResetsSearching() = runTest {
        fakeRepository.shouldReturnError = true
        fakeRepository.errorMessage = "Location search server unreachable"

        viewModel.onSearchSubmitted("UnknownPlace")

        val state = viewModel.uiState.value
        assertFalse(state.isSearching)
        assertEquals("Location search server unreachable", state.userMessage)
    }

    @Test
    fun onToggleFavourite_failure_setsUserErrorMessage() = runTest {
        fakeRepository.shouldReturnError = true
        fakeRepository.errorMessage = "Failed to save favourite"

        viewModel.onToggleFavourite()

        val state = viewModel.uiState.value
        assertEquals("Failed to save favourite", state.userMessage)
    }
}
