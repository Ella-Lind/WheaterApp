package com.wheatherapp.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class LocationPlace(
    val name: String,
    val country: String?,
    val latitude: Double,
    val longitude: Double,
    val isFavourite: Boolean = false
)
