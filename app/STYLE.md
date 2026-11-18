---

### `STYLE.md`

```markdown
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
│   │   └── NetworkMonitor.kt        # Wraps ConnectivityManager into Flow<Boolean>
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
│   ├── repository/
│   │   └── WeatherRepository.kt     # Repository interface consumed by ViewModels
│   └── util/
│       ├── CoordinateValidator.kt   # Pure Kotlin validator for lat/lon floating-point strings
│       └── UnitConverter.kt         # Pure Kotlin °C/°F and m/s/km/h conversion helpers
└── ui/
    ├── navigation/
    │   └── WeatherNavGraph.kt       # Routes: Home, Detail/{dayIndex}, Favourites, Settings
    ├── theme/
    │   ├── Color.kt                 # Material 3 light/dark palette + weather condition accents
    │   ├── Type.kt                  # Clear typographic hierarchy (display temp, headers, body)
    │   └── Theme.kt                 # WeatherAppTheme composable
    ├── components/
    │   ├── SearchHeaderBar.kt       # Toggleable City Search / Lat-Lon Input with validation feedback
    │   ├── CurrentWeatherCard.kt    # Hero card with temperature, WMO icon, feels-like, wind
    │   ├── HourlyForecastRow.kt     # Horizontal scrollable row for Today's hourly forecast
    │   ├── DailyForecastItem.kt     # Row item for 7-day forecast list (clickable -> Detail)
    │   ├── OfflineBanner.kt         # Non-intrusive warning banner when showing cached Room data
    │   └── StatePlaceholders.kt     # Designed LoadingView, ErrorRetryView, EmptyStateView
    └── screens/
        ├── home/
        │   ├── HomeScreen.kt        # Main scrollable forecast + search screen
        │   ├── HomeViewModel.kt     # Manages HomeUiState via StateFlow
        │   └── HomeUiState.kt       # Immutable UI state + user events
        ├── detail/
        │   └── DayDetailScreen.kt   # Detailed single-day forecast view
        ├── favourites/
        │   ├── FavouritesScreen.kt  # Quick-access list of saved Room locations
        │   └── FavouritesViewModel.kt
        └── settings/
            ├── SettingsScreen.kt    # Refresh interval, unit toggles, theme selection
            └── SettingsViewModel.kt