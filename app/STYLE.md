# STYLE.md — Architecture, Naming & UI Conventions

## 1. Package & Directory Structure

Organize code strictly by architectural responsibility under `com.wheatherapp`:

```text
app/src/main/java/com/wheatherapp/
├── MainActivity.kt                  # Single-activity entry point, sets up Theme & NavHost
├── WeatherApplication.kt            # Holds AppContainer (manual DI singleton graph)
├── di/
│   └── AppContainer.kt              # Single initialization point for Retrofit, Json, Room DB, Repos
├── data/
│   ├── remote/
│   │   ├── OpenMeteoApi.kt          # Retrofit interface for forecast endpoint (returns String)
│   │   ├── GeocodingApi.kt          # Retrofit interface for place-to-coordinate search (returns String)
│   │   └── dto/
│   │       ├── OpenMeteoDto.kt      # Raw @Serializable network models matching Open-Meteo JSON
│   │       └── GeocodingDto.kt      # Raw @Serializable geocoding response models
│   ├── parser/
│   │   ├── ForecastJsonParser.kt    # Uses kotlinx.serialization Json to decode String -> Domain models
│   │   └── GeocodingJsonParser.kt   # Uses kotlinx.serialization Json to decode String -> LocationPlace
│   ├── local/
│   │   ├── WeatherDatabase.kt       # Room database (CachedForecast, FavouriteLocation, UserSettings)
│   │   ├── WeatherTypeConverters.kt # Room @TypeConverter using kotlinx.serialization Json
│   │   ├── CachedForecastDao.kt     # Room DAO for offline forecast persistence
│   │   ├── FavouriteLocationDao.kt  # Room DAO for saved favourite places
│   │   ├── UserSettingsDao.kt       # Room DAO for single-row user preferences (id = 1)
│   │   └── entity/
│   │       ├── CachedForecastEntity.kt
│   │       ├── FavouriteLocationEntity.kt
│   │       └── UserSettingsEntity.kt
│   ├── network/
│   │   └── NetworkMonitor.kt        # ConnectivityManager implementation of domain NetworkMonitor
│   └── repository/
│       └── WeatherRepositoryImpl.kt # Coordinates Retrofit, parsers, Room caching, and offline fallback
├── domain/
│   ├── model/
│   │   ├── WeatherForecast.kt       # Clean domain aggregate (current, todayHourly, remainingDaily)
│   │   ├── HourlyWeather.kt         # Single hour forecast model
│   │   ├── DailyWeather.kt          # Single day forecast model
│   │   ├── WeatherCondition.kt      # Enum mapping WMO codes -> label, icon, semantic color
│   │   ├── LocationPlace.kt         # Clean location model (name, country, lat, lon, isFavourite)
│   │   └── UserSettings.kt          # Domain model for refresh interval, units, and theme
│   ├── network/
│   │   └── NetworkMonitor.kt        # Network connectivity interface consumed by WeatherViewModel
│   ├── repository/
│   │   └── WeatherRepository.kt     # Repository interface consumed by WeatherViewModel
│   └── util/
│       ├── CoordinateValidator.kt   # Pure Kotlin validator for lat/lon floating-point strings
│       └── WeatherFormatters.kt     # Pure Kotlin °C/°F, wind speed, date/time formatting helpers
└── ui/
    ├── navigation/
    │   └── WeatherNavGraph.kt       # Routes: Home, Detail/{dayIndex}, SavedLocations, Settings
    ├── theme/
    │   ├── Color.kt                 # Material 3 light/dark palette + weather condition accents
    │   ├── Type.kt                  # Clear typographic hierarchy (display temp, headers, body)
    │   └── Theme.kt                 # WeatherAppTheme composable
    ├── components/
    │   ├── SearchOverlay.kt         # City Search / Lat-Lon input overlay with validation feedback
    │   └── WeatherStateViews.kt     # Designed LoadingStateView, OfflineBanner, and Error states
    └── screens/
        ├── home/
        │   ├── HomeScreen.kt        # Main forecast orchestrator screen
        │   ├── HomeComponents.kt    # Extracted hero card, hourly row, and 7-day forecast cards
        │   ├── WeatherViewModel.kt  # Shared StateFlow ViewModel with forecastJob cancellation
        │   └── WeatherUiState.kt    # Sealed UI states (Loading, Success, Offline, Error)
        ├── detail/
        │   ├── DayDetailScreen.kt   # Single-day forecast screen with dayIndex bounds check
        │   └── DayDetailComponents.kt # BentoMetricGrid and HourlyMatrixSection composables
        ├── favourites/
        │   └── SavedLocationsScreen.kt # Quick-access list of saved Room locations
        └── settings/
            └── SettingsScreen.kt    # Refresh interval, unit toggles, and theme selection