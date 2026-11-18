package com.wheatherapp.ui.util

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.wheatherapp.di.AppContainer
import com.wheatherapp.ui.screens.detail.DayDetailViewModel
import com.wheatherapp.ui.screens.favourites.FavouritesViewModel
import com.wheatherapp.ui.screens.home.HomeViewModel
import com.wheatherapp.ui.screens.settings.SettingsViewModel

/**
 * Factory class providing instances of ViewModels configured with dependencies from [AppContainer].
 */
@Suppress("UNCHECKED_CAST")
class ViewModelFactory(
    private val appContainer: AppContainer
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(HomeViewModel::class.java) -> {
                HomeViewModel(appContainer.weatherRepository) as T
            }
            modelClass.isAssignableFrom(FavouritesViewModel::class.java) -> {
                FavouritesViewModel(appContainer.weatherRepository) as T
            }
            modelClass.isAssignableFrom(SettingsViewModel::class.java) -> {
                SettingsViewModel(appContainer.weatherRepository) as T
            }
            modelClass.isAssignableFrom(DayDetailViewModel::class.java) -> {
                DayDetailViewModel(appContainer.weatherRepository) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
