package com.wheatherapp.data.remote

import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Url

interface GeocodingApi {
    @GET
    suspend fun searchLocation(
        @Url url: String,
        @Query("name") name: String,
        @Query("count") count: Int = 10,
        @Query("language") language: String = "en",
        @Query("format") format: String = "json"
    ): String
}
