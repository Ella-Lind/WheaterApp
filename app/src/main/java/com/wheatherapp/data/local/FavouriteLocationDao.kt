package com.wheatherapp.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.wheatherapp.data.local.entity.FavouriteLocationEntity
import kotlinx.coroutines.flow.Flow

/**
 * Room DAO for accessing saved favourite locations.
 */
@Dao
interface FavouriteLocationDao {
    @Query("SELECT * FROM favourite_locations")
    fun getFavouriteLocations(): Flow<List<FavouriteLocationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavourite(favourite: FavouriteLocationEntity)

    @Query("DELETE FROM favourite_locations WHERE latitude = :latitude AND longitude = :longitude")
    suspend fun removeFavourite(latitude: Double, longitude: Double)

    @Query("SELECT COUNT(*) FROM favourite_locations WHERE latitude = :latitude AND longitude = :longitude")
    suspend fun isFavourite(latitude: Double, longitude: Double): Int
}
