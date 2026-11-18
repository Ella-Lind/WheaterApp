package com.wheatherapp.ui.screens.favourites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wheatherapp.domain.model.LocationPlace
import com.wheatherapp.domain.repository.WeatherRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel managing UI state and business logic for saved favourite locations.
 */
class FavouritesViewModel(
    private val weatherRepository: WeatherRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FavouritesUiState())
    val uiState: StateFlow<FavouritesUiState> = _uiState.asStateFlow()

    init {
        observeFavourites()
    }

    private fun observeFavourites() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            weatherRepository.getFavouriteLocations().collectLatest { favList ->
                _uiState.update { state ->
                    state.copy(
                        favouriteLocations = favList,
                        filteredLocations = filterLocations(favList, state.filterQuery),
                        isLoading = false
                    )
                }
            }
        }
    }

    fun onFilterQueryChanged(query: String) {
        _uiState.update { state ->
            state.copy(
                filterQuery = query,
                filteredLocations = filterLocations(state.favouriteLocations, query)
            )
        }
    }

    fun onAddFavourite(location: LocationPlace) {
        viewModelScope.launch {
            weatherRepository.addFavouriteLocation(location)
                .onSuccess {
                    _uiState.update {
                        it.copy(userMessage = "Added ${location.name} to favourites")
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(userMessage = error.localizedMessage ?: "Failed to add favourite")
                    }
                }
        }
    }

    fun onRemoveFavourite(location: LocationPlace) {
        viewModelScope.launch {
            weatherRepository.removeFavouriteLocation(location)
                .onSuccess {
                    _uiState.update {
                        it.copy(userMessage = "Removed ${location.name} from favourites")
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(userMessage = error.localizedMessage ?: "Failed to remove favourite")
                    }
                }
        }
    }

    fun clearUserMessage() {
        _uiState.update { it.copy(userMessage = null) }
    }

    private fun filterLocations(locations: List<LocationPlace>, query: String): List<LocationPlace> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return locations
        return locations.filter { place ->
            place.name.contains(trimmed, ignoreCase = true) ||
                    (place.country?.contains(trimmed, ignoreCase = true) == true)
        }
    }
}
