package com.wheatherapp.ui.screens.home

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
 * ViewModel managing UI state and business logic for the Home forecast screen.
 */
class HomeViewModel(
    private val weatherRepository: WeatherRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        observeSettings()
        observeFavourites()
        val initialLocation = _uiState.value.selectedLocation ?: HomeUiState.DEFAULT_LOCATION
        loadForecastForLocation(initialLocation)
    }

    private fun observeSettings() {
        viewModelScope.launch {
            weatherRepository.getUserSettings().collectLatest { settings ->
                _uiState.update { it.copy(userSettings = settings) }
            }
        }
    }

    private fun observeFavourites() {
        viewModelScope.launch {
            weatherRepository.getFavouriteLocations().collectLatest { favList ->
                val currentSelected = _uiState.value.selectedLocation
                val isFav = if (currentSelected != null) {
                    favList.any { fav ->
                        Math.abs(fav.latitude - currentSelected.latitude) < 0.0001 &&
                                Math.abs(fav.longitude - currentSelected.longitude) < 0.0001
                    }
                } else false

                _uiState.update { state ->
                    state.copy(
                        favouriteLocations = favList,
                        isFavourite = isFav,
                        selectedLocation = currentSelected?.copy(isFavourite = isFav)
                    )
                }
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        val trimmed = query.trim()
        if (trimmed.isBlank()) {
            _uiState.update { it.copy(searchResults = emptyList(), isSearching = false) }
        } else {
            onSearchSubmitted(trimmed)
        }
    }

    fun onSearchSubmitted(query: String) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSearching = true) }
            weatherRepository.searchLocations(trimmed)
                .onSuccess { results ->
                    _uiState.update {
                        it.copy(
                            searchResults = results,
                            isSearching = false
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isSearching = false,
                            userMessage = error.localizedMessage ?: "Failed to search location"
                        )
                    }
                }
        }
    }

    fun onSelectLocation(location: LocationPlace) {
        _uiState.update {
            it.copy(
                selectedLocation = location,
                searchQuery = "",
                searchResults = emptyList()
            )
        }
        loadForecastForLocation(location)
    }

    fun loadForecastForLocation(location: LocationPlace, isRefresh: Boolean = false) {
        viewModelScope.launch {
            if (isRefresh) {
                _uiState.update { it.copy(isRefreshing = true) }
            } else {
                _uiState.update { it.copy(forecastState = ForecastUiState.Loading) }
            }

            val isFav = weatherRepository.isLocationFavourite(location.latitude, location.longitude)

            weatherRepository.getForecastForLocation(location)
                .onSuccess { forecast ->
                    _uiState.update {
                        it.copy(
                            selectedLocation = forecast.location,
                            forecastState = ForecastUiState.Success(forecast),
                            isRefreshing = false,
                            isOffline = forecast.isFromCache,
                            isFavourite = isFav
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            forecastState = ForecastUiState.Error(
                                error.localizedMessage ?: "Failed to load weather forecast"
                            ),
                            isRefreshing = false
                        )
                    }
                }
        }
    }

    fun onRefresh() {
        val currentLoc = _uiState.value.selectedLocation ?: return
        loadForecastForLocation(currentLoc, isRefresh = true)
    }

    fun onToggleFavourite(targetLocation: LocationPlace? = null) {
        val loc = targetLocation ?: _uiState.value.selectedLocation ?: return
        val currentlyFav = if (targetLocation != null) {
            _uiState.value.favouriteLocations.any {
                Math.abs(it.latitude - loc.latitude) < 0.0001 &&
                        Math.abs(it.longitude - loc.longitude) < 0.0001
            }
        } else {
            _uiState.value.isFavourite
        }

        viewModelScope.launch {
            if (currentlyFav) {
                weatherRepository.removeFavouriteLocation(loc)
                    .onSuccess {
                        _uiState.update {
                            val newIsFav = if (it.selectedLocation?.latitude == loc.latitude &&
                                it.selectedLocation.longitude == loc.longitude) false else it.isFavourite
                            it.copy(
                                isFavourite = newIsFav,
                                userMessage = "Removed ${loc.name} from favourites"
                            )
                        }
                    }
                    .onFailure { error ->
                        _uiState.update {
                            it.copy(userMessage = error.localizedMessage ?: "Failed to remove favourite")
                        }
                    }
            } else {
                weatherRepository.addFavouriteLocation(loc)
                    .onSuccess {
                        _uiState.update {
                            val newIsFav = if (it.selectedLocation?.latitude == loc.latitude &&
                                it.selectedLocation.longitude == loc.longitude) true else it.isFavourite
                            it.copy(
                                isFavourite = newIsFav,
                                userMessage = "Added ${loc.name} to favourites"
                            )
                        }
                    }
                    .onFailure { error ->
                        _uiState.update {
                            it.copy(userMessage = error.localizedMessage ?: "Failed to add favourite")
                        }
                    }
            }
        }
    }

    fun clearSearch() {
        _uiState.update {
            it.copy(searchQuery = "", searchResults = emptyList(), isSearching = false)
        }
    }

    fun clearUserMessage() {
        _uiState.update { it.copy(userMessage = null) }
    }
}
