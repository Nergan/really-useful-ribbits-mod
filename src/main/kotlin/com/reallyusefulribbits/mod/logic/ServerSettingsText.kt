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

    /** TOML NeoForge до 1.1.0 без раздела баллов: его хаос 25 тоже был значением по умолчанию. */
    fun migrateLegacyToml(text: String): String? {
        if ("[sorcerer.spells]" in text || !LEGACY_TOML_CHAOS.containsMatchIn(text)) return null
        return LEGACY_TOML_CHAOS.replace(text) { it.groupValues[1] + SorcererTable.DEFAULT_CHAOS }
    }

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
        val points = SorcererAction.entries.associateWith { action ->
            (values[SPELL_PREFIX + action.id]?.toIntOrNull() ?: action.defaultPoints).coerceIn(0, SorcererTable.MAX_POINTS)
        }
        return ServerSettings(radius, chaos, points)
    }

    fun complete(values: Map<String, String>): Boolean {
        if ("scan_radius" !in values || "chaos_level" !in values) return false
        return SorcererAction.entries.all { (SPELL_PREFIX + it.id) in values }
    }

    fun render(settings: ServerSettings): String = buildString {
        appendLine("# Really Useful Ribbits server settings. Restart the server after editing.")
        appendLine("# How far a ribbit looks for water, farms, containers, luck, endermen and sorcerer plant growth.")
        appendLine("scan_radius=${settings.scanRadius}")
        appendLine("# 0 keeps the spell points as they are; 100 makes every enabled spell equally likely.")
        appendLine("chaos_level=${settings.chaos}")
        appendLine("# Sorcerer spell points. A spell's chance is its points divided by the sum of all points.")
        appendLine("# 0 turns a spell off. The defaults add up to ${SorcererTable.DEFAULT_TOTAL}.")
        for (action in SorcererAction.entries) {
            appendLine("$SPELL_PREFIX${action.id}=${settings.points[action] ?: action.defaultPoints}")
        }
    }
}
