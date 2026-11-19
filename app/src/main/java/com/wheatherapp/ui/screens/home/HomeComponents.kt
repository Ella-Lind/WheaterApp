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
fun TopSearchBar(
    onOpenSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onOpenSearch,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search Icon",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Search city or coordinates",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun ForecastContent(
    forecast: WeatherForecast,
    userSettings: UserSettings,
    offlineBannerMessage: String?,
    onToggleFavourite: (LocationPlace) -> Unit,
    onDayClick: (Int) -> Unit,
    onRetry: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Offline Mode Banner if offline or cached
        if (forecast.isFromCache || offlineBannerMessage != null) {
            OfflineModeBanner(
                message = offlineBannerMessage ?: "Cached forecast data — Reconnect to update",
                onRetry = onRetry
            )
        }

        // Tier 1: Hero Weather Card
        HeroWeatherCard(
            forecast = forecast,
            userSettings = userSettings,
            onToggleFavourite = onToggleFavourite
        )

        // Tier 2: Hourly Today (LazyRow)
        val hourlyList = forecast.todayHourly.ifEmpty {
            forecast.daily.firstOrNull()?.hourlyForecast ?: emptyList()
        }
        if (hourlyList.isNotEmpty()) {
            HourlyTodayRow(
                hourlyList = hourlyList,
                userSettings = userSettings
            )
        }

        // Tier 3: 7-Day Forecast List
        if (forecast.daily.isNotEmpty()) {
            DailyForecastSection(
                dailyList = forecast.daily,
                userSettings = userSettings,
                onDayClick = onDayClick
            )
        }
    }
}

