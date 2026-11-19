package com.wheatherapp.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wheatherapp.data.network.NetworkMonitor
import com.wheatherapp.domain.model.LocationPlace
import com.wheatherapp.domain.model.UserSettings
import com.wheatherapp.domain.repository.WeatherRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

/**
 * ViewModel managing UI state for weather forecast operations.
 * Decoupled strictly from Navigation controller according to MVVM rules.
 */
class WeatherViewModel(
    private val weatherRepository: WeatherRepository,
    private val networkMonitor: NetworkMonitor
) : ViewModel() {

    private val _uiState = MutableStateFlow<WeatherUiState>(WeatherUiState.Loading)
    val uiState: StateFlow<WeatherUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<LocationPlace>>(emptyList())
    val searchResults: StateFlow<List<LocationPlace>> = _searchResults.asStateFlow()

    val favouriteLocations: StateFlow<List<LocationPlace>> = weatherRepository
        .getFavouriteLocations()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val userSettings: StateFlow<UserSettings> = weatherRepository
        .getUserSettings()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserSettings()
        )

    private val selectedLocation = MutableStateFlow<LocationPlace?>(null)
    private var searchJob: Job? = null
    private var forecastJob: Job? = null

    init {
        loadStartupData()
        observeNetworkConnectivity()
    }

    /**
     * Loads the latest cached forecast on startup so cached data is available immediately offline,
     * or fetches live forecast data for the default location.
     */
    private fun loadStartupData() {
        viewModelScope.launch {
            _uiState.value = WeatherUiState.Loading

            val latestCachedResult = weatherRepository.getLatestCachedForecast()
            if (latestCachedResult.isSuccess) {
                val cachedForecast = latestCachedResult.getOrNull()
                if (cachedForecast != null) {
                    selectedLocation.value = cachedForecast.location
                    if (networkMonitor.isNetworkAvailable()) {
                        loadForecastForLocation(cachedForecast.location)
                    } else {
                        _uiState.value = WeatherUiState.Offline(
                            cachedForecast = cachedForecast,
                            message = "Device is offline. Showing latest cached forecast."
                        )
                    }
                    return@launch
                }
            }

            val defaultLocation = LocationPlace(
                name = "Stockholm",
                country = "Sweden",
                latitude = 59.3293,
                longitude = 18.0686
            )
            selectedLocation.value = defaultLocation
            loadForecastForLocation(defaultLocation)
        }
    }

    /**
     * Collects live connectivity updates from [NetworkMonitor].
     */
    private fun observeNetworkConnectivity() {
        viewModelScope.launch {
            networkMonitor.observe().collect { isOnline ->
                val currentLoc = selectedLocation.value
                val currentState = _uiState.value

                if (isOnline) {
                    if (currentState is WeatherUiState.Offline || currentState is WeatherUiState.Error) {
                        currentLoc?.let { loadForecastForLocation(it) }
                    }
                } else {
                    if (currentState is WeatherUiState.Success) {
                        _uiState.value = WeatherUiState.Offline(
                            cachedForecast = currentState.forecast,
                            message = "Network connection lost. Showing cached forecast."
                        )
                    }
                }
            }
        }
    }

    /**
     * Action to handle changes in the location search bar input.
     * Searches by city name or raw "lat, lon" coordinates.
     */
    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
        searchJob?.cancel()

        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            _searchResults.value = emptyList()
            return
        }

        searchJob = viewModelScope.launch {
            val result = weatherRepository.searchLocations(trimmed)
            if (result.isSuccess) {
                _searchResults.value = result.getOrDefault(emptyList())
            } else {
                _searchResults.value = emptyList()
            }
        }
    }

    /**
     * Action when a location search query is submitted directly.
     */
    fun onSearchSubmitted(query: String) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return

        viewModelScope.launch {
            val result = weatherRepository.searchLocations(trimmed)
            val locations = result.getOrDefault(emptyList())
            if (locations.isNotEmpty()) {
                selectLocation(locations.first())
            }
        }
    }

    /**
     * Action to select a location from search results or favourites list.
     */
    fun selectLocation(location: LocationPlace) {
        selectedLocation.value = location
        _searchQuery.value = ""
        _searchResults.value = emptyList()
        loadForecastForLocation(location)
    }

    /**
     * Refreshes forecast for the currently selected location.
     */
    fun refreshForecast() {
        val location = selectedLocation.value
        if (location != null) {
            loadForecastForLocation(location)
        } else {
            loadStartupData()
        }
    }

    /**
     * Loads the forecast for a specific location and updates [uiState].
     * Distinguishes network failures (IOException) from HTTP server errors (HttpException).
     */
    private fun loadForecastForLocation(location: LocationPlace) {
        forecastJob?.cancel()
        forecastJob = viewModelScope.launch {
            _uiState.value = WeatherUiState.Loading

            val result = weatherRepository.getForecastForLocation(location)
            result.fold(
                onSuccess = { forecast ->
                    if (forecast.isFromCache) {
                        _uiState.value = WeatherUiState.Offline(
                            cachedForecast = forecast,
                            message = "Showing offline cached forecast for ${location.name}."
                        )
                    } else {
                        _uiState.value = WeatherUiState.Success(forecast)
                    }
                },
                onFailure = { throwable ->
                    handleForecastFailure(throwable, location)
                }
            )
        }
    }

    /**
     * Handles forecast failure by attempting to load cached data or creating a structured [WeatherUiState.Error].
     */
    private suspend fun handleForecastFailure(throwable: Throwable, location: LocationPlace) {
        val rootCause = getRootCause(throwable)

        val latestCached = weatherRepository.getLatestCachedForecast().getOrNull()

        if (latestCached != null && latestCached.location.latitude == location.latitude && latestCached.location.longitude == location.longitude) {
            _uiState.value = WeatherUiState.Offline(
                cachedForecast = latestCached,
                message = "Unable to connect to live network. Showing cached forecast for ${location.name}."
            )
            return
        }

        when (rootCause) {
            is IOException -> {
                _uiState.value = WeatherUiState.Error(
                    message = rootCause.message ?: "Network connection error. Check your internet connection.",
                    isNetworkError = true,
                    httpCode = null
                )
            }
            is HttpException -> {
                _uiState.value = WeatherUiState.Error(
                    message = "Server error (${rootCause.code()}): ${rootCause.message()}",
                    isNetworkError = false,
                    httpCode = rootCause.code()
                )
            }
            else -> {
                val message = throwable.message ?: "An unexpected error occurred while loading the forecast."
                val isNetwork = message.contains("network", ignoreCase = true) || message.contains("internet", ignoreCase = true)
                _uiState.value = WeatherUiState.Error(
                    message = message,
                    isNetworkError = isNetwork,
                    httpCode = null
                )
            }
        }
    }

    private fun getRootCause(throwable: Throwable): Throwable {
        var cause: Throwable = throwable
        while (cause.cause != null && cause.cause != cause) {
            if (cause is IOException || cause is HttpException) break
            cause = cause.cause!!
        }
        return cause
    }

    /**
     * Action to toggle favourite status for a location.
     */
    fun toggleFavourite(location: LocationPlace) {
        viewModelScope.launch {
            if (location.isFavourite) {
                weatherRepository.removeFavouriteLocation(location.latitude, location.longitude)
            } else {
                weatherRepository.addFavouriteLocation(location)
            }

            _searchResults.value = _searchResults.value.map { place ->
                if (place.latitude == location.latitude && place.longitude == location.longitude) {
                    place.copy(isFavourite = !location.isFavourite)
                } else {
                    place
                }
            }

            val currentState = _uiState.value
            if (currentState is WeatherUiState.Success && currentState.forecast.location.latitude == location.latitude && currentState.forecast.location.longitude == location.longitude) {
                val updatedLoc = currentState.forecast.location.copy(isFavourite = !location.isFavourite)
                _uiState.value = WeatherUiState.Success(currentState.forecast.copy(location = updatedLoc))
            }
        }
    }

    /**
     * Action to update user settings.
     */
    fun updateUserSettings(newSettings: UserSettings) {
        viewModelScope.launch {
            weatherRepository.updateUserSettings(newSettings)
        }
    }
}
