package com.wheatherapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wheatherapp.data.network.NetworkMonitor
import com.wheatherapp.ui.navigation.WeatherNavGraph
import com.wheatherapp.ui.screens.home.WeatherViewModel
import com.wheatherapp.ui.theme.WeatherAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val appContainer = (application as WeatherApplication).container

        setContent {
            val weatherViewModel: WeatherViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return WeatherViewModel(
                            weatherRepository = appContainer.weatherRepository,
                            networkMonitor = NetworkMonitor(applicationContext)
                        ) as T
                    }
                }
            )

            val userSettings by weatherViewModel.userSettings.collectAsStateWithLifecycle()

            WeatherAppTheme(themeMode = userSettings.themeMode) {
                WeatherNavGraph(weatherViewModel = weatherViewModel)
            }
        }
    }
}
