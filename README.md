# WheaterApp

A native Android weather app written in **Kotlin** with **Jetpack Compose**. It shows the current weather, an hourly forecast for today and a 7-day forecast for any place in the world. It uses the free [Open-Meteo](https://open-meteo.com/) APIs (no API key), and keeps working offline by falling back to forecasts cached in a local database.

Built for the *Native Mobile App Development* course (Pair Programming Lab B:2).

| | |
|---|---|
| Package / application id | `com.wheatherapp` (the "wheather" spelling is used consistently in code) |
| Language / UI | Kotlin 2.0.21, Jetpack Compose (Material 3) |
| Min / target SDK | 26 / 37 |
| Networking | Retrofit + OkHttp, returning **raw JSON strings** |
| JSON | `kotlinx.serialization` |
| Persistence | Room (via KSP) |
| Dependency injection | Hand-written container (`AppContainer`), no framework |

---

## 1. Architecture at a glance

The app is a single-activity **MVVM** app with **unidirectional data flow**: state flows down from the ViewModel to the UI, and events flow up from the UI to the ViewModel. The code is split into four layers, each in its own package under `app/src/main/java/com/wheatherapp/`.

```
┌──────────────────────────────────────────────────────────────────────┐
│ ui/            Compose screens (stateless)  ◄── state ── ViewModel   │
│                WeatherNavGraph, theme, shared components             │
│                         │ events (lambdas)        ▲ StateFlow        │
│                         ▼                         │                  │
│                WeatherViewModel  (WeatherUiState, search, favourites)│
└────────────────────────────┬─────────────────────────────────────────┘
                             │ depends only on the interface
┌────────────────────────────▼─────────────────────────────────────────┐
│ domain/        WeatherRepository (interface)                         │
│                Models: WeatherForecast, LocationPlace, UserSettings… │
│                Utils:  CoordinateValidator, WeatherFormatters        │
└────────────────────────────▲─────────────────────────────────────────┘
                             │ implemented by
┌────────────────────────────┴─────────────────────────────────────────┐
│ data/          WeatherRepositoryImpl                                 │
│   remote/   OpenMeteoApi, GeocodingApi  ──► raw JSON String          │
│   parser/   ForecastJsonParser, GeocodingJsonParser ──► domain models│
│   local/    WeatherDatabase + 3 DAOs + entities (Room)               │
│   network/  NetworkMonitor (online/offline)                          │
└──────────────────────────────────────────────────────────────────────┘
        di/AppContainer builds and wires everything above once.
```

**The rules that keep the layers apart**

- `@Composable` functions never touch Retrofit, JSON or Room. They only receive state and call lambdas.
- The ViewModel only knows the `WeatherRepository` **interface** (in `domain`), never `WeatherRepositoryImpl`.
- Retrofit returns the response body as a plain `String`. All JSON decoding happens in the parser classes, so network code and parsing code can be tested separately.
- The `domain` layer has no dependency on Android UI, Retrofit or Room.

---

## 2. Where to start reading

If you only read five files, read these in order:

1. `WeatherApplication.kt` – creates the dependency container.
2. `di/AppContainer.kt` – shows every object in the app and how they are wired.
3. `domain/repository/WeatherRepository.kt` – the full list of things the UI can ask the data layer to do.
4. `data/repository/WeatherRepositoryImpl.kt` – the online/offline logic.
5. `ui/screens/home/WeatherViewModel.kt` – how results become UI state.

---

## 3. Files and responsibilities

### 3.1 App entry and wiring

| File | Responsibility |
|---|---|
| `WeatherApplication.kt` | `Application` subclass. Creates one `DefaultAppContainer` in `onCreate()` and exposes it as `container`. |
| `MainActivity.kt` | The only activity. Reads the container, creates the shared `WeatherViewModel` with an inline `ViewModelProvider.Factory`, observes `userSettings` to pick the theme, and hosts `WeatherNavGraph` inside `WeatherAppTheme`. |
| `di/AppContainer.kt` | `AppContainer` interface plus `DefaultAppContainer`. The single place that builds the `OkHttpClient` (15 s timeouts, basic logging), the one `Retrofit` instance (base URL `https://api.open-meteo.com/`, scalars converter), both API interfaces, both parsers, the Room database and DAOs, the `NetworkMonitor`, and the `WeatherRepositoryImpl`. Everything is `by lazy`. |

### 3.2 `domain/` – the app's vocabulary and contract

| File | Responsibility |
|---|---|
| `repository/WeatherRepository.kt` | The interface the UI layer depends on. Covers: `searchLocations`, `getForecastForLocation`, `getForecastForCoordinates`, `getLatestCachedForecast`, favourites (`getFavouriteLocations` as a `Flow`, add / remove / `isLocationFavourite`) and settings (`getUserSettings` as a `Flow`, `updateUserSettings`). One-shot operations return `Result<T>` so failures are values, not crashes. |
| `model/WeatherForecast.kt` | The aggregate for one location: `location`, `current`, `todayHourly`, `daily` (7 days, index 0 = today), `fetchedTimestampMillis`, and `isFromCache`. Helpers: `todayDaily`, `remainingDaily`, `getHourlyForDate`. |
| `model/CurrentWeather.kt`, `HourlyWeather.kt`, `DailyWeather.kt` | The pieces of a forecast. Values are stored in metric (°C, km/h, mm); conversion happens only when displaying. Each `DailyWeather` carries its own `hourlyForecast`. |
| `model/WeatherCondition.kt` | Enum that maps WMO weather codes from Open-Meteo to a label (e.g. `RAIN`, `THUNDERSTORM`). `fromWmoCode()` falls back to `UNKNOWN`. |
| `model/LocationPlace.kt` | Name, country, latitude, longitude and an `isFavourite` flag. |
| `model/UserSettings.kt` | `UserSettings` plus the enums `TemperatureUnit`, `SpeedUnit`, `ThemeMode`. |
| `util/CoordinateValidator.kt` | Validates latitude/longitude ranges and parses text such as `"59.3293, 18.0686"` into a coordinate pair (returns `null` if the text is a place name instead). |
| `util/WeatherFormatters.kt` | Turns metric values into display text according to `UserSettings`: temperature, wind speed, compass direction, coordinates, day names and hour labels. This is what the screens use. |
| `util/UnitConverter.kt` | A second, older formatter for temperature and speed (one decimal). Covered by tests but **not used by the screens**; `WeatherFormatters` is the one in use. |

All models are `@Serializable`. This is deliberate: it lets a whole `WeatherForecast` be stored in Room as a JSON string (see `WeatherTypeConverters`).

### 3.3 `data/` – where the information comes from

**Remote (network)**

| File | Responsibility |
|---|---|
| `remote/OpenMeteoApi.kt` | Retrofit interface for `GET v1/forecast`. Requests current values, hourly values and daily values for 7 days with `timezone=auto`. Returns `String`. |
| `remote/GeocodingApi.kt` | Retrofit interface for place search. Uses `@Url` to call `https://geocoding-api.open-meteo.com/v1/search` (a different host than the base URL). Returns `String`. |
| `remote/dto/OpenMeteoDto.kt`, `GeocodingDto.kt` | `@Serializable` classes that mirror the JSON exactly (e.g. `temperature_2m` via `@SerialName`). They are internal to the data layer and never reach the UI. |

**Parsing**

| File | Responsibility |
|---|---|
| `parser/ForecastJsonParser.kt` | Decodes the forecast JSON into DTOs and then maps them to a `WeatherForecast`: converts WMO codes, groups the 168 hourly entries by date, attaches each day's hours to its `DailyWeather`, and picks today's 24 hours as `todayHourly`. Missing fields fall back to safe defaults. |
| `parser/GeocodingJsonParser.kt` | Decodes the search JSON into a `List<LocationPlace>` (empty list when there are no results). |

**Local storage (Room)**

| File | Responsibility |
|---|---|
| `local/WeatherDatabase.kt` | The Room database (`weather_database.db`, version 1) holding three tables. Provides a singleton `getDatabase()`. |
| `local/entity/CachedForecastEntity.kt` | Table `cached_forecasts`. One row per coordinate pair (primary key = latitude + longitude) holding the full `WeatherForecast` and a cache timestamp. |
| `local/entity/FavouriteLocationEntity.kt` | Table `favourite_locations`, keyed by latitude + longitude. |
| `local/entity/UserSettingsEntity.kt` | Table `user_settings`. Always a single row with `id = 1`. |
| `local/CachedForecastDao.kt` | Get a forecast by coordinates, get the most recently cached forecast, search cached place names, insert (replace), delete old rows. |
| `local/FavouriteLocationDao.kt` | Observe all favourites as a `Flow`, insert, remove, and count (used as an "is favourite" check). |
| `local/UserSettingsDao.kt` | Observe the settings row as a `Flow`, insert-or-replace. |
| `local/WeatherTypeConverters.kt` | Room `@TypeConverter` that stores `WeatherForecast` as a JSON string using `kotlinx.serialization`. |

**Connectivity**

| File | Responsibility |
|---|---|
| `network/NetworkMonitor.kt` | Wraps Android's `ConnectivityManager`. `isNetworkAvailable()` gives a one-off answer; `observe()` gives a `Flow<Boolean>` that emits whenever connectivity changes. Accepts a `null` context plus a `testOnline` flag so it can be used in unit tests. |

**The coordinator**

| File | Responsibility |
|---|---|
| `repository/WeatherRepositoryImpl.kt` | Implements `WeatherRepository`. It is the only class that knows about APIs, parsers, DAOs and the network monitor at the same time. Runs on `Dispatchers.IO` (injectable for tests), converts exceptions into `Result.failure` with readable messages, and always rethrows `CancellationException`. See section 4 for its decisions. |

### 3.4 `ui/` – what the user sees

| File | Responsibility |
|---|---|
| `screens/home/WeatherViewModel.kt` | **The ViewModel the app actually uses.** One instance is shared by all four screens. Holds `uiState: StateFlow<WeatherUiState>`, the search query and results, and exposes `favouriteLocations` and `userSettings` (Room `Flow`s turned into `StateFlow`s). Loads startup data, reacts to connectivity changes, and handles search, selecting a location, refresh, toggling favourites and updating settings. |
| `screens/home/WeatherUiState.kt` | Sealed interface with four states: `Loading`, `Success(forecast)`, `Offline(cachedForecast, message)` and `Error(message, isNetworkError, httpCode)`. |
| `screens/home/HomeScreen.kt` | Main screen. Shows a search bar, switches to `SearchOverlay` when search is active, and otherwise renders the content for the current `WeatherUiState`. |
| `screens/home/HomeComponents.kt` | Building blocks of the home screen: `TopSearchBar`, `HeroWeatherCard`, `MetricPill`, hourly row/cards, `DailyForecastSection`, `TemperatureRangeBar`, `OfflineModeBanner`, `WeatherConditionIcon` (maps a `WeatherCondition` to an icon). |
| `screens/detail/DayDetailScreen.kt` | Detail view for one day. Reads that day out of the already-loaded forecast by index; it does **not** fetch anything itself. |
| `screens/detail/DayDetailComponents.kt` | Top bar, hero card, 2×2 metric grid and the hour-by-hour list for the detail screen. |
| `screens/favourites/SavedLocationsScreen.kt` | List of saved places with a local filter field. Tap to open a place, or remove it. |
| `screens/settings/SettingsScreen.kt` | Choose temperature unit, wind-speed unit, refresh interval and theme using segmented selectors. |
| `components/SearchOverlay.kt` | Full-screen search UI showing results (or saved places) with a favourite toggle. |
| `components/WeatherStateViews.kt` | Shared `LoadingStateView` and `ErrorStateView` (with retry). |
| `navigation/WeatherNavGraph.kt` | Defines the routes, the bottom navigation bar and the `NavHost`. This is where ViewModel state is collected and passed into the stateless screens. |
| `theme/Color.kt`, `Type.kt`, `Theme.kt` | Material 3 colours, typography and `WeatherAppTheme`, which follows the `ThemeMode` from settings (system / light / dark). |

**Routes** (`WeatherNavGraph.kt`)

| Route | Screen | Notes |
|---|---|---|
| `weather` | `HomeScreen` | Start destination, bottom tab |
| `detail/{dayIndex}` | `DayDetailScreen` | Opened by tapping a day; not a bottom tab |
| `saved` | `SavedLocationsScreen` | Bottom tab |
| `settings` | `SettingsScreen` | Bottom tab |

---

## 4. How the layers connect

### Dependency wiring

```
WeatherApplication ──creates──► DefaultAppContainer
                                     │ lazily builds
   OkHttpClient → Retrofit → OpenMeteoApi, GeocodingApi
   ForecastJsonParser, GeocodingJsonParser
   WeatherDatabase → CachedForecastDao, FavouriteLocationDao, UserSettingsDao
   NetworkMonitor
        └──────────────► WeatherRepositoryImpl  (exposed as WeatherRepository)

MainActivity ──gets container.weatherRepository──► WeatherViewModel ──► WeatherNavGraph ──► screens
```

### Flow 1 – app start
1. `WeatherViewModel.init` asks the repository for the **latest cached forecast**.
2. If one exists, that place becomes the selected location. If the device is online a live refresh starts; if not, the UI goes straight to `Offline` with the cached data.
3. If nothing is cached, the app loads **Stockholm** as the default location.

### Flow 2 – loading a forecast
`HomeScreen` → `WeatherViewModel.selectLocation()` → `WeatherRepository.getForecastForLocation()`:

1. If `NetworkMonitor` says offline → return the cached forecast for those coordinates (marked `isFromCache = true`) or a clear failure.
2. Otherwise `OpenMeteoApi` returns a JSON string → `ForecastJsonParser` turns it into a `WeatherForecast` → the favourite flag is filled in from Room.
3. The result is saved to `cached_forecasts` (replacing the old row for those coordinates), then returned.
4. Network or unexpected errors fall back to the cache. HTTP errors (e.g. 500) are reported as distinct failures.
5. The ViewModel maps the `Result` to `WeatherUiState` (`Success`, `Offline` or `Error`) and the screen redraws.

### Flow 3 – searching
`onSearchQueryChange()` cancels the previous search and calls `searchLocations()`. The repository first checks whether the text is a **coordinate pair** (`CoordinateValidator`) and, if so, returns that point without any network call. Otherwise it calls `GeocodingApi`, parses the result, and marks which results are already favourites. If the network call fails, it searches the cached forecasts by place name instead.

### Flow 4 – connectivity changes
The ViewModel collects `NetworkMonitor.observe()`. Going **offline** while showing `Success` switches to `Offline` with the same forecast. Coming back **online** while in `Offline` or `Error` triggers an automatic reload.

### Flow 5 – favourites and settings
Room exposes both as `Flow`s. The repository maps entities to domain models; the ViewModel turns them into `StateFlow`s; screens read them. Writing (toggle favourite, change a setting) goes the other way through the repository into Room, and the `Flow` re-emits, so the UI updates itself. `MainActivity` also reads `userSettings.themeMode` to apply the theme.

---

## 5. Things worth knowing

- **One shared ViewModel.** `WeatherViewModel` serves all screens. The detail screen shows data from the same `uiState` rather than reloading.
- **Extra ViewModels exist but are not wired into the app.** `HomeViewModel`, `FavouritesViewModel`, `SettingsViewModel`, `DayDetailViewModel` (plus their `*UiState` classes) and `ui/util/ViewModelFactory.kt` are an earlier per-screen design. No screen or navigation code uses them; they are only exercised by unit tests. `WeatherViewModel` is the live path.
- **`NetworkMonitor` is created twice.** `AppContainer` has one, but `MainActivity` (and the default parameter in `WeatherNavGraph`) creates its own for the ViewModel.
- **Refresh interval is stored but not acted on.** The setting is saved in Room and editable in Settings, but nothing schedules automatic refreshes yet.
- **Unused helpers.** `CachedForecastDao.deleteOldForecasts` and `UnitConverter` are not called from app code.
- **Backward-compat theme wrapper.** `WheatherAppTheme` (old spelling) in `Theme.kt` is not used.
- **Design docs.** `app/SPEC.md`, `app/STYLE.md`, `app/CONTEXT.md` describe the original plan and conventions. `STYLE.md`'s file tree is a plan and differs in some file names, so treat this README and the source as the current truth.

---

## 6. Tests

Unit tests live in `app/src/test/java/com/wheatherapp/`, mirroring the main packages:

| Area | Tests |
|---|---|
| `data/parser/` | `ForecastJsonParserTest`, `GeocodingJsonParserTest` |
| `data/repository/` | `WeatherRepositoryTest` (with fake DAOs), `FakeWeatherRepository` (reused by ViewModel tests) |
| `domain/util/` | `CoordinateValidatorTest`, `UnitConverterTest`, `WeatherFormattersTest` |
| `ui/screens/**` | `WeatherViewModelTest`, `HomeViewModelTest`, `FavouritesViewModelTest`, `SettingsViewModelTest`, `DayDetailViewModelTest` |
| `util/` | `MainDispatcherRule` (swaps the main dispatcher for coroutine tests) |

Because the ViewModels depend on the `WeatherRepository` interface and `NetworkMonitor` can run without a context, the tests need no emulator. `app/TEST_REPORT.md` documents the mutation-testing results.

---

## 7. Running the project

1. Open the project root in **Android Studio** and let Gradle sync (JDK 17).
2. Pick an emulator (phone or tablet) or a physical device with API 26+.
3. Run the `app` configuration.
4. The app needs an internet connection for live data (`INTERNET` and `ACCESS_NETWORK_STATE` permissions are declared in `AndroidManifest.xml`).

Unit tests from the command line:

```bash
./gradlew testDebugUnitTest
```

---

## 8. Data sources

- Forecast: `https://api.open-meteo.com/v1/forecast`
- Place search: `https://geocoding-api.open-meteo.com/v1/search`

Weather data by [Open-Meteo.com](https://open-meteo.com/).