package com.wheatherapp.ui.screens.detail

import com.wheatherapp.domain.model.DailyWeather
import com.wheatherapp.domain.model.HourlyWeather
import com.wheatherapp.domain.model.LocationPlace
import com.wheatherapp.domain.model.UserSettings

/**
 * Immutable UI State for the Day Detail Screen.
 */
data class DayDetailUiState(
    val location: LocationPlace? = null,
    val selectedDateIso: String? = null,
    val dailyWeather: DailyWeather? = null,
    val hourlyForecast: List<HourlyWeather> = emptyList(),
    val isLoading: Boolean = false,
    val userSettings: UserSettings = UserSettings(),
    val errorMessage: String? = null
)
