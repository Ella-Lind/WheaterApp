package com.wheatherapp.ui.screens.home

import com.wheatherapp.domain.model.LocationPlace
import com.wheatherapp.domain.model.UserSettings
import com.wheatherapp.domain.model.WeatherForecast

/**
 * Represents the status of fetching or displaying a forecast on the Home screen.
 */
sealed interface ForecastUiState {
    data object Idle : ForecastUiState
    data object Loading : ForecastUiState
    data class Success(val forecast: WeatherForecast) : ForecastUiState
    data class Error(val message: String) : ForecastUiState
}

/**
 * Immutable UI State for the Home Screen following Clean MVVM UDF.
 */
data class HomeUiState(
    val searchQuery: String = "",
    val isSearching: Boolean = false,
    val searchResults: List<LocationPlace> = emptyList(),
    val favouriteLocations: List<LocationPlace> = emptyList(),
    val selectedLocation: LocationPlace? = DEFAULT_LOCATION,
    val forecastState: ForecastUiState = ForecastUiState.Idle,
    val isRefreshing: Boolean = false,
    val isOffline: Boolean = false,
    val isFavourite: Boolean = false,
    val userSettings: UserSettings = UserSettings(),
    val userMessage: String? = null
) {
    companion object {
        val DEFAULT_LOCATION = LocationPlace(
            name = "Stockholm",
            country = "Sweden",
            latitude = 59.3293,
            longitude = 18.0686
        )
    }
}
