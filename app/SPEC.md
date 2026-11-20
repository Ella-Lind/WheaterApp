# SPEC.md — Native Android Weather App (Lab B:2)

## 1. AI Meta-Instructions
> **READ THIS BEFORE GENERATING ANY CODE:**
> 1. **Source of Truth:** Always read `SPEC.md`, `STYLE.md`, and `CONTEXT.md` at the start of every coding session before proposing changes.
> 2. **Language Rule:** All code, comments, commit messages, and documentation (`SPEC.md`, `docs/TEST_REPORT.md`, `LLM_PROCESS.md`, `REFLECTION.md`) must be written in English.
> 3. **Locked Tech Stack:** Use **Retrofit** (with `ScalarsConverterFactory` returning raw JSON strings) for HTTP, **`kotlinx.serialization`** for JSON parsing and Room type converters, and **Room** for all local persistence (cached forecasts, favourites, and user settings). Never introduce Ktor, Moshi, Gson, DataStore, or SharedPreferences.
> 4. **Requirement Traceability:** Every feature, class, or test generated must map directly to one or more Requirement IDs (`W-01` through `W-18`) listed below. Cite the relevant `W-xx` ID in explanations.
> 5. **Strict Layer Boundaries:** Never place networking, JSON parsing, Room DAO calls, or `NavController` references inside `@Composable` functions or `ViewModel` classes. Follow the exact package and layer rules in `STYLE.md`.
> 6. **No Silent Failures or Fabricated Data:** Every network or I/O path must handle exceptions explicitly, preserve cached Room data on failure, and expose structured UI states (`Loading`, `Success`, `Offline`, `Error`). Never invent or hardcode UI metrics not present in the domain models.
> 7. **Student Ownership Rule:** Never tick requirement checkboxes (`[x]`), write test results, or write `docs/REFLECTION.md` or `docs/LLM_PROCESS.md` on behalf of the students. Remind the students to run the End-of-Session Ritual at the close of each session.

---

## 2. System Architecture Overview

The application follows **Clean MVVM with Unidirectional Data Flow (UDF)** in package `com.wheatherapp`, keeping Retrofit networking, `kotlinx.serialization` parsing, Room persistence, ViewModel state management, and Jetpack Compose UI decoupled:

```text
+-------------------------------------------------------------------+
|                   UI Layer (Jetpack Compose)                      |
|  WeatherNavGraph (NavHost controlled strictly via UI lambdas)     |
|  HomeScreen | DayDetailScreen | SavedLocationsScreen | Settings   |
+---------------------------------+---------------------------------+
                                  | Observes StateFlow<UiState>
                                  v
+-------------------------------------------------------------------+
|                    State Layer (ViewModel)                        |
|                        WeatherViewModel                           |
|          (Uses WeatherFormatters & forecastJob cancellation)      |
+---------------------------------+---------------------------------+
                                  | Calls suspend functions & Flows
                                  v
+-------------------------------------------------------------------+
|                   Domain Layer (Pure Kotlin)                      |
|  Models: WeatherForecast, DailyWeather, HourlyWeather,            |
|          LocationPlace, UserSettings                              |
|  Interfaces: WeatherRepository, NetworkMonitor                    |
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
```

---

## 3. Requirements Checklist (`W-01` – `W-18`)

*(Students tick `[x]` manually once implemented, tested, and verified for oral explanation.)*

