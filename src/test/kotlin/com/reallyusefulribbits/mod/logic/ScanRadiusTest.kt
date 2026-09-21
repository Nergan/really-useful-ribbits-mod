package com.reallyusefulribbits.mod.logic

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class ScanRadiusTest {

    @Test
    @DisplayName("Радиус режется в допустимые границы")
    fun clampKeepsLegalRange() {
        assertEquals(ScanRadius.MIN, ScanRadius.clamp(1))
        assertEquals(ScanRadius.MAX, ScanRadius.clamp(999))
        assertEquals(64, ScanRadius.clamp(64))
    }

    @Test
    @DisplayName("Точка на границе радиуса ещё считается своей")
    fun inclusiveSphere() {
        val origin = GridPos(0, 0, 0)
        assertTrue(ScanRadius.inRange(origin, GridPos(64, 0, 0), 64))
        assertFalse(ScanRadius.inRange(origin, GridPos(65, 0, 0), 64))
    }
}
