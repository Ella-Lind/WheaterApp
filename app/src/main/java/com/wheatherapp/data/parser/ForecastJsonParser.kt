package com.wheatherapp.data.parser

import com.wheatherapp.data.remote.dto.OpenMeteoResponseDto
import com.wheatherapp.domain.model.CurrentWeather
import com.wheatherapp.domain.model.DailyWeather
import com.wheatherapp.domain.model.HourlyWeather
import com.wheatherapp.domain.model.LocationPlace
import com.wheatherapp.domain.model.WeatherCondition
import com.wheatherapp.domain.model.WeatherForecast
import kotlinx.serialization.json.Json

/**
 * Dedicated parser for converting raw JSON String responses from Retrofit
 * into clean internal domain models (WeatherForecast).
 */
class ForecastJsonParser(
    private val json: Json = Json { ignoreUnknownKeys = true; isLenient = true }
) {
    /**
     * Parses the raw JSON string from Open-Meteo into a [WeatherForecast] domain model.
     * Groups hourly data by date so today's 24-hour prognosis and each subsequent day's 
     * hourly forecast are neatly organized.
     *
     * @param jsonString Raw HTTP response payload.
     * @param location Target location associated with the forecast request.
     * @param timestampMillis Time when the response was fetched.
     */
    fun parse(
        jsonString: String,
        location: LocationPlace,
        timestampMillis: Long = System.currentTimeMillis()
    ): WeatherForecast {
        val dto = json.decodeFromString<OpenMeteoResponseDto>(jsonString)

        // 1. Parse Current Weather
        val currentDto = dto.current
        val currentWeather = if (currentDto != null) {
            CurrentWeather(
                temperatureCelsius = currentDto.temperature2m,
                relativeHumidityPercent = currentDto.relativeHumidity2m,
                apparentTemperatureCelsius = currentDto.apparentTemperature,
                precipitationMm = currentDto.precipitation,
                weatherCondition = WeatherCondition.fromWmoCode(currentDto.weatherCode),
                windSpeedKmh = currentDto.windSpeed10m,
                windDirectionDegrees = currentDto.windDirection10m
            )
        } else {
            CurrentWeather(
                temperatureCelsius = 0.0,
                relativeHumidityPercent = 0,
                apparentTemperatureCelsius = 0.0,
                precipitationMm = 0.0,
                weatherCondition = WeatherCondition.UNKNOWN,
                windSpeedKmh = 0.0,
                windDirectionDegrees = 0
            )
        }

        // 2. Parse All Hourly Entries (e.g. 168 hours for 7 days)
        val hourlyList = mutableListOf<HourlyWeather>()
        val hourlyDto = dto.hourly
        if (hourlyDto != null) {
            val count = hourlyDto.time.size
            for (i in 0 until count) {
                val timeIso = hourlyDto.time.getOrNull(i) ?: continue
                val temp = hourlyDto.temperature2m.getOrNull(i) ?: 0.0
                val precipProb = hourlyDto.precipitationProbability.getOrNull(i) ?: 0
                val wmoCode = hourlyDto.weatherCode.getOrNull(i) ?: 0
                val wind = hourlyDto.windSpeed10m.getOrNull(i) ?: 0.0

                hourlyList.add(
                    HourlyWeather(
                        timeIso = timeIso,
                        temperatureCelsius = temp,
                        precipitationProbabilityPercent = precipProb,
                        weatherCondition = WeatherCondition.fromWmoCode(wmoCode),
                        windSpeedKmh = wind
                    )
                )
            }
        }

        // 3. Group Hourly Entries by Date ("YYYY-MM-DD")
        val hourlyByDate: Map<String, List<HourlyWeather>> = hourlyList.groupBy {
            it.timeIso.substringBefore('T')
        }

        // Determine Today's Date ISO string (first daily entry or first hourly entry)
        val todayDateIso = dto.daily?.time?.firstOrNull()
            ?: hourlyList.firstOrNull()?.timeIso?.substringBefore('T')
            ?: ""

        // Extract Today's 24-hour Detailed Prognosis
        val todayHourly = hourlyByDate[todayDateIso] ?: hourlyList.take(24)

        // 4. Parse Daily Weather & Attach Matching Hourly Prognosis to Each Day
        val dailyList = mutableListOf<DailyWeather>()
        val dailyDto = dto.daily
        if (dailyDto != null) {
            val count = dailyDto.time.size
            for (i in 0 until count) {
                val dateIso = dailyDto.time.getOrNull(i) ?: continue
                val wmoCode = dailyDto.weatherCode.getOrNull(i) ?: 0
                val maxTemp = dailyDto.temperature2mMax.getOrNull(i) ?: 0.0
                val minTemp = dailyDto.temperature2mMin.getOrNull(i) ?: 0.0
                val precipSum = dailyDto.precipitationSum.getOrNull(i) ?: 0.0
                val maxPrecipProb = dailyDto.precipitationProbabilityMax.getOrNull(i) ?: 0
                val maxWind = dailyDto.windSpeed10mMax.getOrNull(i) ?: 0.0

                // Match 24 hourly prognosis items specifically for this day
                val dayHourlyPrognosis = hourlyByDate[dateIso] ?: emptyList()

                dailyList.add(
                    DailyWeather(
                        dateIso = dateIso,
                        weatherCondition = WeatherCondition.fromWmoCode(wmoCode),
                        maxTemperatureCelsius = maxTemp,
                        minTemperatureCelsius = minTemp,
                        precipitationSumMm = precipSum,
                        maxPrecipitationProbabilityPercent = maxPrecipProb,
                        maxWindSpeedKmh = maxWind,
                        hourlyForecast = dayHourlyPrognosis
                    )
                )
            }
        }

        return WeatherForecast(
            location = location,
            current = currentWeather,
            todayHourly = todayHourly,
            daily = dailyList,
            fetchedTimestampMillis = timestampMillis,
            isFromCache = false
        )
    }
}
