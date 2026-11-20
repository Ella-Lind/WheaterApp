package com.wheatherapp.ui.screens.detail

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.wheatherapp.domain.util.WeatherFormatters
import com.wheatherapp.ui.components.LoadingStateView

@Composable
fun DayDetailScreen(
    dayIndex: Int,
    uiState: DayDetailUiState,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val day = uiState.dailyWeather
    val locationName = uiState.location?.let { it.name + (it.country?.let { c -> ", $c" } ?: "") } ?: "Forecast"

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
                    userSettings = uiState.userSettings
                )

                // 2. 2x2 Bento Metric Grid
                BentoMetricGrid(
                    day = day,
                    userSettings = uiState.userSettings
                )

                // 3. Vertical "Hourly Matrix" List
                if (uiState.hourlyForecast.isNotEmpty()) {
                    HourlyMatrixSection(
                        hourlyForecast = uiState.hourlyForecast,
                        userSettings = uiState.userSettings
                    )
                }
            }
        } else {
            LoadingStateView(modifier = Modifier.padding(innerPadding))
        }
    }
}
