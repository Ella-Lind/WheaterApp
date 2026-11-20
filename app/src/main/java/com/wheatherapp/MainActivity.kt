package com.wheatherapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wheatherapp.ui.navigation.WeatherNavGraph
import com.wheatherapp.ui.screens.settings.SettingsViewModel
import com.wheatherapp.ui.theme.WeatherAppTheme
import com.wheatherapp.ui.util.ViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val appContainer = (application as WeatherApplication).container

        setContent {
            val settingsViewModel: SettingsViewModel = viewModel(
                factory = ViewModelFactory(appContainer)
            )

            val settingsUiState by settingsViewModel.uiState.collectAsStateWithLifecycle()

            WeatherAppTheme(themeMode = settingsUiState.settings.themeMode) {
                WeatherNavGraph()
            }
        }
    }
}
