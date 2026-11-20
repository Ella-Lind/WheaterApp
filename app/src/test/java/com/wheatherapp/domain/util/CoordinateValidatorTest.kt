package com.wheatherapp.domain.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CoordinateValidatorTest {

    @Test
    fun isValidLatitude_validatesBounds() {
        assertTrue(CoordinateValidator.isValidLatitude(59.3293))
        assertTrue(CoordinateValidator.isValidLatitude(-90.0))
        assertTrue(CoordinateValidator.isValidLatitude(90.0))
        assertFalse(CoordinateValidator.isValidLatitude(90.1))
        assertFalse(CoordinateValidator.isValidLatitude(-91.0))
    }

    @Test
    fun isValidLongitude_validatesBounds() {
        assertTrue(CoordinateValidator.isValidLongitude(18.0686))
        assertTrue(CoordinateValidator.isValidLongitude(-180.0))
        assertTrue(CoordinateValidator.isValidLongitude(180.0))
        assertFalse(CoordinateValidator.isValidLongitude(180.1))
        assertFalse(CoordinateValidator.isValidLongitude(-181.0))
    }

    @Test
    fun parseCoordinates_validFormat_returnsPair() {
        val result = CoordinateValidator.parseCoordinates("59.3293, 18.0686")
        assertEquals(59.3293, result?.first ?: 0.0, 0.0001)
        assertEquals(18.0686, result?.second ?: 0.0, 0.0001)
    }

    @Test
    fun parseCoordinates_placeNameInput_returnsNull() {
        val result = CoordinateValidator.parseCoordinates("Stockholm")
        assertNull(result)
    }

    @Test
    fun parseCoordinates_outOfBoundsLatitude_returnsNull() {
        val result = CoordinateValidator.parseCoordinates("95.0, 18.0686")
        assertNull(result)
    }

    @Test
    fun parseCoordinates_outOfBoundsLongitude_returnsNull() {
        val result = CoordinateValidator.parseCoordinates("59.3293, 200.0")
        assertNull(result)
    }

    @Test
    fun parseCoordinates_invalidNumberFormat_returnsNull() {
        val result = CoordinateValidator.parseCoordinates("abc, xyz")
        assertNull(result)
    }

    @Test
    fun parseCoordinates_tooManyCommaSeparatedValues_returnsNull() {
        val result = CoordinateValidator.parseCoordinates("59.3293, 18.0686, 100.0")
        assertNull(result)
    }
}
