package com.wheatherapp.ui.screens.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Water
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wheatherapp.domain.model.DailyWeather
import com.wheatherapp.domain.model.HourlyWeather
import com.wheatherapp.domain.model.UserSettings
import com.wheatherapp.domain.util.WeatherFormatters
import com.wheatherapp.ui.components.LoadingStateView
import com.wheatherapp.ui.screens.home.WeatherConditionIcon
import com.wheatherapp.ui.screens.home.WeatherUiState
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun DayDetailScreen(
    dayIndex: Int,
    uiState: WeatherUiState,
    userSettings: UserSettings,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val forecast = when (uiState) {
        is WeatherUiState.Success -> uiState.forecast
        is WeatherUiState.Offline -> uiState.cachedForecast
        else -> null
    }

    val day = forecast?.daily?.getOrNull(dayIndex) ?: forecast?.daily?.firstOrNull()
    val locationName = forecast?.location?.let { it.name + (it.country?.let { c -> ", $c" } ?: "") } ?: "Forecast"

    Scaffold(
        topBar = {
            DayDetailTopAppBar(
                locationName = locationName,
                dateTitle = day?.let { WeatherFormatters.formatDayTitle(it.dateIso, dayIndex) } ?: "Day Forecast",
                onNavigateBack = onNavigateBack
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        if (day != null) {
            val scrollState = rememberScrollState()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(scrollState)
                    .padding(bottom = 16.dp)
            ) {
                // 1. Condition Hero Card
                DaySummaryHeroCard(
                    day = day,
                    userSettings = userSettings
                )

                // 2. 2x2 Bento Metric Grid
                BentoMetricGrid(
                    day = day,
                    userSettings = userSettings
                )

                // 3. Vertical "Hourly Matrix" List
                if (day.hourlyForecast.isNotEmpty()) {
                    HourlyMatrixSection(
                        hourlyForecast = day.hourlyForecast,
                        userSettings = userSettings
                    )
                }
            }
        } else {
            LoadingStateView(modifier = Modifier.padding(innerPadding))
        }
    }
}
