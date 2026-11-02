package com.reallyusefulribbits.mod.logic

data class ServerSettings(
    val scanRadius: Int,
    val chaos: Int,
    val points: Map<SorcererAction, Int>,
)

/** Файл `reallyusefulribbits-server.properties` для Fabric: разбор, старые значения и запись. */
object ServerSettingsText {
    private const val SPELL_PREFIX = "spell."
    private val LEGACY_TOML_CHAOS = Regex("""(?m)^(\s*chaos_level\s*=\s*)${SorcererTable.LEGACY_DEFAULT_CHAOS}(?=[ \t]*\r?$)""")

    /**
     * Баллы по умолчанию из 1.1.0, сумма 10000. Файл, где все баллы ровно такие
     * или ровно как в 1.1.2, их не выбирал: он получает новые значения целиком. Правленый остаётся.
     */
    private val POINTS_1_1_0: Map<String, Int> = mapOf(
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

    /** Баллы по умолчанию из 1.1.2, сумма миллион. Нетронутый файл получает текущую таблицу. */
    private val POINTS_1_1_2: Map<String, Int> = mapOf(
        "effect_or_cleanse" to 121_178, "enchant" to 150_000, "launch" to 30_000, "grow_plants" to 100_000,
        "weather" to 50_000, "day_night" to 45_000, "summon_mobs" to 25_000, "lightning" to 10_000,
        "flip" to 30_000, "scale" to 35_000, "max_health" to 30_000, "ignite_area" to 2_000,
        "structure_trip" to 20_000, "summon_pets" to 65_000, "diamond_rain" to 30_000, "morph" to 10_000,
        "giant_phantom" to 2_000, "random_dimension" to 3_000, "spectator" to 6_000, "mega_explosion" to 50,
        "creative_flight" to 12_000, "ender_dragon" to 90, "super_secret_settings" to 55_000, "inverted_controls" to 35_000,
        "vvvvvv" to 35_000, "nickname" to 45_000, "random_structure" to 3_000, "fireballs" to 20_000,
        "lightning_hands" to 15_000, "giant_cat" to 15_000, "bedrock_room" to 500, "fireball_rain" to 80,
        "lava_lake" to 100, "delete_world" to 1, "corrupt_worldgen" to 1,
    )

    /**
     * TOML NeoForge: без раздела баллов (до 1.1.0) хаос 25 был значением по умолчанию,
     * а раздел с нетронутыми баллами 1.1.0 или 1.1.2 получает новые баллы вместе со строками `# Default:`.
     */
    fun migrateLegacyToml(text: String): String? {
        if ("[sorcerer.spells]" !in text) {
            if (!LEGACY_TOML_CHAOS.containsMatchIn(text)) return null
            return LEGACY_TOML_CHAOS.replace(text) { it.groupValues[1] + SorcererTable.DEFAULT_CHAOS }
        }
        val untouched = listOf(POINTS_1_1_0, POINTS_1_1_2).any { table ->
            SorcererAction.entries.all { action ->
                tomlEntry(action.id).find(text)?.groupValues?.get(2)?.toIntOrNull() == table[action.id]
            }
        }
        if (!untouched) return null
        var migrated = text
        for (action in SorcererAction.entries) {
            migrated = tomlDefaultComment(action.id).replace(migrated) { it.groupValues[1] + action.defaultPoints }
            migrated = tomlEntry(action.id).replace(migrated) { it.groupValues[1] + action.defaultPoints }
        }
        return migrated.replace("0 ~ 1000000", "0 ~ ${SorcererTable.MAX_POINTS}")
    }

    private fun tomlEntry(id: String) = Regex("""(?m)^(\s*${Regex.escape(id)}\s*=\s*)(\d+)(?=[ \t]*\r?$)""")

    private fun tomlDefaultComment(id: String) =
        Regex("""(?m)^(\s*# Default: )\d+(?=\r?\n\s*# Range: [^\r\n]*\r?\n\s*${Regex.escape(id)}\s*=)""")

    fun parse(lines: List<String>): Map<String, String> {
        val values = LinkedHashMap<String, String>()
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("#")) continue
            val parts = trimmed.split("=", limit = 2)
            if (parts.size == 2) values[parts[0].trim()] = parts[1].trim()
        }
        return values
    }

    /** Файл до 1.1.0 не знает баллов. Его хаос 25 был значением по умолчанию, а не выбором. */
    fun resolve(values: Map<String, String>): ServerSettings {
        val legacy = values.keys.none { it.startsWith(SPELL_PREFIX) }
        val radius = ScanRadius.clamp(values["scan_radius"]?.toIntOrNull() ?: ScanRadius.DEFAULT)
        var chaos = values["chaos_level"]?.toIntOrNull()?.coerceIn(0, 100) ?: SorcererTable.DEFAULT_CHAOS
        if (legacy && chaos == SorcererTable.LEGACY_DEFAULT_CHAOS) chaos = SorcererTable.DEFAULT_CHAOS
        val stale = holdsStaleDefaults(values)
        val points = SorcererAction.entries.associateWith { action ->
            val stored = if (stale) null else values[SPELL_PREFIX + action.id]?.toIntOrNull()
            (stored ?: action.defaultPoints).coerceIn(0, SorcererTable.MAX_POINTS)
        }
        return ServerSettings(radius, chaos, points)
    }

    /** Файл без части ключей или с нетронутыми баллами 1.1.0 либо 1.1.2 переписывается полным. */
    fun needsRewrite(values: Map<String, String>): Boolean {
        if ("scan_radius" !in values || "chaos_level" !in values) return true
        if (SorcererAction.entries.any { (SPELL_PREFIX + it.id) !in values }) return true
        return holdsStaleDefaults(values)
    }

    private fun holdsStaleDefaults(values: Map<String, String>): Boolean =
        holdsTable(values, POINTS_1_1_0) || holdsTable(values, POINTS_1_1_2)

    private fun holdsTable(values: Map<String, String>, table: Map<String, Int>): Boolean =
        SorcererAction.entries.all { action ->
            values[SPELL_PREFIX + action.id]?.toIntOrNull() == table[action.id]
        }

    fun render(settings: ServerSettings): String = buildString {
        appendLine("# Really Useful Ribbits server settings. Restart the server after editing.")
        appendLine("# How far a ribbit looks for water, farms, containers, luck, endermen and sorcerer plant growth.")
        appendLine("scan_radius=${settings.scanRadius}")
        appendLine("# 0 keeps the spell points as they are; 100 makes every enabled spell equally likely.")
        appendLine("chaos_level=${settings.chaos}")
        appendLine("# Sorcerer spell points. A spell's chance is its points divided by the sum of all points.")
        appendLine("# 0 turns a spell off. The defaults add up to ${SorcererTable.DEFAULT_TOTAL}; with them 1 point is a 0.00001% chance.")
        for (action in SorcererAction.entries) {
            appendLine("$SPELL_PREFIX${action.id}=${settings.points[action] ?: action.defaultPoints}")
        }
    }
}