| ID | Category | Requirement | Status |
| :--- | :--- | :--- |:-------|
| **W-01** | Basic / API | Fetch live 7-day and hourly weather forecast data from the public Open-Meteo API (`open-meteo.com`) using Retrofit. | `[X]`  |
| **W-02** | Basic / API | Resolve city/place names to geographic coordinates using the public geocoding API, and accept valid floating-point coordinates (`lat, lon`). | `[X]`  |
| **W-03** | Detailed / Parsing | Parse raw JSON responses into internal Kotlin domain models (`WeatherForecast`, `DailyWeather`, `HourlyWeather`, `LocationPlace`) in a dedicated parser class using `kotlinx.serialization`. | `[X]`  |
| **W-04** | Detailed / Cache | Store parsed forecast data locally in Room so the app displays the last successfully fetched forecast when offline. | `[X]`  |
| **W-05** | Architecture / Nav | Set up Compose Navigation (`WeatherNavGraph.kt`) between Home, Day Detail, Saved Locations, and Settings controlled strictly from the UI layer (never from `WeatherViewModel`). | `[X]`  |
| **W-06** | Basic / UI | Home screen displays search bar, current weather conditions, hourly forecast for today (`LazyRow`), and a scrollable 7-day forecast list with temperature and weather condition icons/colors. | `[X]`  |
| **W-07** | Basic / UI | Detail screen (`DayDetailScreen.kt`) displays full forecast for a selected day (temperature high/low, weather condition, max wind, precipitation, and hourly breakdown). | `[X]`  |
| **W-08** | Basic / UI | Allow users to mark/unmark places as favourites and view/select them in a dedicated `SavedLocationsScreen`. | `[X]`  |
| **W-09** | Basic / UI | Settings screen (`SettingsScreen.kt`) allows configuring temperature units (`°C`/`°F`), wind speed units (`km/h`/`m/s`/`mph`), theme (`System`/`Light`/`Dark`), and refresh interval persisted in Room. | `[X]`  |
| **W-10** | Detailed / Async | Execute all network and database operations off the main thread using Kotlin Coroutines (`viewModelScope` / `Dispatchers.IO`). | `[X]`  |
| **W-11** | Detailed / Errors | Handle all network, HTTP, and parsing errors explicitly without crashes or blank screens, distinguishing network errors (`IOException`) from server errors (`HttpException`). | `[X]`  |
| **W-12** | Higher / State | Prevent race conditions on rapid searches by cancelling in-flight forecast jobs (`forecastJob?.cancel()`) and prevent stale data during loading. | `[X]`  |
| **W-13** | Detailed / Layout | Support both portrait and landscape orientations across all screens without clipped controls or overlapping navigation bars. | `[X]`  |
| **W-14** | Higher / Code | Separate concerns cleanly across Data, Domain, State, and UI layers, splitting large screens into concise orchestrators (`*Screen.kt`) and `*Components.kt` files. | `[X]`  |
| **W-15** | Higher / UI | Apply a considered Material 3 visual hierarchy (3-tier Home layout, consistent dark-slate palette, designed Loading/Offline/Error states). | `[X]`  |
| **W-16** | Detailed / Testing | Write runnable, passing unit tests covering JSON parsing, error-state logic, and data transformation/formatting (`./gradlew testDebugUnitTest`), plus mutation tests in `docs/TEST_REPORT.md`. | `[X]`  |
| **W-17** | Detailed / QA | Test and document at least 20 distinct UX scenarios (starting state, action, expected result) on an Android Emulator or physical device in `docs/TEST_REPORT.md`. | `[X]`  |
| **W-18** | Process / Docs | Maintain `SPEC.md`, `README.md`, `docs/LLM_PROCESS.md`, and `docs/REFLECTION.md` in English, and be able to explain every line of code at the oral examination. | `[X]`  |

---

## 4. Component Specifications (Key Modules)

- **`WeatherRepositoryImpl` (`data/repository/`)**: Coordinates `OpenMeteoApi`, `GeocodingApi`, `ForecastJsonParser`, and Room DAOs (`CachedForecastDao`, `FavouriteLocationDao`, `UserSettingsDao`). Implements cache-first fallback on network failure (`W-01`, `W-02`, `W-04`, `W-11`).
- **`ForecastJsonParser` (`data/parser/`)**: Maps raw Open-Meteo and Geocoding JSON strings via `kotlinx.serialization` (`ignoreUnknownKeys = true`) into `WeatherForecast` and `LocationPlace` (`W-03`).
- **`WeatherFormatters` (`domain/util/WeatherFormatters.kt`)**: Pure, unit-testable functions converting temperatures (`°C`/`°F`), wind speeds (`km/h`/`m/s`/`mph`), coordinates, ISO dates/times, and wind descriptions (`W-06`, `W-07`, `W-09`, `W-14`).
- **`WeatherViewModel` (`ui/screens/home/WeatherViewModel.kt`)**: Exposes `StateFlow<WeatherUiState>` (`Loading`, `Success`, `Offline`, `Error`), search state, favourites, and settings. Cancels active `forecastJob` on new searches (`W-05`, `W-10`, `W-11`, `W-12`).
- **`WeatherNavGraph` (`ui/navigation/WeatherNavGraph.kt`)**: Houses `NavHost` and bottom `NavigationBar`, passing navigation events solely as composable lambdas (`W-05`).

---

## 5. Oral Exam Map (Questions 1–30)

*(Verify and update exact line numbers and unit test names before presentation.)*

