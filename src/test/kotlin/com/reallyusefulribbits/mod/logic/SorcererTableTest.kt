package com.reallyusefulribbits.mod.logic

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class SorcererTableTest {

    @Test
    @DisplayName("Веса сходятся в 10000, дождь из алмазов около 1%")
    fun weightsAreBalanced() {
        assertEquals(10000, SorcererTable.TOTAL_WEIGHT)
        assertEquals(0.01, SorcererTable.chance(SorcererAction.DIAMOND_RAIN), 1e-9)
        assertTrue(SorcererTable.chance(SorcererAction.SUMMON_DRAGON) < 0.002)
        assertTrue(SorcererTable.chance(SorcererAction.EFFECT_OR_CLEANSE) > 0.1)
    }

    @Test
    @DisplayName("Бросок попадает в объявленный вес")
    fun pickHonorsWeights() {
        assertEquals(SorcererAction.EFFECT_OR_CLEANSE, SorcererTable.pick(0))
        assertEquals(SorcererAction.SUMMON_DRAGON, SorcererTable.pick(9999))
        val counts = HashMap<SorcererAction, Int>()
        repeat(10000) { roll ->
            val action = SorcererTable.pick(roll)
            counts[action] = (counts[action] ?: 0) + 1
        }
        assertEquals(100, counts[SorcererAction.DIAMOND_RAIN])
        assertEquals(10, counts[SorcererAction.SUMMON_DRAGON])
    }

    @Test
    @DisplayName("Подброс и здоровье режутся в обещанные диапазоны")
    fun numericRanges() {
        assertTrue(SorcererTable.launchHeight(0) in 4..16)
        assertTrue(SorcererTable.maxHealth(0) in 1..128)
        assertEquals(0.25, SorcererTable.applyScaleSteps(0.3, -3), 1e-6)
        assertEquals(3.0, SorcererTable.applyScaleSteps(2.9, 3), 1e-6)
    }
}
