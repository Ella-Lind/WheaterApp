package com.wheatherapp.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.wheatherapp.data.local.entity.CachedForecastEntity
import com.wheatherapp.data.local.entity.FavouriteLocationEntity
import com.wheatherapp.data.local.entity.UserSettingsEntity

/**
 * Room database for WeatherApp holding cached forecasts, favourite locations, and user settings.
 */
@Database(
    entities = [
        CachedForecastEntity::class,
        FavouriteLocationEntity::class,
        UserSettingsEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(WeatherTypeConverters::class)
abstract class WeatherDatabase : RoomDatabase() {
    abstract fun cachedForecastDao(): CachedForecastDao
    abstract fun favouriteLocationDao(): FavouriteLocationDao
    abstract fun userSettingsDao(): UserSettingsDao

    companion object {
        @Volatile
        private var INSTANCE: WeatherDatabase? = null

        fun getDatabase(context: Context): WeatherDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    WeatherDatabase::class.java,
                    "weather_database.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
