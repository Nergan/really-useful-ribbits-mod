package com.reallyusefulribbits.mod.logic

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class ServerSettingsTextTest {

    @Test
    @DisplayName("Старый файл Fabric с хаосом 25 получает хаос 0 и все баллы")
    fun legacyFileMigrates() {
        val values = ServerSettingsText.parse(listOf("scan_radius=48", "chaos_level=25"))
        assertTrue(ServerSettingsText.needsRewrite(values))
        val settings = ServerSettingsText.resolve(values)
        assertEquals(48, settings.scanRadius)
        assertEquals(SorcererTable.DEFAULT_CHAOS, settings.chaos)
        assertEquals(SorcererAction.SUMMON_DRAGON.defaultPoints, settings.points[SorcererAction.SUMMON_DRAGON])
    }

    @Test
    @DisplayName("Свой хаос и баллы переживают перезапись файла")
    fun customValuesSurvive() {
        val written = ServerSettingsText.render(
            ServerSettingsText.resolve(
                ServerSettingsText.parse(listOf("# comment", "scan_radius=64", "chaos_level=40", "spell.ender_dragon=0")),
            ),
        )
        val values = ServerSettingsText.parse(written.lines())
        assertFalse(ServerSettingsText.needsRewrite(values))
        val settings = ServerSettingsText.resolve(values)
        assertEquals(40, settings.chaos)
        assertEquals(0, settings.points[SorcererAction.SUMMON_DRAGON])
        assertEquals(SorcererAction.ENCHANT.defaultPoints, settings.points[SorcererAction.ENCHANT])
    }

    @Test
    @DisplayName("Старый TOML NeoForge с хаосом 25 получает хаос 0, новый и свой не трогаются")
    fun legacyTomlMigrates() {
        val legacy = "[sorcerer]\r\n\t# Default: 25\r\n\tchaos_level = 25\r\n"
        val migrated = ServerSettingsText.migrateLegacyToml(legacy)
        assertEquals("[sorcerer]\r\n\t# Default: 25\r\n\tchaos_level = 0\r\n", migrated)
        assertNull(ServerSettingsText.migrateLegacyToml("[sorcerer]\n\tchaos_level = 40\n"))
        assertNull(ServerSettingsText.migrateLegacyToml("[sorcerer]\n\tchaos_level = 25\n\n\t[sorcerer.spells]\n\t\tenchant = 1000\n"))
    }

    @Test
    @DisplayName("Нетронутые баллы 1.1.0 в файле Fabric уступают новым, правленые остаются")
    fun points110MoveOnFabric() {
        val old = listOf("scan_radius=64", "chaos_level=0") + POINTS_1_1_0.map { (id, points) -> "spell.$id=$points" }
        val untouched = ServerSettingsText.parse(old)
        assertTrue(ServerSettingsText.needsRewrite(untouched))
        assertEquals(SorcererAction.SUMMON_DRAGON.defaultPoints, ServerSettingsText.resolve(untouched).points[SorcererAction.SUMMON_DRAGON])
        val tuned = ServerSettingsText.parse(old.map { if (it == "spell.enchant=1000") "spell.enchant=2000" else it })
        assertFalse(ServerSettingsText.needsRewrite(tuned))
        assertEquals(93, ServerSettingsText.resolve(tuned).points[SorcererAction.SUMMON_DRAGON])
        assertEquals(2000, ServerSettingsText.resolve(tuned).points[SorcererAction.ENCHANT])
    }

    @Test
    @DisplayName("Нетронутые баллы 1.1.0 в TOML NeoForge уступают новым вместе со строкой Default")
    fun points110MoveOnNeoForge() {
        val old = buildString {
            append("[sorcerer]\r\n\tchaos_level = 0\r\n\r\n\t[sorcerer.spells]\r\n")
            for ((id, points) in POINTS_1_1_0) {
                append("\t\t# Default: $points\r\n\t\t# Range: 0 ~ 1000000\r\n\t\t$id = $points\r\n")
            }
        }
        val migrated = ServerSettingsText.migrateLegacyToml(old)!!
        val dragon = SorcererAction.SUMMON_DRAGON.defaultPoints
        assertTrue("\t\t# Default: $dragon\r\n\t\t# Range: 0 ~ 1000000\r\n\t\tender_dragon = $dragon\r\n" in migrated)
        assertTrue("\t\tlightning = ${SorcererAction.LIGHTNING.defaultPoints}\r\n" in migrated)
        assertTrue("\t\tlightning_hands = ${SorcererAction.LIGHTNING_HANDS.defaultPoints}\r\n" in migrated)
        assertTrue("chaos_level = 0\r\n" in migrated)
        assertNull(ServerSettingsText.migrateLegacyToml(migrated))
        assertNull(ServerSettingsText.migrateLegacyToml(old.replace("\t\tenchant = 1000\r\n", "\t\tenchant = 1200\r\n")))
    }

    @Test
    @DisplayName("Комната из бедрока — куб 16 с оболочкой в один блок")
    fun roomShape() {
        val room = RoomShape.around(100, 64, -20)
        var shell = 0
        var inside = 0
        room.forEach { x, y, z -> if (room.isShell(x, y, z)) shell++ else inside++ }
        assertEquals(16 * 16 * 16 - 14 * 14 * 14, shell)
        assertEquals(14 * 14 * 14, inside)
        assertEquals(63, room.minY)
        assertFalse(room.isShell(100, 64, -20))
    }
}

/** Баллы, которые 1.1.0 записал в конфиг по умолчанию. */
private val POINTS_1_1_0 = linkedMapOf(
    "effect_or_cleanse" to 956, "enchant" to 1000, "launch" to 500, "grow_plants" to 700,
    "weather" to 500, "day_night" to 450, "summon_mobs" to 450, "lightning" to 350,
    "flip" to 300, "scale" to 350, "max_health" to 300, "ignite_area" to 250,
    "structure_trip" to 200, "summon_pets" to 400, "diamond_rain" to 150, "morph" to 100,
    "giant_phantom" to 70, "random_dimension" to 80, "spectator" to 60, "mega_explosion" to 50,
    "creative_flight" to 60, "ender_dragon" to 93, "super_secret_settings" to 550, "inverted_controls" to 400,
    "vvvvvv" to 400, "nickname" to 450, "random_structure" to 300, "fireballs" to 200,
    "lightning_hands" to 150, "giant_cat" to 120, "bedrock_room" to 25, "fireball_rain" to 20,
    "lava_lake" to 8, "delete_world" to 5, "corrupt_worldgen" to 3,
)
