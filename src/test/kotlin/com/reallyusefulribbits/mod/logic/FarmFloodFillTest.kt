package com.reallyusefulribbits.mod.logic

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class FarmFloodFillTest {

    @Test
    @DisplayName("Связная пашня собирается, дырка режет участок")
    fun connectedFarmOnly() {
        val farm = setOf(
            GridPos(0, 0, 0),
            GridPos(1, 0, 0),
            GridPos(2, 0, 0),
            GridPos(4, 0, 0),
        )
        val filled = FarmFloodFill.fill(GridPos(0, 0, 0), 64) { it in farm }
        assertEquals(3, filled.size)
        assertTrue(GridPos(4, 0, 0) !in filled)
    }

    @Test
    @DisplayName("Блоки за радиусом не входят в ферму")
    fun respectsRadius() {
        val farm = (0..80).map { GridPos(it, 0, 0) }.toSet()
        val filled = FarmFloodFill.fill(GridPos(0, 0, 0), 8) { it in farm }
        assertTrue(filled.all { it.x <= 8 })
        assertEquals(9, filled.size)
    }

    @Test
    @DisplayName("Шахматная посадка тростника считается одним полем")
    fun checkerboardConnects() {
        val farm = setOf(
            GridPos(0, 0, 0),
            GridPos(1, 0, 1),
            GridPos(2, 0, 0),
            GridPos(3, 0, 1),
        )
        val filled = FarmFloodFill.fill(GridPos(0, 0, 0), 16) { it in farm }
        assertEquals(4, filled.size)
    }
}
