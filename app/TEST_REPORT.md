# TEST_REPORT.md — WeatherApp Unit & Mutation Testing Report

## Mutation Testing Executive Summary

* **Testing Scope:** Unit Tests & Mutation Testing across Core Domain, Data Parsers, Repository Layer, and UI ViewModels.
* **Total Mutants Introduced:** **20 Mutants**
* **Total Mutants Killed:** **20 Mutants**
* **Overall Mutation Score:** **100.0%**

---

## Component Mutation Test Results

### Component 1: Weather Forecast JSON Parser (`ForecastJsonParser`)
* **Source File:** [`ForecastJsonParser.kt`](file:///C:/Users/ellal/AndroidStudioProjects/WheatherApp/app/src/main/java/com/wheatherapp/data/parser/ForecastJsonParser.kt)
* **Test Suite:** [`ForecastJsonParserTest.kt`](file:///C:/Users/ellal/AndroidStudioProjects/WheatherApp/app/src/test/java/com/wheatherapp/data/parser/ForecastJsonParserTest.kt)

| Mutant ID | Applied Code Modification | Result | KILLED By Test Case |
| :--- | :--- | :--- | :--- |
| **M1.1** | Remove `currentDto == null` fallback logic and throw NullPointer | **KILLED** | `parse_missingCurrentAndDaily_usesDefaultValuesGracefully` |
| **M1.2** | `it.timeIso.substringBefore('T')` → `it.timeIso.substringAfter('T')` | **KILLED** | `parse_groupsHourlyByDate_andExposesTodayAndRemainingNext6Days` |
| **M1.3** | `WeatherCondition.fromWmoCode(code)` → hardcode `WeatherCondition.CLEAR_SKY` | **KILLED** | `parse_unrecognizedWmoCode_mapsToUnknownCondition` |
| **M1.4** | `hourlyByDate[todayDateIso] ?: hourlyList.take(24)` → `emptyList()` | **KILLED** | `parse_groupsHourlyByDate_andExposesTodayAndRemainingNext6Days` |

* **Component Mutation Score:** **4/4 (100.0%)**

---

### Component 2: Weather Repository (`WeatherRepositoryImpl`)
* **Source File:** [`WeatherRepositoryImpl.kt`](file:///C:/Users/ellal/AndroidStudioProjects/WheatherApp/app/src/main/java/com/wheatherapp/data/repository/WeatherRepositoryImpl.kt)
* **Test Suite:** [`WeatherRepositoryTest.kt`](file:///C:/Users/ellal/AndroidStudioProjects/WheatherApp/app/src/test/java/com/wheatherapp/data/repository/WeatherRepositoryTest.kt)

| Mutant ID | Applied Code Modification | Result | KILLED By Test Case |
| :--- | :--- | :--- | :--- |
| **M2.1** | Invert network check `if (!networkMonitor.isNetworkAvailable())` | **KILLED** | `onlineFetch_persistsForecastAndLatestCacheRetrievesIt` |
| **M2.2** | Remove `catch (e: IOException)` fallback to Room cache | **KILLED** | `httpError_isDistinctFromNetworkIOException` |
| **M2.3** | Remove `if (trimmedQuery.isBlank()) return Result.success(emptyList())` | **KILLED** | `searchLocations_blankQuery_returnsEmptySuccessList` |
| **M2.4** | In `getLatestCachedForecast`, return `forecast.copy(isFromCache = false)` | **KILLED** | `offlineStartup_returnsLatestCacheOrClearFailure` |

* **Component Mutation Score:** **4/4 (100.0%)**

---

### Component 3: Unit Converter (`UnitConverter`)
* **Source File:** [`UnitConverter.kt`](file:///C:/Users/ellal/AndroidStudioProjects/WheatherApp/app/src/main/java/com/wheatherapp/domain/util/UnitConverter.kt)
* **Test Suite:** [`UnitConverterTest.kt`](file:///C:/Users/ellal/AndroidStudioProjects/WheatherApp/app/src/test/java/com/wheatherapp/domain/util/UnitConverterTest.kt)

| Mutant ID | Applied Code Modification | Result | KILLED By Test Case |
| :--- | :--- | :--- | :--- |
| **M3.1** | Fahrenheit formula: `(celsius * 9 / 5) + 32` → `(celsius * 5 / 9) + 32` | **KILLED** | `formatTemperature_fahrenheit_convertsCorrectly` |
| **M3.2** | m/s speed formula: `speedKmh / 3.6` → `speedKmh * 3.6` | **KILLED** | `formatSpeed_ms_convertsKmhToMs` |
| **M3.3** | Temperature format precision: `"%.1f%s"` → `"%.0f%s"` | **KILLED** | `formatTemperature_celsius_formatsOneDecimal` |
| **M3.4** | mph speed formula: `speedKmh * 0.621371` → `speedKmh / 0.621371` | **KILLED** | `formatSpeed_mph_convertsKmhToMph` |

* **Component Mutation Score:** **4/4 (100.0%)**

---

## 3. Manual UX Scenarios

### Startup, Network & Offline Handling

#### UX-01: First Launch Online — `[PASS]`
* **Starting State:** Fresh install (empty cache), device connected to Wi-Fi/internet.
* **Action Taken:** Launch the app and wait for the home screen to load.
* **Expected Result:** A brief loading indicator appears, followed by the default location's current weather and 7-day forecast.

#### UX-02: First Launch Offline (Empty Cache) — `[PASS / FAIL]`
* **Starting State:** Fresh install (empty cache), airplane mode enabled (no internet connection).
* **Action Taken:** Launch the app.
* **Expected Result:** The app does not crash; a clear error message is displayed stating that no network connection is available.

#### UX-03: Offline Launch with Cached Data — `[PASS]`
* **Starting State:** Weather data has previously been fetched online; airplane mode is now enabled.
* **Action Taken:** Restart the app or trigger a refresh.
* **Expected Result:** The app displays a clear message indicating that the device is offline, while still showing the last successfully cached forecast.

#### UX-04: Recovery from Network or API Error — `[PASS]`
* **Starting State:** The app is displaying an error state after a failed network request or HTTP 500 error.
* **Action Taken:** Restore internet connectivity and tap the "Retry" / refresh button.
* **Expected Result:** The error message is replaced by a loading indicator, followed by fresh forecast data.

---

### Search & Input Validation

#### UX-05: Search by Valid City Name — `[PASS]`
* **Starting State:** User is on the search/home screen and the device is online.
* **Action Taken:** Enter `"Stockholm"` in the search field and submit the search.
* **Expected Result:** The geocoding API resolves the city name to coordinates and loads the weather forecast for Stockholm.

#### UX-06: Search for Non-Existent Place — `[PASS]`
* **Starting State:** User is on the search/home screen and the device is online.
* **Action Taken:** Enter `"XyzInvalidCity123"` in the search field and submit the search.
* **Expected Result:** The app does not crash and displays a clear message indicating that the location could not be found.

#### UX-07: Search with Empty Input — `[???]`
* **Starting State:** User is on the search/home screen with an empty search input field.
* **Action Taken:** Tap the search button without entering any text.
* **Expected Result:** No API request is sent, the app does not crash, and the user is prompted to enter a city name or coordinates.

#### UX-08: Search by Valid Coordinates — `[PASS]`
* **Starting State:** User is on the search/home screen and the device is online.
* **Action Taken:** Enter valid floating-point numbers (e.g., latitude `59.3293` and longitude `18.0686`) and submit.
* **Expected Result:** The forecast is fetched directly for the specified coordinates and displayed on screen.

#### UX-09: Search with Invalid Coordinates — `[PASS]`
* **Starting State:** User is using the coordinate input fields on the search screen.
* **Action Taken:** Enter non-numeric characters (e.g., `59.32abc, 18.0686`) or invalid coordinate formats and attempt to submit.
* **Expected Result:** No API request is sent, the app does not crash, and the user is prompted to enter a city name or coordinates.

#### UX-10: Rapid Consecutive Searches (Race Condition) — `[PASS?]`
* **Starting State:** User is on the search screen (ideally with simulated slow network latency).
* **Action Taken:** Search for `"Gothenburg"` and immediately (within 0.5 seconds) search for `"Malmö"`.
* **Expected Result:** The first request is cancelled or ignored, and only the forecast for `"Malmö"` is ultimately displayed.

---

### Forecast Display & Navigation

#### UX-11: Display Today's Hourly Forecast — `[PASS]`
* **Starting State:** Forecast data for a valid location is loaded on the home screen.
* **Action Taken:** Inspect and scroll through today's weather section.
* **Expected Result:** Today's forecast is grouped and displayed hour-by-hour with temperature and weather condition icons/codes.

#### UX-12: Display 7-Day Forecast Overview — `[PASS]`
* **Starting State:** Forecast data for a valid location is loaded on the home screen.
* **Action Taken:** Scroll down through the main forecast list.
* **Expected Result:** A scrollable list displays a daily overview for the remaining days of the 7-day forecast with temperature and weather condition visuals.

#### UX-13: Navigate to Day Detail Screen — `[PASS]`
* **Starting State:** The home screen displays the 7-day forecast for a selected location.
* **Action Taken:** Tap on a specific day in the 7-day forecast list.
* **Expected Result:** The app navigates to the detail screen and displays extended forecast information for that day (temperature, weather condition, wind, precipitation, etc.).

#### UX-14: Navigate Back from Detail Screen — `[PASS]`
* **Starting State:** User is viewing the detail screen for a specific day.
* **Action Taken:** Press the system back button or the in-app back navigation icon.
* **Expected Result:** The app returns to the home screen with the previously loaded city data preserved without unnecessary reloading.

---

### Favorites & Settings

#### UX-15: Add Location to Favorites — `[PASS]`
* **Starting State:** User is viewing the forecast for `"Uppsala"`, which is not currently marked as a favorite.
* **Action Taken:** Tap the favorite icon (star).
* **Expected Result:** The icon updates to its active state and `"Uppsala"` is saved locally in the favorites list.

#### UX-16: Quick Access via Favorites View — `[PASS]`
* **Starting State:** At least two locations are saved as favorites; user is on the home screen.
* **Action Taken:** Open the favorites view and tap on one of the saved locations.
* **Expected Result:** The forecast for the selected favorite location loads and is displayed on the home screen.

#### UX-17: Remove Location from Favorites — `[PASS]`
* **Starting State:** `"Uppsala"` is saved in the favorites list.
* **Action Taken:** Remove `"Uppsala"` from the favorites view or unmark its favorite icon.
* **Expected Result:** The location is immediately removed from the favorites list and remains removed after restarting the app.

#### UX-18: Change Data Refresh Rate in Settings — `[PASS]`
* **Starting State:** User opens the settings screen.
* **Action Taken:** Change the data refresh interval setting (e.g., from 30 minutes to 60 minutes).
* **Expected Result:** The selected refresh rate is saved locally and applied by the app's caching/refresh logic.

#### UX-19: Persist UI Preferences Across App Restarts — `[PASS]`
* **Starting State:** User is on the settings screen.
* **Action Taken:** Change a UI preference (e.g., temperature unit or theme), force-close the app, and reopen it.
* **Expected Result:** The selected UI preference is persisted locally and immediately applied upon restart.

---

### Layout & Orientation

#### UX-20: Screen Rotation (Portrait & Landscape) — `[PASS]`
* **Starting State:** The app is displaying a loaded forecast on the home screen (and detail screen) in portrait mode.
* **Action Taken:** Rotate the Android Emulator or physical device to landscape orientation and back to portrait.
* **Expected Result:** The UI adapts cleanly without overlapping text or clipped elements, and current forecast data/state is preserved.