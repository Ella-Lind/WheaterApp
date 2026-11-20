package com.wheatherapp.ui.screens.settings

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
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.wheatherapp.domain.model.SpeedUnit
import com.wheatherapp.domain.model.TemperatureUnit
import com.wheatherapp.domain.model.ThemeMode

@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onTemperatureUnitChanged: (TemperatureUnit) -> Unit,
    onSpeedUnitChanged: (SpeedUnit) -> Unit,
    onRefreshIntervalChanged: (Int) -> Unit,
    onThemeModeChanged: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val userSettings = uiState.settings

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Screen Header
            Text(
                text = "Settings & Preferences",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Manage telemetry units, appearance, and sensor sync frequency.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Card 1: Measurement Units
            SettingsCard(
                title = "Measurement Units",
                icon = Icons.Default.Thermostat
            ) {
                Text(
                    text = "Temperature",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                SegmentedPillSelector(
                    options = listOf(
                        TemperatureUnit.CELSIUS to "°C",
                        TemperatureUnit.FAHRENHEIT to "°F"
                    ),
                    selectedOption = userSettings.temperatureUnit,
                    onOptionSelected = onTemperatureUnitChanged
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Wind Velocity",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                SegmentedPillSelector(
                    options = listOf(
                        SpeedUnit.KMH to "km/h",
                        SpeedUnit.MS to "m/s",
                        SpeedUnit.MPH to "mph"
                    ),
                    selectedOption = userSettings.speedUnit,
                    onOptionSelected = onSpeedUnitChanged
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Card 2: Appearance & Atmosphere
            SettingsCard(
                title = "Appearance & Atmosphere",
                icon = Icons.Default.Palette
            ) {
                Text(
                    text = "Interface Tone",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                SegmentedPillSelector(
                    options = listOf(
                        ThemeMode.SYSTEM to "System",
                        ThemeMode.LIGHT to "Light",
                        ThemeMode.DARK to "Dark"
                    ),
                    icons = listOf(
                        Icons.Default.Computer,
                        Icons.Default.WbSunny,
                        Icons.Default.NightsStay
                    ),
                    selectedOption = userSettings.themeMode,
                    onOptionSelected = onThemeModeChanged
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Card 3: Data & Telemetry Sync
            SettingsCard(
                title = "Data & Telemetry Sync",
                icon = Icons.Default.Sync
            ) {
                Text(
                    text = "Auto-Refresh Interval",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                SegmentedPillSelector(
                    options = listOf(
                        15 to "15 min",
                        30 to "30 min",
                        60 to "1 hr"
                    ),
                    selectedOption = userSettings.refreshIntervalMinutes,
                    onOptionSelected = onRefreshIntervalChanged
                )
            }
        }
    }
}

@Composable
fun SettingsCard(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 14.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            content()
        }
    }
}

@Composable
fun <T> SegmentedPillSelector(
    options: List<Pair<T, String>>,
    selectedOption: T,
    onOptionSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    icons: List<ImageVector>? = null
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            options.forEachIndexed { index, (option, label) ->
                val isSelected = option == selectedOption
                val icon = icons?.getOrNull(index)

                Surface(
                    onClick = { onOptionSelected(option) },
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                    contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp, horizontal = 4.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (icon != null) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            ),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
