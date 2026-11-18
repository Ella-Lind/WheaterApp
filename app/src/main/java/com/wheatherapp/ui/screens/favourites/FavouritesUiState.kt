package com.wheatherapp.ui.screens.favourites

import com.wheatherapp.domain.model.LocationPlace

/**
 * Immutable UI State for the Favourites Screen.
 */
data class FavouritesUiState(
    val favouriteLocations: List<LocationPlace> = emptyList(),
    val filteredLocations: List<LocationPlace> = emptyList(),
    val filterQuery: String = "",
    val isLoading: Boolean = false,
    val userMessage: String? = null
)
