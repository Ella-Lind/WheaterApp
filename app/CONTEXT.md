---

### `CONTEXT.md`

```markdown
# CONTEXT.md — Course Constraints, API Specs & Testing Rules

## 1. Course & Assignment Context (Lab B:2)

* **Course Context:** Native Mobile App Development — Pair Programming Lab B:2.
* **Target Platform:** Native **Android** using **Kotlin** and **Jetpack Compose** in **Android Studio**.
* **Execution Environment:** Must compile and run on an official **Android Emulator (Phone AND Tablet AVDs)** or a physical Android device. Browser previews do not count.
* **Live API Mandate:** The final app must query the live public Open-Meteo endpoints (do **not** use the KTH SMHI proxy server, as it uses SMHI's schema rather than Open-Meteo).
* **Oral Exam Accountability:** Both partners must understand and be able to explain every file and line of code, including AI-generated code, and defend architectural trade-offs (`W-18`).

---

## 2. Locked Dependency & Library Policy

Use **Android/Jetpack SDK libraries** together with our locked stack (**Retrofit**, **`kotlinx.serialization`**, and **Room**):

* **UI:** Jetpack Compose (Material 3), `androidx.compose.material:material-icons-extended`, Navigation Compose (`androidx.navigation:navigation-compose`).
* **Architecture & Lifecycle:** `androidx.lifecycle:lifecycle-viewmodel-compose`, `androidx.lifecycle:lifecycle-runtime-compose`, Kotlin Coroutines (`org.jetbrains.kotlinx:kotlinx-coroutines-android`).
* **Networking (Retrofit):**
    * `com.squareup.retrofit2:retrofit`
    * `com.squareup.retrofit2:converter-scalars` (returns raw JSON strings for our dedicated parser layer)
    * `com.squareup.okhttp3:okhttp`
    * Initialize a single `Retrofit` client instance **once** in `AppContainer`.
* **JSON Serialization (`kotlinx.serialization`):**
    * `org.jetbrains.kotlinx:kotlinx-serialization-json` (plus `org.jetbrains.kotlin.plugin.serialization` Gradle plugin).
    * Keep all JSON decoding in `ForecastJsonParser`, `GeocodingJsonParser`, and `WeatherTypeConverters` inside the `data` layer.
* **Local Persistence (Room):**
    * `androidx.room:room-runtime`
    * `androidx.room:room-ktx`
    * `androidx.room:room-compiler` via **KSP** (`com.google.devtools.ksp`)
    * Store cached weather forecasts (`W-10`), favourite locations (`W-08`), and user settings (`W-09`) in a single `WeatherDatabase`.
* **System Services:** Android `ConnectivityManager` (requires `<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />` and `<uses-permission android:name="android.permission.INTERNET" />` in `AndroidManifest.xml`).
* **Testing:** `junit`, `org.jetbrains.kotlinx:kotlinx-coroutines-test`, `com.squareup.okhttp3:mockwebserver` (optional for API contract tests), and fake DAOs/repositories.
* **Prohibited:** Do **not** add Ktor, Moshi, Gson, Jetpack DataStore, SharedPreferences, RxJava, or heavy third-party UI frameworks.

---

## 3. API Specifications (Open-Meteo)

### A. Weather Forecast Endpoint (No API Key Required)
* **Base URL:** `https://api.open-meteo.com/`
* **Retrofit GET Path:** `v1/forecast`
* **Example Full Request:**
  ```text
  [https://api.open-meteo.com/v1/forecast?latitude=59.3293&longitude=18.0686&current=temperature_2m,relative_humidity_2m,apparent_temperature,precipitation,weather_code,wind_speed_10m,wind_direction_10m&hourly=temperature_2m,precipitation_probability,weather_code,wind_speed_10m&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_sum,precipitation_probability_max,wind_speed_10m_max&timezone=auto&forecast_days=7](https://api.open-meteo.com/v1/forecast?latitude=59.3293&longitude=18.0686&current=temperature_2m,relative_humidity_2m,apparent_temperature,precipitation,weather_code,wind_speed_10m,wind_direction_10m&hourly=temperature_2m,precipitation_probability,weather_code,wind_speed_10m&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_sum,precipitation_probability_max,wind_speed_10m_max&timezone=auto&forecast_days=7)