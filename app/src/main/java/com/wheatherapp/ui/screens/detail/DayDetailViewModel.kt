package com.wheatherapp.ui.screens.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wheatherapp.domain.model.LocationPlace
import com.wheatherapp.domain.model.WeatherForecast
import com.wheatherapp.domain.repository.WeatherRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel managing UI state and business logic for a single day detailed forecast.
 */
class DayDetailViewModel(
    private val weatherRepository: WeatherRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DayDetailUiState())
    val uiState: StateFlow<DayDetailUiState> = _uiState.asStateFlow()

    init {
        observeSettings()
    }

    private fun observeSettings() {
        viewModelScope.launch {
            weatherRepository.getUserSettings().collectLatest { settings ->
                _uiState.update { it.copy(userSettings = settings) }
            }
        }
    }

    fun loadFromForecast(forecast: WeatherForecast, dateIso: String) {
        val targetDaily = forecast.daily.find { it.dateIso == dateIso }
            ?: forecast.todayDaily
        val hourly = forecast.getHourlyForDate(dateIso)

        _uiState.update {
            it.copy(
                location = forecast.location,
                selectedDateIso = dateIso,
                dailyWeather = targetDaily,
                hourlyForecast = hourly,
                isLoading = false,
                errorMessage = null
            )
        }
    }

    fun loadDayDetail(location: LocationPlace, dateIso: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            weatherRepository.getForecastForLocation(location)
                .onSuccess { forecast ->
                    loadFromForecast(forecast, dateIso)
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.localizedMessage ?: "Failed to load detailed forecast"
                        )
                    }
                }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
