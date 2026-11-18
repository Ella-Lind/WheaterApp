package com.wheatherapp.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wheatherapp.domain.model.SpeedUnit
import com.wheatherapp.domain.model.TemperatureUnit
import com.wheatherapp.domain.model.ThemeMode
import com.wheatherapp.domain.model.UserSettings
import com.wheatherapp.domain.repository.WeatherRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel managing UI state and business logic for application settings.
 */
class SettingsViewModel(
    private val weatherRepository: WeatherRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        observeSettings()
    }

    private fun observeSettings() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            weatherRepository.getUserSettings().collectLatest { settings ->
                _uiState.update {
                    it.copy(
                        settings = settings,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun onTemperatureUnitChanged(unit: TemperatureUnit) {
        val updated = _uiState.value.settings.copy(temperatureUnit = unit)
        saveSettings(updated, "Temperature unit updated to ${unit.name}")
    }

    fun onSpeedUnitChanged(unit: SpeedUnit) {
        val updated = _uiState.value.settings.copy(speedUnit = unit)
        saveSettings(updated, "Speed unit updated to ${unit.name}")
    }

    fun onRefreshIntervalChanged(minutes: Int) {
        val updated = _uiState.value.settings.copy(refreshIntervalMinutes = minutes)
        saveSettings(updated, "Refresh interval set to $minutes minutes")
    }

    fun onThemeModeChanged(mode: ThemeMode) {
        val updated = _uiState.value.settings.copy(themeMode = mode)
        saveSettings(updated, "Theme set to ${mode.name}")
    }

    fun resetToDefaults() {
        saveSettings(UserSettings(), "Settings reset to default values")
    }

    fun clearUserMessage() {
        _uiState.update { it.copy(userMessage = null) }
    }

    private fun saveSettings(newSettings: UserSettings, successMessage: String) {
        viewModelScope.launch {
            weatherRepository.updateUserSettings(newSettings)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            settings = newSettings,
                            userMessage = successMessage
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(userMessage = error.localizedMessage ?: "Failed to save settings")
                    }
                }
        }
    }
}
