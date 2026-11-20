package com.wheatherapp.ui.screens.home

import com.wheatherapp.domain.model.WeatherForecast

/**
 * Sealed interface representing UI state for weather forecast screens.
 */
sealed interface WeatherUiState {
    data object Loading : WeatherUiState
    data class Success(val forecast: WeatherForecast) : WeatherUiState
    data class Offline(val cachedForecast: WeatherForecast?, val message: String) : WeatherUiState
    data class Error(
        val message: String,
        val isNetworkError: Boolean = false,
        val httpCode: Int? = null
    ) : WeatherUiState
}
