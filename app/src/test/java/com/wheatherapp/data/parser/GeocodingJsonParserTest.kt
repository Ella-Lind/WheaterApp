package com.wheatherapp.data.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GeocodingJsonParserTest {

    private val parser = GeocodingJsonParser()

    @Test
    fun parse_validJson_returnsLocationList() {
        val json = """
        {
          "results": [
            {
              "name": "Stockholm",
              "latitude": 59.3293,
              "longitude": 18.0686,
              "country": "Sweden"
            }
          ]
        }
        """.trimIndent()

        val results = parser.parse(json)
        assertEquals(1, results.size)
        assertEquals("Stockholm", results[0].name)
        assertEquals("Sweden", results[0].country)
        assertEquals(59.3293, results[0].latitude, 0.0001)
        assertEquals(18.0686, results[0].longitude, 0.0001)
    }

    @Test
    fun parse_emptyResults_returnsEmptyList() {
        val json = """{ "results": [] }"""
        val results = parser.parse(json)
        assertTrue(results.isEmpty())
    }

    @Test
    fun parse_nullResults_returnsEmptyList() {
        val json = """{ "generationtime_ms": 0.5 }"""
        val results = parser.parse(json)
        assertTrue(results.isEmpty())
    }

    @Test
    fun parse_missingOptionalFields_handlesNullCountry() {
        val json = """
        {
          "results": [
            {
              "name": "Unknown Island",
              "latitude": 10.0,
              "longitude": 20.0
            }
          ]
        }
        """.trimIndent()

        val results = parser.parse(json)
        assertEquals(1, results.size)
        assertEquals("Unknown Island", results[0].name)
        assertNull(results[0].country)
    }

    @Test(expected = Exception::class)
    fun parse_malformedJson_throwsException() {
        val malformedJson = "{ invalid_json_syntax }"
        parser.parse(malformedJson)
    }
}
