package com.wheatherapp.data.parser

import com.wheatherapp.data.remote.dto.GeocodingResponseDto
import com.wheatherapp.domain.model.LocationPlace
import kotlinx.serialization.json.Json

/**
 * Dedicated parser for converting Geocoding search JSON String responses
 * into clean internal domain models (LocationPlace).
 */
class GeocodingJsonParser(
    private val json: Json = Json { ignoreUnknownKeys = true; isLenient = true }
) {
    /**
     * Parses the raw JSON string from Open-Meteo Geocoding API into a list of [LocationPlace].
     */
    fun parse(jsonString: String): List<LocationPlace> {
        val dto = json.decodeFromString<GeocodingResponseDto>(jsonString)
        val results = dto.results ?: return emptyList()

        return results.map { item ->
            LocationPlace(
                name = item.name,
                country = item.country,
                latitude = item.latitude,
                longitude = item.longitude,
                isFavourite = false
            )
        }
    }
}