| # | Oral Examination Question | Req ID | Code Location                                                                                                                       | Test / Verification |
| :--- | :--- | :--- |:------------------------------------------------------------------------------------------------------------------------------------| :--- |
| **Q1** | Walk through the function that makes the API request. Where is it, and what does it return? | `W-01`, `W-02` | `OpenMeteoApi.kt` & `WeatherRepositoryImpl.kt` (`getForecastForLocation`)                                                           | Repository / Parser unit tests |
| **Q2** | How is the JSON response parsed into your data model? Show the parsing code. | `W-03` | `ForecastJsonParser.kt` (`parse`)                                                                                                   | `ForecastJsonParserTest` |
| **Q3** | What fields from the API response does your app use, and what does it ignore? Why those? | `W-03`, `W-06` | `ForecastJsonParser.kt` & `WeatherForecast.kt`                                                                                      | `ForecastJsonParserTest` |
| **Q4** | What happens if the API returns a successful HTTP 200 but the JSON is missing a field your model expects? | `W-03`, `W-11` | `ForecastJsonParser.kt` (`SerializationException` handling)                                                                         | `ForecastJsonParserTest` (malformed/missing field test) |
| **Q5** | Trace a complete data flow from the user pressing "search" to the forecast appearing on screen. | `W-01`, `W-02`, `W-06` | `SearchOverlay.kt` -> `WeatherViewModel.kt` -> `WeatherRepositoryImpl.kt` -> `HomeScreen.kt`                                        | UX-01, UX-02 |
| **Q6** | How does your app handle caching — what is stored locally, and when is it refreshed? | `W-04`, `W-09` | `WeatherDatabase.kt`, `CachedForecastDao.kt`, `WeatherRepositoryImpl.kt`                                                            | UX-05, UX-16 |
| **Q7** | Where does the fetched weather data live while the app is running? Show the declaration. | `W-05`, `W-12` | `WeatherViewModel.kt` (`_uiState: MutableStateFlow<WeatherUiState>`)                                                                | `WeatherViewModelTest` |
| **Q8** | What happens to the displayed data when the user searches for a new city? | `W-06`, `W-12` | `WeatherViewModel.kt` (`selectLocation` / `loadForecastForLocation`)                                                                | UX-01, UX-02 |
| **Q9** | How does the UI know when new data has arrived? Trace the update from data layer to screen. | `W-05`, `W-14` | `WeatherViewModel.kt` (`StateFlow`) -> `HomeScreen.kt` (`collectAsStateWithLifecycle`)                                              | UX-01 |
| **Q10** | What state is preserved if the user leaves the app and returns? | `W-04`, `W-08`, `W-09` | Room entities (`CachedForecastEntity`, `FavouriteEntity`, `SettingsEntity`) + `NavGraph` `saveState`                                | UX-05, UX-14 |
| **Q11** | Where in the code is the loading state managed? How do you prevent the UI from showing stale data while a new request is in flight? | `W-11`, `W-12` | `WeatherUiState.Loading` in `WeatherViewModel.kt` & `LoadingStateView` in `WeatherStateViews.kt`                                    | UX-01, `WeatherViewModelTest` |
| **Q12** | If the user rapidly searches for two cities, what happens? Can a race condition occur, and how do you guard against it? | `W-12` | `WeatherViewModel.kt` (`private var forecastJob: Job?`, `forecastJob?.cancel()`)                                                    | `WeatherViewModelTest` |
| **Q13** | What does the app display if the network request times out? | `W-11` | `WeatherRepositoryImpl.kt` (`SocketTimeoutException` / `IOException`) -> `WeatherStateViews.kt`                                     | Error-state unit test, UX-17 |
| **Q14** | Where do you distinguish between a network error and an API error response (e.g., HTTP 404 or 500)? | `W-11` | `WeatherRepositoryImpl.kt` / `WeatherViewModel.kt` (`IOException` vs `HttpException` / `httpCode`)                                  | Error-state unit test |
| **Q15** | What happens if the user has no internet connection and opens the app for the first time? | `W-04`, `W-11` | `WeatherViewModel.kt` (`loadStartupData`) -> `WeatherStateViews` (`DISCONNECTED`, no cache)                                         | UX-15 |
| **Q16** | What happens if they open it a second time, after having successfully loaded data before? | `W-04`, `W-11` | `WeatherViewModel.kt` (`getLatestCachedForecast()`) -> `WeatherUiState.Offline` + `"Offline Mode"` banner                           | UX-05, UX-16 |
| **Q17** | Where is the error state declared, and how does it flow to the UI? | `W-11` | `WeatherUiState.kt` (`WeatherUiState.Error`) -> `WeatherViewModel.kt` -> `HomeScreen.kt` -> `WeatherStateViews.kt`                  | Error-state unit test |
| **Q18** | What was the hardest error case to handle? Show the code. | `W-04`, `W-11` | `WeatherRepositoryImpl.kt` (offline fallback + preserving cached Room forecast on HTTP/IO failure)                                  | Error-state unit test |
| **Q19** | Show the MVVM layers in your code: what belongs to the View, what to the ViewModel, what to the Model/Repository? | `W-14` | View: `ui/screens/*`; ViewModel: `WeatherViewModel.kt`; Model/Repo: `domain/*` & `data/*`                                           | Architecture diagram in `README.md` / `SPEC.md` |
| **Q20** | Why is the network call not made directly from the View? | `W-10`, `W-14` | Separation of concerns, lifecycle safety across configuration changes (rotation), and unit testability                              | `WeatherViewModelTest` |
| **Q21** | Where in the code is testable logic separated from UI code? What made that separation easy or hard? | `W-14`, `W-16` | `ForecastJsonParser.kt`, `WeatherFormatters.kt`, `WeatherRepositoryImpl.kt`, `WeatherViewModel.kt`                                  | `app/src/test/...` |
| **Q22** | How does navigation between screens work? Why does the ViewModel not navigate itself? | `W-05` | `WeatherNavGraph.kt` (`NavHost` + UI callbacks `onDayClick`, `onNavigateBack`)                                                      | UX-04, UX-08 |
| **Q23** | Open SPEC.md. Walk through how one requirement ID maps to the code that implements it. | `W-01` – `W-18` | `SPEC.md` Section 3 -> e.g. `W-09` in `SettingsScreen.kt` & `WeatherFormatters.kt`                                                  | `WeatherFormattersTest` |
| **Q24** | What would you change about your architecture if you had to add hourly forecast support tomorrow? | `W-03`, `W-06`, `W-07` | Already modeled via `DailyWeather.hourlyForecast: List<HourlyWeather>` in domain, parser, Room converter, and `HourlyMatrixSection` | `ForecastJsonParserTest` |
| **Q25** | Show a function the AI helped you write. Explain it line by line in your own words. | `W-18` | e.g., `BentoMetricGrid` (`DayDetailComponents.kt`) or `WeatherNavGraph` (`WeatherNavGraph.kt`)                                      | `docs/LLM_PROCESS.md` |
| **Q26** | Open SPEC.md and show us the AI meta-instructions at the top. Walk us through one requirement and find the code that implements it. | `W-18` | `SPEC.md` Section 1 -> `W-05` (`WeatherNavGraph.kt`) or `W-06` (`HomeScreen.kt`)                                                    | Opening exam question |
| **Q27** | Where did the AI suggest something you pushed back on or changed? What was your reasoning? | `W-06`, `W-18` | `DayDetailComponents.kt` (`BentoMetricGrid`: removed AI-fabricated humidity & hardcoded `"NW"` wind direction)                      | `docs/LLM_PROCESS.md` |
| **Q28** | How did you verify that AI-generated code actually works correctly? Show a test case. | `W-16`, `W-17` | `./gradlew testDebugUnitTest`, mutation testing, and 20 Emulator UX scenarios                                                       | `docs/TEST_REPORT.md` |
| **Q29** | What was the most effective prompt you wrote during this lab? What made it work? | `W-18` | Structured multi-constraint Compose/architecture prompts referencing `SPEC.md`, `STYLE.md`, and domain models                       | `docs/LLM_PROCESS.md` |
| **Q30** | If you were starting this lab again with the same AI tools, what would you do differently? | `W-18` | Enforce strict domain-field checks in Stitch/UI prompts earlier to avoid hallucinated metrics                                       | `docs/REFLECTION.md` |

