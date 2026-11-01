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
        assertFalse(ServerSettingsText.complete(values))
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
        assertTrue(ServerSettingsText.complete(values))
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
