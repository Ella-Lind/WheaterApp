package com.wheatherapp.ui.screens.settings

import com.wheatherapp.data.repository.FakeWeatherRepository
import com.wheatherapp.domain.model.SpeedUnit
import com.wheatherapp.domain.model.TemperatureUnit
import com.wheatherapp.domain.model.ThemeMode
import com.wheatherapp.util.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var fakeRepository: FakeWeatherRepository
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setUp() {
        fakeRepository = FakeWeatherRepository()
        viewModel = SettingsViewModel(fakeRepository)
    }

    @Test
    fun onTemperatureUnitChanged_updatesUnitInState() = runTest {
        viewModel.onTemperatureUnitChanged(TemperatureUnit.FAHRENHEIT)
        assertEquals(TemperatureUnit.FAHRENHEIT, viewModel.uiState.value.settings.temperatureUnit)
    }

    @Test
    fun onSpeedUnitChanged_updatesSpeedUnitInState() = runTest {
        viewModel.onSpeedUnitChanged(SpeedUnit.MS)
        assertEquals(SpeedUnit.MS, viewModel.uiState.value.settings.speedUnit)
    }

    @Test
    fun onRefreshIntervalChanged_updatesIntervalInState() = runTest {
        viewModel.onRefreshIntervalChanged(60)
        assertEquals(60, viewModel.uiState.value.settings.refreshIntervalMinutes)
    }

    @Test
    fun onThemeModeChanged_updatesThemeInState() = runTest {
        viewModel.onThemeModeChanged(ThemeMode.DARK)
        assertEquals(ThemeMode.DARK, viewModel.uiState.value.settings.themeMode)
    }

    @Test
    fun resetToDefaults_resetsSettings() = runTest {
        viewModel.onTemperatureUnitChanged(TemperatureUnit.FAHRENHEIT)
        viewModel.resetToDefaults()
        assertEquals(TemperatureUnit.CELSIUS, viewModel.uiState.value.settings.temperatureUnit)
    }

    @Test
    fun clearUserMessage_clearsMessage() = runTest {
        viewModel.onTemperatureUnitChanged(TemperatureUnit.FAHRENHEIT)
        viewModel.clearUserMessage()
        assertNull(viewModel.uiState.value.userMessage)
    }

    @Test
    fun saveSettings_failure_showsUserErrorMessage() = runTest {
        fakeRepository.shouldReturnError = true
        fakeRepository.errorMessage = "Failed to write user settings"

        viewModel.onTemperatureUnitChanged(TemperatureUnit.FAHRENHEIT)

        val state = viewModel.uiState.value
        assertEquals("Failed to write user settings", state.userMessage)
    }
}
