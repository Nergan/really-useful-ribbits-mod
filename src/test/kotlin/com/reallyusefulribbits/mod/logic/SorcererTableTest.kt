package com.reallyusefulribbits.mod.logic

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class SorcererTableTest {

    @Test
    @DisplayName("Баллы по умолчанию сходятся в 10000, дракон около 1/108")
    fun defaultPoints() {
        assertEquals(10000, SorcererTable.DEFAULT_TOTAL)
        assertEquals(1.0 / 108.0, SorcererTable.chance(SorcererAction.SUMMON_DRAGON), 0.0005)
        assertEquals(0.015, SorcererTable.chance(SorcererAction.DIAMOND_RAIN), 1e-9)
        assertEquals(SorcererAction.entries.size, SorcererAction.entries.map { it.id }.toSet().size)
    }

    @Test
    @DisplayName("Порча генерации — самое редкое, хорошие заклинания чаще бедствий")
    fun rarityOrder() {
        val rarest = SorcererAction.entries.minBy { it.defaultPoints }
        assertEquals(SorcererAction.CORRUPT_WORLDGEN, rarest)
        assertTrue(SorcererAction.LAVA_LAKE.defaultPoints < SorcererAction.FIREBALL_RAIN.defaultPoints)
        assertTrue(SorcererAction.DELETE_WORLD.defaultPoints < SorcererAction.BEDROCK_ROOM.defaultPoints)
        assertTrue(SorcererAction.CREATIVE_FLIGHT.defaultPoints < SorcererAction.VVVVVV.defaultPoints)
        assertTrue(SorcererAction.SUPER_SECRET_SETTINGS.defaultPoints > SorcererAction.INVERTED_CONTROLS.defaultPoints)
        assertTrue(SorcererAction.ENCHANT.defaultPoints > SorcererAction.LAUNCH.defaultPoints)
    }

    @Test
    @DisplayName("Бросок попадает в объявленные баллы")
    fun pickHonorsPoints() {
        assertEquals(SorcererAction.EFFECT_OR_CLEANSE, SorcererTable.pick(0.0))
        assertEquals(SorcererAction.CORRUPT_WORLDGEN, SorcererTable.pick(0.99999))
        val counts = HashMap<SorcererAction, Int>()
        repeat(10000) { index ->
            val action = SorcererTable.pick((index + 0.5) / 10000.0) ?: return@repeat
            counts[action] = (counts[action] ?: 0) + 1
        }
        assertEquals(150, counts[SorcererAction.DIAMOND_RAIN])
        assertEquals(93, counts[SorcererAction.SUMMON_DRAGON])
        assertEquals(3, counts[SorcererAction.CORRUPT_WORLDGEN])
    }

    @Test
    @DisplayName("Хаос выравнивает шансы и не меняет порядок")
    fun chaosFlattensWeights() {
        val calm = SorcererTable.weights(0)
        val chaotic = SorcererTable.weights(100)
        assertTrue(chaotic.maxOf { it.weight } / chaotic.minOf { it.weight } < 1.0001)
        val half = SorcererTable.weights(50).associate { it.value to it.weight }
        assertTrue(half.getValue(SorcererAction.ENCHANT) > half.getValue(SorcererAction.SUMMON_DRAGON))
        assertTrue(half.getValue(SorcererAction.SUMMON_DRAGON) > half.getValue(SorcererAction.CORRUPT_WORLDGEN))
        assertTrue(SorcererTable.chance(SorcererAction.CORRUPT_WORLDGEN, 25) < 0.002)
        assertEquals(calm.size, chaotic.size)
    }

    @Test
    @DisplayName("Ноль баллов выключает заклинание даже при полном хаосе")
    fun zeroPointsDisable() {
        val points = { action: SorcererAction -> if (action == SorcererAction.SUMMON_DRAGON) 0 else action.defaultPoints }
        assertEquals(0.0, SorcererTable.chance(SorcererAction.SUMMON_DRAGON, 100, points), 0.0)
        assertNull(SorcererTable.pick(0.5, 0, emptySet()))
        assertNotNull(SorcererTable.pick(0.5, 0, setOf(SorcererAction.FLIP), points))
    }

    @Test
    @DisplayName("Выбор уважает список доступных заклинаний")
    fun pickHonorsAllowed() {
        val allowed = setOf(SorcererAction.FLIP, SorcererAction.SCALE)
        repeat(50) { roll ->
            assertTrue(SorcererTable.pick(roll / 50.0, 0, allowed) in allowed)
        }
    }

    @Test
    @DisplayName("Размер растёт чаще и сильнее, чем уменьшается")
    fun scaleReachesHigher() {
        assertTrue(SorcererTable.GROW_CHANCE > 0.5)
        assertEquals(1.3, SorcererTable.scaleFactor(true, 0.0), 1e-9)
        assertEquals(2.5, SorcererTable.scaleFactor(true, 1.0), 1e-9)
        assertTrue(SorcererTable.scaleFactor(false, 1.0) < 1.0)
        assertEquals(SorcererTable.MAX_SCALE, SorcererTable.applyScale(7.0, 2.5), 1e-9)
        assertEquals(SorcererTable.MIN_SCALE, SorcererTable.applyScale(0.3, 0.45), 1e-9)
        assertEquals(1.3, SorcererTable.applyScale(0.0, 1.3), 1e-9)
        assertTrue(SorcererTable.MAX_SCALE > 3.0)
    }

    @Test
    @DisplayName("Подброс, здоровье и призыв режутся в обещанные диапазоны")
    fun numericRanges() {
        assertTrue(SorcererTable.launchHeight(0) in 4..16)
        assertTrue(SorcererTable.maxHealth(0) in 1..128)
        assertTrue(SorcererTable.summonCount(0) in 1..16)
        assertTrue(SorcererTable.summonCount(15) in 1..16)
        assertTrue(SorcererTable.petCount(0) in 1..8)
        assertTrue(SorcererTable.petCount(7) in 1..8)
        assertTrue(SorcererTable.phantomSize(0) in 8..12)
        assertTrue(SorcererTable.explosionPower(0) in 120f..160f)
    }

    @Test
    @DisplayName("Ник и шейдер не повторяют текущий")
    fun namesAndShadersChange() {
        repeat(30) { roll ->
            val name = SorcererNames.pick(roll, "Батон")
            assertFalse(name == "Батон")
            val shader = SuperSecretShaders.pick(roll, SuperSecretShaders.SHADERS.first())
            assertFalse(shader == SuperSecretShaders.SHADERS.first())
        }
        assertEquals(-1, SorcererNames.color(0.2, 5))
        assertEquals(5, SorcererNames.color(0.8, 5))
    }
}
