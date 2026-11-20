package com.wheatherapp.ui.screens.favourites

import com.wheatherapp.data.repository.FakeWeatherRepository
import com.wheatherapp.domain.model.LocationPlace
import com.wheatherapp.util.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FavouritesViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var fakeRepository: FakeWeatherRepository
    private lateinit var viewModel: FavouritesViewModel

    @Before
    fun setUp() {
        fakeRepository = FakeWeatherRepository()
        viewModel = FavouritesViewModel(fakeRepository)
    }

    @Test
    fun addFavourite_updatesFavouriteLocations() = runTest {
        val place = LocationPlace("Oslo", "Norway", 59.9139, 10.7522)
        viewModel.onAddFavourite(place)

        val state = viewModel.uiState.value
        assertEquals(1, state.favouriteLocations.size)
        assertEquals("Oslo", state.favouriteLocations[0].name)
    }

    @Test
    fun removeFavourite_removesFromList() = runTest {
        val place = LocationPlace("Oslo", "Norway", 59.9139, 10.7522)
        viewModel.onAddFavourite(place)
        assertEquals(1, viewModel.uiState.value.favouriteLocations.size)

        viewModel.onRemoveFavourite(place)
        assertEquals(0, viewModel.uiState.value.favouriteLocations.size)
    }

    @Test
    fun filterQuery_filtersFavouriteList() = runTest {
        val place1 = LocationPlace("Oslo", "Norway", 59.9139, 10.7522)
        val place2 = LocationPlace("Paris", "France", 48.8566, 2.3522)
        viewModel.onAddFavourite(place1)
        viewModel.onAddFavourite(place2)

        viewModel.onFilterQueryChanged("Os")
        val state = viewModel.uiState.value
        assertEquals(1, state.filteredLocations.size)
        assertEquals("Oslo", state.filteredLocations[0].name)
    }

    @Test
    fun clearUserMessage_clearsMessage() = runTest {
        val place = LocationPlace("Oslo", "Norway", 59.9139, 10.7522)
        viewModel.onAddFavourite(place)
        viewModel.clearUserMessage()
        assertNull(viewModel.uiState.value.userMessage)
    }

    @Test
    fun onAddFavourite_failure_showsUserErrorMessage() = runTest {
        fakeRepository.shouldReturnError = true
        fakeRepository.errorMessage = "Database write failed"

        val place = LocationPlace("Oslo", "Norway", 59.9139, 10.7522)
        viewModel.onAddFavourite(place)

        val state = viewModel.uiState.value
        assertEquals("Database write failed", state.userMessage)
    }

    @Test
    fun onRemoveFavourite_failure_showsUserErrorMessage() = runTest {
        fakeRepository.shouldReturnError = true
        fakeRepository.errorMessage = "Database delete failed"

        val place = LocationPlace("Oslo", "Norway", 59.9139, 10.7522)
        viewModel.onRemoveFavourite(place)

        val state = viewModel.uiState.value
        assertEquals("Database delete failed", state.userMessage)
    }
}
