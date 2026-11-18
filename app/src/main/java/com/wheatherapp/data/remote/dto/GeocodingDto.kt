package com.wheatherapp.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GeocodingResponseDto(
    @SerialName("results") val results: List<GeocodingLocationDto>? = null
)

@Serializable
data class GeocodingLocationDto(
    @SerialName("id") val id: Long? = null,
    @SerialName("name") val name: String,
    @SerialName("latitude") val latitude: Double,
    @SerialName("longitude") val longitude: Double,
    @SerialName("country") val country: String? = null,
    @SerialName("country_code") val countryCode: String? = null,
    @SerialName("admin1") val admin1: String? = null
)
