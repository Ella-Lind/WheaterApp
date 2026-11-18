package com.wheatherapp

import android.app.Application
import com.wheatherapp.di.AppContainer
import com.wheatherapp.di.DefaultAppContainer

class WeatherApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer()
    }
}
