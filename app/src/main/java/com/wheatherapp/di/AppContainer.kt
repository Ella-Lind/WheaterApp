package com.wheatherapp.di

import com.wheatherapp.data.parser.ForecastJsonParser
import com.wheatherapp.data.parser.GeocodingJsonParser
import com.wheatherapp.data.remote.GeocodingApi
import com.wheatherapp.data.remote.OpenMeteoApi
import com.wheatherapp.data.repository.WeatherRepositoryImpl
import com.wheatherapp.domain.repository.WeatherRepository
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.util.concurrent.TimeUnit

interface AppContainer {
    val retrofit: Retrofit
    val openMeteoApi: OpenMeteoApi
    val geocodingApi: GeocodingApi
    val forecastJsonParser: ForecastJsonParser
    val geocodingJsonParser: GeocodingJsonParser
    val weatherRepository: WeatherRepository
}

/**
 * Single initialization point for Retrofit, OkHttpClient, APIs, Parsers, and manual DI dependency graph.
 */
class DefaultAppContainer : AppContainer {

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            })
            .build()
    }

    /**
     * Singleton Retrofit instance for forecast API, configured with ScalarsConverterFactory
     * so Retrofit endpoints return raw JSON strings for our dedicated parser layer.
     */
    override val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.open-meteo.com/")
            .client(okHttpClient)
            .addConverterFactory(ScalarsConverterFactory.create())
            .build()
    }

    override val openMeteoApi: OpenMeteoApi by lazy {
        retrofit.create(OpenMeteoApi::class.java)
    }

    private val geocodingRetrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("https://geocoding-api.open-meteo.com/")
            .client(okHttpClient)
            .addConverterFactory(ScalarsConverterFactory.create())
            .build()
    }

    override val geocodingApi: GeocodingApi by lazy {
        geocodingRetrofit.create(GeocodingApi::class.java)
    }

    override val forecastJsonParser: ForecastJsonParser by lazy {
        ForecastJsonParser()
    }

    override val geocodingJsonParser: GeocodingJsonParser by lazy {
        GeocodingJsonParser()
    }

    override val weatherRepository: WeatherRepository by lazy {
        WeatherRepositoryImpl(
            openMeteoApi = openMeteoApi,
            geocodingApi = geocodingApi,
            forecastJsonParser = forecastJsonParser,
            geocodingJsonParser = geocodingJsonParser
        )
    }
}
