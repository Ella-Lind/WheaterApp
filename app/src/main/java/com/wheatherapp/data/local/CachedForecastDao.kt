package com.wheatherapp.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.wheatherapp.data.local.entity.CachedForecastEntity

/**
 * Room DAO for accessing cached weather forecasts.
 */
@Dao
interface CachedForecastDao {
    @Query("SELECT * FROM cached_forecasts WHERE latitude = :latitude AND longitude = :longitude")
    suspend fun getCachedForecast(latitude: Double, longitude: Double): CachedForecastEntity?

    @Query(
        "SELECT * FROM cached_forecasts " +
            "WHERE locationName LIKE '%' || :query || '%' " +
            "OR country LIKE '%' || :query || '%' " +
            "ORDER BY cachedTimestampMillis DESC"
    )
    suspend fun searchCachedForecasts(query: String): List<CachedForecastEntity>

    @Query("SELECT * FROM cached_forecasts ORDER BY cachedTimestampMillis DESC LIMIT 1")
    suspend fun getLatestCachedForecast(): CachedForecastEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertForecast(forecast: CachedForecastEntity)

    @Query("DELETE FROM cached_forecasts WHERE cachedTimestampMillis < :maxAgeMillis")
    suspend fun deleteOldForecasts(maxAgeMillis: Long)
}
