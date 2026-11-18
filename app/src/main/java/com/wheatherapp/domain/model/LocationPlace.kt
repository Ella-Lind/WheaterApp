package com.wheatherapp.domain.model

data class LocationPlace(
    val name: String,
    val country: String?,
    val latitude: Double,
    val longitude: Double,
    val isFavourite: Boolean = false
)