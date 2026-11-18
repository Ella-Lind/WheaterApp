package com.wheatherapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.wheatherapp.domain.model.SpeedUnit
import com.wheatherapp.domain.model.TemperatureUnit
import com.wheatherapp.domain.model.ThemeMode

/**
 * Room entity representing single-row user preferences (id = 1).
 */
@Entity(tableName = "user_settings")
data class UserSettingsEntity(
    @PrimaryKey
    val id: Int = 1,
    val temperatureUnit: TemperatureUnit = TemperatureUnit.CELSIUS,
    val speedUnit: SpeedUnit = SpeedUnit.KMH,
    val refreshIntervalMinutes: Int = 30,
    val themeMode: ThemeMode = ThemeMode.SYSTEM
)
