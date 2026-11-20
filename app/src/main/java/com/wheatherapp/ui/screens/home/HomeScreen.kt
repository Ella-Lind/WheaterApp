package com.wheatherapp.ui.screens.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.wheatherapp.domain.model.LocationPlace
import com.wheatherapp.ui.components.ErrorStateView
import com.wheatherapp.ui.components.LoadingStateView
import com.wheatherapp.ui.components.SearchOverlay

@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onSearchQueryChange: (String) -> Unit,
    onSearchSubmitted: (String) -> Unit,
    onLocationSelect: (LocationPlace) -> Unit,
    onDayClick: (Int) -> Unit,
    onToggleFavourite: (LocationPlace) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isSearchActive by rememberSaveable { mutableStateOf(false) }

    if (isSearchActive) {
        SearchOverlay(
            searchQuery = uiState.searchQuery,
            searchResults = uiState.searchResults,
            cachedLocations = uiState.favouriteLocations,
            onQueryChange = onSearchQueryChange,
            onLocationSelect = { location ->
                onLocationSelect(location)
                isSearchActive = false
            },
            onToggleFavourite = onToggleFavourite,
            onDismiss = {
                isSearchActive = false
                onSearchQueryChange("")
            }
        )
    } else {
        val scrollState = rememberScrollState()

        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(bottom = 16.dp)
        ) {
            // 1. Top Search Bar
            TopSearchBar(onOpenSearch = { isSearchActive = true })

            // Render UI depending on ForecastUiState
            when (val state = uiState.forecastState) {
                is ForecastUiState.Loading, ForecastUiState.Idle -> {
                    LoadingStateView(message = "Loading weather forecast...")
                }
                is ForecastUiState.Success -> {
                    ForecastContent(
                        forecast = state.forecast,
                        userSettings = uiState.userSettings,
                        offlineBannerMessage = if (uiState.isOffline) "Showing offline cached forecast." else null,
                        onToggleFavourite = onToggleFavourite,
                        onDayClick = onDayClick,
                        onRetry = onRetry
                    )
                }
                is ForecastUiState.Error -> {
                    ErrorStateView(
                        message = state.message,
                        isNetworkError = uiState.isOffline,
                        httpCode = null,
                        onRetry = onRetry,
                        onViewCached = onRetry
                    )
                }
            }
        }
    }
}