@Composable
fun OfflineModeBanner(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.CloudOff,
                    contentDescription = "Offline Icon",
                    tint = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Offline Mode",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(text = "Retry", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
fun HeroWeatherCard(
    forecast: WeatherForecast,
    userSettings: UserSettings,
    onToggleFavourite: (LocationPlace) -> Unit,
    modifier: Modifier = Modifier
) {
    val location = forecast.location
    val current = forecast.current

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Location Name, CACHED badge, & Favourite Star
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = location.name + (location.country?.let { ", $it" } ?: ""),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (forecast.isFromCache) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = "CACHED",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = WeatherFormatters.formatCoordinatesWithDirections(
                            location.latitude,
                            location.longitude
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = { onToggleFavourite(location) }
                ) {
                    Icon(
                        imageVector = if (location.isFavourite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                        contentDescription = "Toggle Favourite",
                        tint = if (location.isFavourite) Color(0xFFFFC107) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Oversized Temperature & Weather Icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = WeatherFormatters.formatTemperature(current.temperatureCelsius, userSettings),
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontSize = 60.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = current.weatherCondition.description,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                WeatherConditionIcon(
                    condition = current.weatherCondition,
                    modifier = Modifier.size(68.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 2 Metric Pills for Wind Speed & Humidity
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricPill(
                    icon = Icons.Default.Air,
                    label = "WIND SPEED",
                    value = WeatherFormatters.formatWindSpeed(current.windSpeedKmh, userSettings),
                    modifier = Modifier.weight(1f)
                )

                MetricPill(
                    icon = Icons.Default.WaterDrop,
                    label = "HUMIDITY",
                    value = "${current.relativeHumidityPercent}%",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun MetricPill(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun HourlyTodayRow(
    hourlyList: List<HourlyWeather>,
    userSettings: UserSettings,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "HOURLY TRAJECTORY",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Next 24 Hours",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            itemsIndexed(hourlyList.take(24)) { index, item ->
                val isCurrent = index == 0
                HourlyCard(
                    item = item,
                    isCurrent = isCurrent,
                    userSettings = userSettings
                )
            }
        }
    }
}

@Composable
fun HourlyCard(
    item: HourlyWeather,
    isCurrent: Boolean,
    userSettings: UserSettings
) {
    val backgroundColor = if (isCurrent) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surface
    }

    val contentColor = if (isCurrent) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    val subTextColor = if (isCurrent) {
        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = backgroundColor,
        contentColor = contentColor,
        tonalElevation = if (isCurrent) 6.dp else 2.dp,
        modifier = Modifier.width(72.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(vertical = 12.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (isCurrent) "NOW" else WeatherFormatters.formatHourTime(item.timeIso),
                style = MaterialTheme.typography.labelSmall,
                color = subTextColor
            )

            Spacer(modifier = Modifier.height(8.dp))

            WeatherConditionIcon(
                condition = item.weatherCondition,
                tint = contentColor,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = WeatherFormatters.formatTemperature(item.temperatureCelsius, userSettings),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = contentColor
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.WaterDrop,
                    contentDescription = "Precipitation",
                    tint = if (isCurrent) contentColor else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(10.dp)
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = "${item.precipitationProbabilityPercent}%",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = subTextColor
                )
            }
        }
    }
}

@Composable
fun DailyForecastSection(
    dailyList: List<DailyWeather>,
    userSettings: UserSettings,
    onDayClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = "7-DAY SYNOPTIC OUTLOOK",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
        )

        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp
        ) {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                val globalMin = dailyList.minOfOrNull { it.minTemperatureCelsius } ?: 0.0
                val globalMax = dailyList.maxOfOrNull { it.maxTemperatureCelsius } ?: 30.0

                dailyList.forEachIndexed { index, day ->
                    DailyForecastRowItem(
                        day = day,
                        dayIndex = index,
                        globalMinTemp = globalMin,
                        globalMaxTemp = globalMax,
                        userSettings = userSettings,
                        onClick = { onDayClick(index) }
                    )
                    if (index < dailyList.size - 1) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DailyForecastRowItem(
    day: DailyWeather,
    dayIndex: Int,
    globalMinTemp: Double,
    globalMaxTemp: Double,
    userSettings: UserSettings,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = WeatherFormatters.formatDayName(day.dateIso, dayIndex),
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.width(64.dp)
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.width(68.dp)
        ) {
            WeatherConditionIcon(
                condition = day.weatherCondition,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            if (day.maxPrecipitationProbabilityPercent > 0) {
                Text(
                    text = "${day.maxPrecipitationProbabilityPercent}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Text(
            text = WeatherFormatters.formatTemperature(day.minTemperatureCelsius, userSettings),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.End,
            modifier = Modifier.width(42.dp)
        )

        TemperatureRangeBar(
            minTemp = day.minTemperatureCelsius,
            maxTemp = day.maxTemperatureCelsius,
            globalMinTemp = globalMinTemp,
            globalMaxTemp = globalMaxTemp,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 10.dp)
                .height(6.dp)
        )

        Text(
            text = WeatherFormatters.formatTemperature(day.maxTemperatureCelsius, userSettings),
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
            modifier = Modifier.width(42.dp)
        )
    }
}

@Composable
fun TemperatureRangeBar(
    minTemp: Double,
    maxTemp: Double,
    globalMinTemp: Double,
    globalMaxTemp: Double,
    modifier: Modifier = Modifier
) {
    val totalSpan = (globalMaxTemp - globalMinTemp).coerceAtLeast(1.0)
    val startFraction = ((minTemp - globalMinTemp) / totalSpan).coerceIn(0.0, 1.0).toFloat()
    val endFraction = ((maxTemp - globalMinTemp) / totalSpan).coerceIn(0.0, 1.0).toFloat()

    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        val rangeFraction = (endFraction - startFraction).coerceAtLeast(0.08f)
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction = rangeFraction)
                .align(Alignment.CenterStart)
                .clip(CircleShape)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.secondary
                        )
                    )
                )
        )
    }
}

@Composable
fun WeatherConditionIcon(
    condition: WeatherCondition,
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified
) {
    val (icon, defaultTint) = when (condition) {
        WeatherCondition.CLEAR_SKY, WeatherCondition.MAINLY_CLEAR ->
            Icons.Filled.WbSunny to Color(0xFFFFC107)
        WeatherCondition.PARTLY_CLOUDY ->
            Icons.Filled.WbCloudy to Color(0xFF90A4AE)
        WeatherCondition.OVERCAST ->
            Icons.Filled.Cloud to Color(0xFF78909C)
        WeatherCondition.FOG ->
            Icons.Filled.Cloud to Color(0xFFB0BEC5)
        WeatherCondition.DRIZZLE, WeatherCondition.RAIN, WeatherCondition.RAIN_SHOWERS ->
            Icons.Filled.Grain to Color(0xFF38BDF8)
        WeatherCondition.SNOW, WeatherCondition.SNOW_SHOWERS ->
            Icons.Filled.AcUnit to Color(0xFFE0F2FE)
        WeatherCondition.THUNDERSTORM ->
            Icons.Filled.FlashOn to Color(0xFFFFB74D)
        WeatherCondition.UNKNOWN ->
            Icons.Filled.WbCloudy to Color(0xFF90A4AE)
    }

    Icon(
        imageVector = icon,
        contentDescription = condition.description,
        tint = if (tint != Color.Unspecified) tint else defaultTint,
        modifier = modifier
    )
}
