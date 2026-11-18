package com.wheatherapp.di

import android.content.Context
import com.wheatherapp.data.local.CachedForecastDao
import com.wheatherapp.data.local.FavouriteLocationDao
import com.wheatherapp.data.local.UserSettingsDao
import com.wheatherapp.data.local.WeatherDatabase
import com.wheatherapp.data.network.NetworkMonitor
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
    val database: WeatherDatabase
    val cachedForecastDao: CachedForecastDao
    val favouriteLocationDao: FavouriteLocationDao
    val userSettingsDao: UserSettingsDao
    val networkMonitor: NetworkMonitor
    val weatherRepository: WeatherRepository
}

/**
 * Single initialization point for Retrofit, OkHttpClient, APIs, Parsers, Room Database, DAOs, and Repositories.
 */
class DefaultAppContainer(private val context: Context) : AppContainer {

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            })
            .build()
    }

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

    override val geocodingApi: GeocodingApi by lazy {
        retrofit.create(GeocodingApi::class.java)
    }

    override val forecastJsonParser: ForecastJsonParser by lazy {
        ForecastJsonParser()
    }

    override val geocodingJsonParser: GeocodingJsonParser by lazy {
        GeocodingJsonParser()
    }

    override val database: WeatherDatabase by lazy {
        WeatherDatabase.getDatabase(context)
    }

    override val cachedForecastDao: CachedForecastDao by lazy {
        database.cachedForecastDao()
    }

    override val favouriteLocationDao: FavouriteLocationDao by lazy {
        database.favouriteLocationDao()
    }

    override val userSettingsDao: UserSettingsDao by lazy {
        database.userSettingsDao()
    }

    override val networkMonitor: NetworkMonitor by lazy {
        NetworkMonitor(context)
    }

    override val weatherRepository: WeatherRepository by lazy {
        WeatherRepositoryImpl(
            openMeteoApi = openMeteoApi,
            geocodingApi = geocodingApi,
            forecastJsonParser = forecastJsonParser,
            geocodingJsonParser = geocodingJsonParser,
            cachedForecastDao = cachedForecastDao,
            favouriteLocationDao = favouriteLocationDao,
            userSettingsDao = userSettingsDao,
            networkMonitor = networkMonitor
        )
    }
}
