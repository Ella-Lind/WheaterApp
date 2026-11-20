package com.wheatherapp.data.parser

import com.wheatherapp.domain.model.LocationPlace
import com.wheatherapp.domain.model.WeatherCondition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ForecastJsonParserTest {

    private val parser = ForecastJsonParser()

    private val sampleLocation = LocationPlace(
        name = "Stockholm",
        country = "Sweden",
        latitude = 59.3293,
        longitude = 18.0686
    )

    @Test
    fun parse_groupsHourlyByDate_andExposesTodayAndRemainingNext6Days() {
        val sampleJson = """
        {
          "latitude": 59.3293,
          "longitude": 18.0686,
          "current": {
            "temperature_2m": 12.5,
            "relative_humidity_2m": 75,
            "apparent_temperature": 11.0,
            "precipitation": 0.0,
            "weather_code": 3,
            "wind_speed_10m": 15.2,
            "wind_direction_10m": 180
          },
          "hourly": {
            "time": [
              "2025-02-17T00:00", "2025-02-17T01:00",
              "2025-02-18T00:00", "2025-02-18T01:00",
              "2025-02-19T00:00", "2025-02-20T00:00",
              "2025-02-21T00:00", "2025-02-22T00:00",
              "2025-02-23T00:00"
            ],
            "temperature_2m": [10.0, 10.5, 12.0, 12.5, 14.0, 15.0, 16.0, 17.0, 18.0],
            "precipitation_probability": [0, 5, 10, 15, 20, 25, 30, 35, 40],
            "weather_code": [0, 1, 2, 3, 45, 51, 61, 71, 80],
            "wind_speed_10m": [5.0, 5.5, 6.0, 6.5, 7.0, 7.5, 8.0, 8.5, 9.0]
          },
          "daily": {
            "time": [
              "2025-02-17", "2025-02-18", "2025-02-19",
              "2025-02-20", "2025-02-21", "2025-02-22", "2025-02-23"
            ],
            "weather_code": [0, 2, 3, 45, 61, 71, 80],
            "temperature_2m_max": [15.0, 16.0, 17.0, 18.0, 19.0, 20.0, 21.0],
            "temperature_2m_min": [8.0, 9.0, 10.0, 11.0, 12.0, 13.0, 14.0],
            "precipitation_sum": [0.0, 0.5, 1.0, 1.5, 2.0, 2.5, 3.0],
            "precipitation_probability_max": [10, 20, 30, 40, 50, 60, 70],
            "wind_speed_10m_max": [10.0, 12.0, 14.0, 16.0, 18.0, 20.0, 22.0]
          }
        }
        """.trimIndent()

        val forecast = parser.parse(sampleJson, sampleLocation)

        // Verify total daily count (7 days fetched)
        assertEquals(7, forecast.daily.size)

        // Verify today's hourly prognosis (2 entries for 2025-02-17 in test JSON)
        assertEquals(2, forecast.todayHourly.size)
        assertEquals("2025-02-17T00:00", forecast.todayHourly[0].timeIso)

        // Verify remaining 6 days forecast
        assertEquals(6, forecast.remainingDaily.size)
        assertEquals("2025-02-18", forecast.remainingDaily[0].dateIso)
        assertEquals("2025-02-23", forecast.remainingDaily[5].dateIso)

        // Verify each daily item contains its corresponding hourly prognosis
        val feb18 = forecast.daily.find { it.dateIso == "2025-02-18" }
        assertNotNull(feb18)
        assertEquals(2, feb18?.hourlyForecast?.size)
        assertEquals("2025-02-18T00:00", feb18?.hourlyForecast?.get(0)?.timeIso)
    }

    @Test(expected = Exception::class)
    fun parse_malformedJson_throwsException() {
        val malformedJson = "{ invalid_forecast_json }"
        parser.parse(malformedJson, sampleLocation)
    }

    @Test
    fun parse_missingCurrentAndDaily_usesDefaultValues() {
        val incompleteJson = """
        {
          "latitude": 59.3293,
          "longitude": 18.0686
          
        }
        """.trimIndent()

        val forecast = parser.parse(incompleteJson, sampleLocation)

        assertEquals(0.0, forecast.current.temperatureCelsius, 0.001)
        assertEquals(WeatherCondition.UNKNOWN, forecast.current.weatherCondition)
        assertEquals(0, forecast.daily.size)
        assertEquals(0, forecast.todayHourly.size)
    }

    @Test
    fun parse_unrecognizedWmoCode_mapsToUnknownCondition() {
        val jsonWithInvalidWmo = """
        {
          "latitude": 59.3293,
          "longitude": 18.0686,
          "current": {
            "temperature_2m": 15.0,
            "relative_humidity_2m": 50,
            "apparent_temperature": 15.0,
            "precipitation": 0.0,
            "weather_code": 999,
            "wind_speed_10m": 10.0,
            "wind_direction_10m": 90
          }
        }
        """.trimIndent()

        val forecast = parser.parse(jsonWithInvalidWmo, sampleLocation)
        assertEquals(WeatherCondition.UNKNOWN, forecast.current.weatherCondition)
    }
}
