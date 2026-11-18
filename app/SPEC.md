# SPEC.md — Native Android Weather App (Lab B:2)

## AI Meta-Instructions
> **READ THIS BEFORE GENERATING ANY CODE:**
> 1. **Source of Truth:** Always read `SPEC.md`, `STYLE.md`, and `CONTEXT.md` at the start of every coding session before proposing changes.
> 2. **Locked Tech Stack:** Use **Retrofit** (with `ScalarsConverterFactory` returning raw JSON strings) for HTTP, **`kotlinx.serialization`** for JSON parsing and Room type converters, and **Room** for all local persistence (cached forecasts, favourites, and user settings). Never introduce Ktor, Moshi, Gson, DataStore, or SharedPreferences.
> 3. **Requirement Traceability:** Every feature, class, or test you generate must map directly to one or more Requirement IDs (`W-01` through `W-18`) listed below. Cite the relevant `W-xx` ID in your explanation.
> 4. **Strict Layer Boundaries:** Never place networking, JSON parsing, or Room DAO calls inside `@Composable` functions or `ViewModel` classes. Follow the exact package and layer rules in `STYLE.md`.
> 5. **No Silent Failures:** Every network or I/O path must handle exceptions explicitly, preserve cached Room data on failure, and expose structured UI states (`Loading`, `Success`, `Error`, plus offline indicators).
> 6. **Living Document Rule:** At the end of every working session, update the **Requirement Status** column in this file and append a new entry to the **End-of-Session Update Log** (and remind the user to log the interaction in `LLM_PROCESS.md`).

---

## System Architecture Overview

The application follows **Clean MVVM with Unidirectional Data Flow (UDF)** so that Retrofit networking, `kotlinx.serialization` parsing, Room persistence, ViewModel state management, and Jetpack Compose UI remain strictly decoupled:

```text
+-------------------------------------------------------------------+
|                        UI Layer (Compose)                         |
|  HomeScreen  |  DayDetailScreen  |  FavouritesScreen  | Settings  |
+---------------------------------+---------------------------------+
                                  | Observes StateFlow<UiState>
                                  v
+-------------------------------------------------------------------+
|                    State Layer (ViewModels)                       |
|   HomeViewModel   |   FavouritesViewModel   |   SettingsViewModel |
+---------------------------------+---------------------------------+
                                  | Calls suspend functions & Flows
                                  v
+-------------------------------------------------------------------+
|                   Domain Layer (Pure Kotlin)                      |
|  Models: WeatherForecast, HourlyWeather, DailyWeather, Location   |
|  Interfaces: WeatherRepository                                    |
+---------------------------------+---------------------------------+
                                  | Implemented by
                                  v
+-------------------------------------------------------------------+
|                      Data Layer (Repository)                      |
|  +-----------------------+  +----------------+  +--------------+  |
|  | Remote (Retrofit)     |  | JSON Parsers   |  | Local (Room) |  |
|  | - OpenMeteoApi        |->| - ForecastJson |->| - Forecasts  |  |
|  | - GeocodingApi        |  |   Parser       |  | - Favourites |  |
|  |   (Raw JSON String)   |  | (kotlinx.ser.) |  | - Settings   |  |
|  +-----------------------+  +----------------+  +--------------+  |
|  +-------------------------------------------------------------+  |
|  | NetworkMonitor (ConnectivityManager + ACCESS_NETWORK_STATE) |  |
|  +-------------------------------------------------------------+  |
+-------------------------------------------------------------------+