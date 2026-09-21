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
        assertTrue(SorcererTable.chance(SorcererAction.CREATIVE_FLIGHT) < 0.005)
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
        assertEquals(20, counts[SorcererAction.CREATIVE_FLIGHT])
    }

    @Test
    @DisplayName("Хаос выравнивает вероятности эффектов")
    fun chaosFlattensWeights() {
        val calm = SorcererTable.blendedWeights(0)
        val chaotic = SorcererTable.blendedWeights(100)
        val calmSpread = calm.maxOf { it.weight }.toDouble() / calm.minOf { it.weight }
        val chaosSpread = chaotic.maxOf { it.weight }.toDouble() / chaotic.minOf { it.weight }
        assertTrue(chaosSpread < 1.2)
        assertTrue(chaosSpread < calmSpread)
        assertEquals(SorcererAction.EFFECT_OR_CLEANSE, SorcererTable.pick(0, 0))
    }

    @Test
    @DisplayName("Выбор уважает список доступных эффектов")
    fun pickHonorsAllowed() {
        val allowed = setOf(SorcererAction.FLIP, SorcererAction.SCALE)
        repeat(50) { roll ->
            assertTrue(SorcererTable.pick(roll * 97, 0, allowed) in allowed)
        }
    }

    @Test
    @DisplayName("Подброс, здоровье и размер режутся в обещанные диапазоны")
    fun numericRanges() {
        assertTrue(SorcererTable.launchHeight(0) in 4..16)
        assertTrue(SorcererTable.maxHealth(0) in 1..128)
        assertEquals(0.25, SorcererTable.applyScaleSteps(0.3, -3), 1e-6)
        assertEquals(3.0, SorcererTable.applyScaleSteps(2.9, 3), 1e-6)
        assertEquals(1.4, SorcererTable.applyScaleSteps(1.0, 2), 1e-6)
        assertEquals(1.0, SorcererTable.normalizeScaleBase(0.0), 1e-6)
        assertTrue(SorcererTable.scaleDelta(true, 0) > 0)
        assertTrue(SorcererTable.scaleDelta(false, 0) < 0)
        assertTrue(SorcererTable.summonCount(0) in 1..16)
        assertTrue(SorcererTable.summonCount(15) in 1..16)
        assertTrue(SorcererTable.petCount(0) in 1..8)
        assertTrue(SorcererTable.petCount(7) in 1..8)
        assertTrue(SorcererTable.phantomSize(0) in 8..12)
        assertTrue(SorcererTable.explosionPower(0) in 12f..16f)
        assertTrue(SorcererTable.chance(SorcererAction.MORPH) < 0.01)
        assertTrue(SorcererTable.chance(SorcererAction.SPECTATOR) < 0.005)
        assertTrue(SorcererTable.chance(SorcererAction.MEGA_EXPLOSION) < 0.005)
        assertTrue(SorcererTable.chance(SorcererAction.SUMMON_GIANT_PHANTOM) < 0.005)
    }
}
