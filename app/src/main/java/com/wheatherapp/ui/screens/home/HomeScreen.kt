package com.wheatherapp.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbCloudy
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wheatherapp.domain.model.DailyWeather
import com.wheatherapp.domain.model.HourlyWeather
import com.wheatherapp.domain.model.LocationPlace
import com.wheatherapp.domain.model.UserSettings
import com.wheatherapp.domain.model.WeatherCondition
import com.wheatherapp.domain.model.WeatherForecast
import com.wheatherapp.domain.util.WeatherFormatters
import com.wheatherapp.ui.components.ErrorStateView
import com.wheatherapp.ui.components.LoadingStateView
import com.wheatherapp.ui.components.SearchOverlay
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun HomeScreen(
    uiState: WeatherUiState,
    userSettings: UserSettings,
    searchQuery: String,
    searchResults: List<LocationPlace>,
    favouriteLocations: List<LocationPlace>,
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
            searchQuery = searchQuery,
            searchResults = searchResults,
            cachedLocations = favouriteLocations,
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

            // Render UI depending on WeatherUiState
            when (uiState) {
                is WeatherUiState.Loading -> {
                    LoadingStateView(message = "Loading weather forecast...")
                }
                is WeatherUiState.Success -> {
                    ForecastContent(
                        forecast = uiState.forecast,
                        userSettings = userSettings,
                        offlineBannerMessage = null,
                        onToggleFavourite = onToggleFavourite,
                        onDayClick = onDayClick,
                        onRetry = onRetry
                    )
                }
                is WeatherUiState.Offline -> {
                    val cached = uiState.cachedForecast
                    if (cached != null) {
                        ForecastContent(
                            forecast = cached.copy(isFromCache = true),
                            userSettings = userSettings,
                            offlineBannerMessage = uiState.message,
                            onToggleFavourite = onToggleFavourite,
                            onDayClick = onDayClick,
                            onRetry = onRetry
                        )
                    } else {
                        ErrorStateView(
                            message = uiState.message,
                            isNetworkError = true,
                            httpCode = null,
                            onRetry = onRetry
                        )
                    }
                }
                is WeatherUiState.Error -> {
                    ErrorStateView(
                        message = uiState.message,
                        isNetworkError = uiState.isNetworkError,
                        httpCode = uiState.httpCode,
                        onRetry = onRetry,
                        onViewCached = {
                            onRetry()
                        }
                    )
                }
            }
        }
    }
}