---

## 6. End-of-Session Update Log

| Date       | Session Focus | Summary of Changes |
|:-----------| :--- | :--- |
| 2026-11-18 | Architecture, Data & Domain Layer | Set up Retrofit (`OpenMeteoApi`, `GeocodingApi`), `ForecastJsonParser` and `GeocodingJsonParser` (`kotlinx.serialization`), Room database (`CachedForecastDao`, `FavouriteLocationDao`, `UserSettingsDao`), `NetworkMonitor`, and pure Kotlin domain models (`W-01`–`W-04`, `W-10`). |
| 2026-11-19 | UI, Compose Navigation & Refactor | Built Material 3 screens (`HomeScreen`, `DayDetailScreen`, `SavedLocationsScreen`, `SettingsScreen`) and UI-controlled `WeatherNavGraph`; refactored large screens into `*Components.kt` files, centralized formatting in `WeatherFormatters`, removed fabricated detail metrics, and added `forecastJob?.cancel()` (`W-05`–`W-09`, `W-11`–`W-15`). |
| 2026-11-20 | Testing, Audit & Final Docs | Ran `./gradlew testDebugUnitTest`, executed mutation tests, verified 20 portrait/landscape UX scenarios on the Android Emulator, mapped Q1–Q30 to exact code lines, and finalized `SPEC.md`, `docs/TEST_REPORT.md`, `LLM_PROCESS.md`, and `REFLECTION.md` (`W-16`–`W-18`). |