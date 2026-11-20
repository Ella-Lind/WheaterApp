# WeatherApp

A native Android weather application written in **Kotlin** with **Jetpack Compose**. It displays current weather, an hourly forecast for today, and a 7-day forecast for any location globally. It queries live public [Open-Meteo](https://open-meteo.com/) APIs (no API key required) and provides full offline functionality by serving cached forecasts from a local Room database.

Built for the *Native Mobile App Development* course (Pair Programming Lab B:2).

| Key | Details                                                                                                                              |
|---|--------------------------------------------------------------------------------------------------------------------------------------|
| Package / Application ID | `com.wheatherapp`                                                                                                                    |
| Language & Frameworks | Kotlin 2.0.21, Jetpack Compose (Material 3)                                                                                          |
| Min / Target SDK | 26 / 37                                                                                                                              |
| Networking | Retrofit + OkHttp (returning raw JSON strings)                                                                                       |
| JSON Parsing | `kotlinx.serialization`                                                                                                              |
| Local Persistence | Room (via KSP)                                                                                                                       |
| Dependency Injection | Manual Dependency Injection Container (`AppContainer`)                                                                               |
| Documentation | `app/SPEC.md`, `app/STYLE.md`, `app/CONTEXT.md`, `app/docs/TEST_REPORT.md`, `app/docs/LLM_PROCESS.md`, `app/docs/REFLECTION.md` |

---

## 1. System Architecture & Unidirectional Data Flow

The application follows **Clean MVVM with Unidirectional Data Flow (UDF)**. UI screens observe immutable state produced by `WeatherViewModel`, while user interactions emit events back to the ViewModel:

```text
┌────────────────────────────────────────────────────────────────────────────┐
│ UI Layer (Jetpack Compose)                                                 │
│   HomeScreen, DayDetailScreen, SavedLocationsScreen, SettingsScreen        │
│   WeatherNavGraph & NavigationBar (Scaffold bottom bar)                    │
│                        │ events (lambdas)            ▲ StateFlow           │
│                        ▼                             │                     │
│               WeatherViewModel ──────► WeatherUiState (Sealed Interface)  │
└────────────────────────────┬───────────────────────────────────────────────┘
                             │ depends on interface
┌────────────────────────────▼───────────────────────────────────────────────┐
│ Domain Layer (Pure Kotlin)                                                 │
│   WeatherRepository (interface), NetworkMonitor (interface)                │
│   Models: WeatherForecast, LocationPlace, UserSettings, WeatherCondition   │
│   Utils:  CoordinateValidator, WeatherFormatters                           │
└────────────────────────────▲───────────────────────────────────────────────┘
                             │ implemented by
┌────────────────────────────┴───────────────────────────────────────────────┐
│ Data Layer (Repository & Persistence)                                      │
│   WeatherRepositoryImpl                                                    │
│     ├── Remote: OpenMeteoApi, GeocodingApi (Raw JSON String via Retrofit)   │
│     ├── Parser: ForecastJsonParser, GeocodingJsonParser (kotlinx.ser.)     │
│     ├── Local:  WeatherDatabase (Room), 3 DAOs & Entities                  │
│     └── Network: NetworkMonitor (Android ConnectivityManager)              │
└────────────────────────────────────────────────────────────────────────────┘
         di/AppContainer instantiates and wires the singleton graph.
```

### Architectural Principles & Layer Rules
- **Decoupled Composables**: `@Composable` functions do not access Retrofit APIs, JSON parsers, Room DAOs, or `NavController`. They receive state and trigger event lambdas.
- **Single ViewModel**: `WeatherViewModel` manages state across the entire navigation graph. Navigation is owned strictly by the UI layer (`WeatherNavGraph.kt`).
- **Raw JSON Network Boundary**: Retrofit endpoints return raw `String` payloads. All JSON decoding occurs within dedicated parser classes (`ForecastJsonParser`, `GeocodingJsonParser`).
- **Pure Domain Layer**: The `domain` package contains pure Kotlin interfaces and data models without dependencies on Android UI, Retrofit, or Room.

---

## 2. Where to Start Reading

1. `WeatherApplication.kt` – Initializes the manual dependency graph.
2. `di/AppContainer.kt` – Configures singleton instances for OkHttpClient, Retrofit, APIs, Parsers, Room DB/DAOs, NetworkMonitor, and Repository.
3. `domain/repository/WeatherRepository.kt` – Contract defining forecast queries, geocoding search, favourites, and settings.
4. `data/repository/WeatherRepositoryImpl.kt` – Implements network calls, JSON parsing, Room offline caching, and fallback handling.
5. `ui/screens/home/WeatherViewModel.kt` – Manages `WeatherUiState`, location search, favourites, settings, and network transitions.

---

## 3. Active Repository Files & Responsibilities

### 3.1 Application Entry & Dependency Injection

| File | Location | Responsibility |
|---|---|---|
| `WeatherApplication.kt` | `app/src/main/java/com/wheatherapp/` | `Application` subclass that instantiates `DefaultAppContainer`. |
| `MainActivity.kt` | `app/src/main/java/com/wheatherapp/` | Single activity entry point. Obtains `AppContainer`, creates `WeatherViewModel`, collects `userSettings` to drive `WeatherAppTheme`, and hosts `WeatherNavGraph`. |
| `AppContainer.kt` | `app/src/main/java/com/wheatherapp/di/` | Dependency container holding lazy singletons for OkHttpClient, Retrofit, OpenMeteoApi, GeocodingApi, ForecastJsonParser, GeocodingJsonParser, WeatherDatabase, DAOs, NetworkMonitor, and WeatherRepositoryImpl. |

### 3.2 Domain Layer (`domain/`)

| File | Location | Responsibility |
|---|---|---|
| `WeatherRepository.kt` | `domain/repository/` | Domain contract interface defining search, forecast retrieval, cached forecast access, favourite places, and user settings. |
| `NetworkMonitor.kt` | `domain/network/` | Connectivity interface providing a `Flow<Boolean>` for network status. |
| `WeatherForecast.kt` | `domain/model/` | Aggregate model containing `location`, `current`, `todayHourly`, `daily` (7 days), timestamp, and `isFromCache` flag. |
| `CurrentWeather.kt`, `HourlyWeather.kt`, `DailyWeather.kt` | `domain/model/` | Domain models representing current weather metrics, hourly forecast items, and daily summaries (with hourly breakdown). |
| `WeatherCondition.kt` | `domain/model/` | Enum mapping Open-Meteo WMO weather codes to descriptive labels and icons. |
| `LocationPlace.kt` | `domain/model/` | Clean location model storing place name, country, latitude, longitude, and favourite status. |
| `UserSettings.kt` | `domain/model/` | Domain model storing `TemperatureUnit`, `SpeedUnit`, `ThemeMode`, and `refreshIntervalMinutes`. |
| `CoordinateValidator.kt` | `domain/util/` | Validates latitude/longitude floating-point bounds and parses `"lat, lon"` string inputs. |
| `WeatherFormatters.kt` | `domain/util/` | Pure formatting functions for temperatures (`°C`/`°F`), wind speeds (`km/h`/`m/s`/`mph`), compass directions, coordinates, and dates/times. |
| `UnitConverter.kt` | `domain/util/` | Metric-to-imperial conversion utility functions. |

### 3.3 Data Layer (`data/`)

| File | Location | Responsibility |
|---|---|---|
| `OpenMeteoApi.kt` | `data/remote/` | Retrofit interface querying `/v1/forecast` for current, hourly, and daily metrics. Returns raw `String`. |
| `GeocodingApi.kt` | `data/remote/` | Retrofit interface querying place search endpoint. Returns raw `String`. |
| `OpenMeteoDto.kt`, `GeocodingDto.kt` | `data/remote/dto/` | `@Serializable` DTO classes matching API JSON structures. |
| `ForecastJsonParser.kt` | `data/parser/` | Decodes raw JSON strings into `WeatherForecast` domain objects, grouping hourly entries by date. |
| `GeocodingJsonParser.kt` | `data/parser/` | Decodes raw search JSON strings into `List<LocationPlace>`. |
| `WeatherDatabase.kt` | `data/local/` | Room database (`weather_database.db`, version 1) managing 3 tables. |
| `CachedForecastEntity.kt`, `FavouriteLocationEntity.kt`, `UserSettingsEntity.kt` | `data/local/entity/` | Room entities for cached forecasts, saved favourite places, and single-row user settings. |
| `CachedForecastDao.kt`, `FavouriteLocationDao.kt`, `UserSettingsDao.kt` | `data/local/` | Room DAOs providing SQL operations for offline forecast storage, favourites flow, and user settings. |
| `WeatherTypeConverters.kt` | `data/local/` | Room `@TypeConverter` storing `WeatherForecast` domain aggregates as JSON strings via `kotlinx.serialization`. |
| `NetworkMonitor.kt` | `data/network/` | Android `ConnectivityManager` implementation emitting real-time online/offline status. |
| `WeatherRepositoryImpl.kt` | `data/repository/` | Coordinates Retrofit networking, JSON parsers, Room persistence, and offline fallback on `Dispatchers.IO`. |

### 3.4 UI Layer (`ui/`)

| File | Location | Responsibility |
|---|---|---|
| `WeatherNavGraph.kt` | `ui/navigation/` | Configures `NavHost` and 3-tab `NavigationBar` (`weather`, `saved`, `settings`, and `detail/{dayIndex}`). |
| `WeatherViewModel.kt` | `ui/screens/home/` | Manages `WeatherUiState`, active `forecastJob` cancellation, location searches, favourites, and settings. |
| `WeatherUiState.kt` | `ui/screens/home/` | Sealed interface representing `Loading`, `Success`, `Offline`, and `Error` states. |
| `HomeScreen.kt` & `HomeComponents.kt` | `ui/screens/home/` | Main 3-tier view: Top Search Bar, Hero Weather Card with Bento pills, Hourly Trajectory `LazyRow`, and 7-Day Forecast list. |
| `DayDetailScreen.kt` & `DayDetailComponents.kt` | `ui/screens/detail/` | Detailed single-day forecast screen with 2x2 Bento Metric Grid and 24-hour matrix. |
| `SavedLocationsScreen.kt` | `ui/screens/favourites/` | Filterable list of saved favourite stations with location selection and unstar actions. |
| `SettingsScreen.kt` | `ui/screens/settings/` | Segmented pill selectors for temperature units, wind velocity units, theme mode, and refresh interval. |
| `SearchOverlay.kt` | `ui/components/` | Active location search input overlay displaying matching locations and cached offline cities. |
| `WeatherStateViews.kt` | `ui/components/` | `LoadingStateView` and `ErrorStateView` with network/server status diagnostic rows and retry buttons. |
| `Color.kt`, `Type.kt`, `Theme.kt` | `ui/theme/` | Material 3 dark-slate palette, typography, and `WeatherAppTheme`. |

---

## 4. Key Workflows & Layer Connections

### App Startup & Caching Workflow
1. `WeatherViewModel` loads `weatherRepository.getLatestCachedForecast()`.
2. If cached data exists, it is loaded immediately with `WeatherUiState.Offline` when offline, or updated with live data when online.
3. If no cache exists, the app defaults to loading forecast data for Stockholm.

### Live Forecast Query Workflow
`HomeScreen` → `WeatherViewModel.selectLocation()` → `WeatherRepository.getForecastForLocation()`:
1. `NetworkMonitor` checks connectivity. If offline, the repository retrieves cached Room data (`isFromCache = true`).
2. If online, `OpenMeteoApi` fetches the raw JSON string -> `ForecastJsonParser` converts it to `WeatherForecast` -> Room stores the updated entity in `cached_forecasts`.
3. `WeatherViewModel` emits `WeatherUiState.Success` (or `WeatherUiState.Offline`), and Compose screens re-render.

### Location Search Workflow
`onSearchQueryChange(query)` cancels any active `forecastJob` to prevent race conditions. If the query is a valid `"lat, lon"` coordinate string, `CoordinateValidator` handles it directly without network calls. Otherwise, `GeocodingApi` searches for matching places. When offline, place searches query cached Room entries.

### Network Transition Workflow
`NetworkMonitor.observe()` streams real-time connection status. Transitioning offline while viewing a live forecast seamlessly switches state to `WeatherUiState.Offline` preserving cached data. Reconnecting automatically triggers a live refresh.

---

## 5. Documentation & Project Specification Files

- `app/SPEC.md`: Master technical specification containing AI meta-instructions, MVVM system architecture, requirements traceability checklist (`W-01` through `W-18`), component specs, and oral exam map (Q1–Q30).
- `app/STYLE.md`: Code style guidelines, directory structure, and naming conventions.
- `app/CONTEXT.md`: Course lab requirements, API documentation, and locked library policy.
- `app/docs/TEST_REPORT.md`: Comprehensive test report documenting unit tests, mutation testing, and 20 verified Emulator UX scenarios.
- `app/docs/LLM_PROCESS.md`: Interaction log documenting AI-assisted development prompts and architectural decisions.
- `app/docs/REFLECTION.md`: Student team reflection on development workflow, AI code generation, and testing insights.

---

## 6. Unit Testing Suite

Unit tests mirror the main codebase package structure in `app/src/test/java/com/wheatherapp/`:

| Test Class | Location | Coverage |
|---|---|---|
| `ForecastJsonParserTest.kt` | `data/parser/` | Verifies JSON parsing, WMO code mapping, and hourly entry grouping. |
| `GeocodingJsonParserTest.kt` | `data/parser/` | Verifies place search JSON parsing and empty result handling. |
| `WeatherRepositoryTest.kt` | `data/repository/` | Tests repository caching, coordinate parsing, and network fallback using fake DAOs. |
| `FakeWeatherRepository.kt` | `data/repository/` | In-memory test fake reused across ViewModel test suites. |
| `CoordinateValidatorTest.kt` | `domain/util/` | Validates latitude/longitude bounds and `"lat, lon"` string parsing. |
| `WeatherFormattersTest.kt` | `domain/util/` | Tests unit formatting for temperature, wind speed, wind directions, and coordinates. |
| `UnitConverterTest.kt` | `domain/util/` | Tests raw metric-to-imperial calculation helpers. |
| `WeatherViewModelTest.kt` | `ui/screens/home/` | Tests `WeatherViewModel` startup caching, state transitions, `IOException` vs `HttpException` error handling, search, and favourites logic. |
| `MainDispatcherRule.kt` | `util/` | JUnit test rule overriding `Dispatchers.Main` with `StandardTestDispatcher` for coroutine tests. |

All tests execute on the JVM without requiring an emulator:

```bash
./gradlew testDebugUnitTest
```

---

## 7. Running & Building the Application

1. Open the project in **Android Studio** (configured with JDK 17).
2. Select an Android Emulator or physical device (API 26+).
3. Execute the `app` run configuration.
4. Ensure internet access for live API queries (permissions `INTERNET` and `ACCESS_NETWORK_STATE` are declared in `AndroidManifest.xml`).
