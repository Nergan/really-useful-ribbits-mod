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
    @DisplayName("Баллы по умолчанию сходятся в миллион: дракон 0,009%, лава 0,01%, варден и порча по 0,0001%")
    fun defaultPoints() {
        assertEquals(1_000_000, SorcererTable.DEFAULT_TOTAL)
        assertEquals(0.00009, SorcererTable.chance(SorcererAction.SUMMON_DRAGON), 1e-12)
        assertEquals(0.0001, SorcererTable.chance(SorcererAction.LAVA_LAKE), 1e-12)
        assertEquals(0.000001, SorcererTable.chance(SorcererAction.DELETE_WORLD), 1e-12)
        assertEquals(0.000001, SorcererTable.chance(SorcererAction.CORRUPT_WORLDGEN), 1e-12)
        assertEquals(SorcererAction.entries.size, SorcererAction.entries.map { it.id }.toSet().size)
    }

    @Test
    @DisplayName("Шанс по умолчанию в экране настроек пишется как в README")
    fun percentText() {
        assertEquals("15", SorcererTable.percentText(SorcererAction.ENCHANT.defaultPoints))
        assertEquals("12.12", SorcererTable.percentText(SorcererAction.EFFECT_OR_CLEANSE.defaultPoints))
        assertEquals("0.009", SorcererTable.percentText(SorcererAction.SUMMON_DRAGON.defaultPoints))
        assertEquals("0.0001", SorcererTable.percentText(SorcererAction.DELETE_WORLD.defaultPoints))
    }

    @Test
    @DisplayName("Разрушительные заклинания вместе реже процента, хорошие — самые частые")
    fun rarityOrder() {
        val fewest = SorcererAction.entries.minOf { it.defaultPoints }
        val rarest = SorcererAction.entries.filter { it.defaultPoints == fewest }.toSet()
        assertEquals(setOf(SorcererAction.DELETE_WORLD, SorcererAction.CORRUPT_WORLDGEN), rarest)
        val ruinous = listOf(
            SorcererAction.MEGA_EXPLOSION, SorcererAction.FIREBALL_RAIN, SorcererAction.LAVA_LAKE,
            SorcererAction.SUMMON_DRAGON, SorcererAction.DELETE_WORLD, SorcererAction.CORRUPT_WORLDGEN,
            SorcererAction.RANDOM_STRUCTURE, SorcererAction.IGNITE_AREA,
        )
        assertTrue(ruinous.sumOf { SorcererTable.chance(it) } < 0.01)
        assertTrue(SorcererAction.MEGA_EXPLOSION.defaultPoints < SorcererAction.LAVA_LAKE.defaultPoints)
        assertEquals(SorcererAction.ENCHANT, SorcererAction.entries.maxBy { it.defaultPoints })
        assertTrue(SorcererAction.SUMMON_PETS.defaultPoints > SorcererAction.SUMMON_MOBS.defaultPoints)
        assertTrue(SorcererAction.DIAMOND_RAIN.defaultPoints > SorcererAction.LIGHTNING.defaultPoints)
        assertTrue(SorcererAction.SUPER_SECRET_SETTINGS.defaultPoints > SorcererAction.INVERTED_CONTROLS.defaultPoints)
    }

    @Test
    @DisplayName("Бросок попадает в объявленные баллы, даже в один балл из миллиона")
    fun pickHonorsPoints() {
        assertEquals(SorcererAction.EFFECT_OR_CLEANSE, SorcererTable.pick(0.0))
        val table = SorcererTable.weights()
        val total = table.sumOf { it.weight }
        var start = 0.0
        for (entry in table) {
            assertEquals(entry.value, SorcererTable.pick((start + entry.weight / 2) / total))
            start += entry.weight
        }
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
