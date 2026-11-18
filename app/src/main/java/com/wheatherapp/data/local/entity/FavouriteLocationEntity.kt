package com.wheatherapp.data.local.entity

import androidx.room.Entity

/**
 * Room entity representing a saved favourite location.
 */
@Entity(tableName = "favourite_locations", primaryKeys = ["latitude", "longitude"])
data class FavouriteLocationEntity(
    val latitude: Double,
    val longitude: Double,
    val name: String,
    val country: String?,
    val addedTimestampMillis: Long
)
