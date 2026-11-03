package com.reallyusefulribbits.mod.logic

/**
 * Заклинания чародея. [id] — ключ в серверном конфиге, [defaultPoints] — баллы по умолчанию.
 * Шанс заклинания — его баллы, делённые на сумму баллов всех заклинаний. 0 баллов выключает заклинание.
 * Сумма по умолчанию — 9 999 092. Один балл — это 0,00001%.
 * Дракон и гигантский варден стоят на одном балле: около одного броска на десять миллионов.
 */
enum class SorcererAction(val id: String, val defaultPoints: Int) {
    EFFECT_OR_CLEANSE("effect_or_cleanse", 761_780),
    ENCHANT("enchant", 1_500_000),
    LAUNCH("launch", 300_000),
    GROW_PLANTS("grow_plants", 1_000_000),
    WEATHER("weather", 500_000),
    DAY_NIGHT("day_night", 450_000),
    SUMMON_MOBS("summon_mobs", 250_000),
    LIGHTNING("lightning", 100_000),
    FLIP("flip", 300_000),
    SCALE("scale", 350_000),
    MAX_HEALTH("max_health", 300_000),
    IGNITE_AREA("ignite_area", 20_000),
    STRUCTURE_TRIP("structure_trip", 200_000),
    SUMMON_PETS("summon_pets", 650_000),
    DIAMOND_RAIN("diamond_rain", 300_000),
    APPLE_RAIN("apple_rain", 250_000),
    PLAYER_STATUE("player_statue", 200_000),
    MORPH("morph", 100_000),
    SUMMON_GIANT_PHANTOM("giant_phantom", 20_000),
    RANDOM_DIMENSION("random_dimension", 30_000),
    SPECTATOR("spectator", 60_000),
    MEGA_EXPLOSION("mega_explosion", 500),
    CREATIVE_FLIGHT("creative_flight", 120_000),
    SUMMON_DRAGON("ender_dragon", 1),
    SUPER_SECRET_SETTINGS("super_secret_settings", 550_000),
    INVERTED_CONTROLS("inverted_controls", 350_000),
    VVVVVV("vvvvvv", 350_000),
    NICKNAME("nickname", 450_000),
    RANDOM_STRUCTURE("random_structure", 30_000),
    FIREBALLS("fireballs", 200_000),
    LIGHTNING_HANDS("lightning_hands", 150_000),
    GIANT_CAT("giant_cat", 150_000),
    BEDROCK_ROOM("bedrock_room", 5_000),
    FIREBALL_RAIN("fireball_rain", 800),
    LAVA_LAKE("lava_lake", 1_000),
    DELETE_WORLD("delete_world", 1),
    CORRUPT_WORLDGEN("corrupt_worldgen", 10),
    ;

    companion object {
        fun byId(id: String): SorcererAction? = entries.firstOrNull { it.id == id }
    }
}

data class Weighted<T>(val value: T, val weight: Double)

object SorcererTable {
    const val DEFAULT_CHAOS: Int = 0

    /** Хаос по умолчанию до 1.1.0. Старый файл Fabric с этим числом получает новое значение по умолчанию. */
    const val LEGACY_DEFAULT_CHAOS: Int = 25

    const val MAX_POINTS: Int = 10_000_000

    val DEFAULT_TOTAL: Int = SorcererAction.entries.sumOf { it.defaultPoints }

    /**
     * Хаос выравнивает шансы степенью: вес — баллы в степени `1 - хаос/100`.
     * При 0 шанс пропорционален баллам, при 100 все включённые заклинания равны,
     * а порядок от частых к редким сохраняется на любом уровне.
     */
    fun weights(
        chaos: Int = DEFAULT_CHAOS,
        allowed: Collection<SorcererAction>? = null,
        points: (SorcererAction) -> Int = { it.defaultPoints },
    ): List<Weighted<SorcererAction>> {
        val exponent = 1.0 - chaos.coerceIn(0, 100) / 100.0
        return SorcererAction.entries.mapNotNull { action ->
            if (allowed != null && action !in allowed) return@mapNotNull null
            val raw = points(action).coerceIn(0, MAX_POINTS)
            if (raw <= 0) return@mapNotNull null
            Weighted(action, Math.pow(raw.toDouble(), exponent))
        }
    }

    /** [roll] — число из [0, 1). Null, если ни одно заклинание не включено. */
    fun pick(
        roll: Double,
        chaos: Int = DEFAULT_CHAOS,
        allowed: Collection<SorcererAction>? = null,
        points: (SorcererAction) -> Int = { it.defaultPoints },
    ): SorcererAction? {
        val table = weights(chaos, allowed, points)
        if (table.isEmpty()) return null
        val total = table.sumOf { it.weight }
        var cursor = roll.coerceIn(0.0, Math.nextDown(1.0)) * total
        for (entry in table) {
            cursor -= entry.weight
            if (cursor < 0.0) return entry.value
        }
        return table.last().value
    }

    fun chance(
        action: SorcererAction,
        chaos: Int = DEFAULT_CHAOS,
        points: (SorcererAction) -> Int = { it.defaultPoints },
    ): Double {
        val table = weights(chaos, null, points)
        val total = table.sumOf { it.weight }
        if (total <= 0.0) return 0.0
        return (table.firstOrNull { it.value == action }?.weight ?: 0.0) / total
    }

    /** Доля баллов в процентах без лишних нулей: 15, 12.12, 0.0001, 0.00001. */
    fun percentText(points: Int, total: Int = DEFAULT_TOTAL): String {
        val percent = points * 100.0 / total
        val pattern = when {
            percent >= 1.0 -> "%.2f"
            percent >= 0.0001 -> "%.4f"
            else -> "%.5f"
        }
        val text = String.format(java.util.Locale.ROOT, pattern, percent)
        return text.trimEnd('0').trimEnd('.')
    }

    fun launchHeight(roll: Int): Int = 4 + Math.floorMod(roll, 13)

    fun maxHealth(roll: Int): Int = 1 + Math.floorMod(roll, 128)

    const val MIN_SCALE: Double = 0.25
    const val MAX_SCALE: Double = 8.0
    const val GROW_CHANCE: Double = 0.6

    fun normalizeScaleBase(current: Double): Double = if (current <= 0.05) 1.0 else current

    /** Рост — в 1,3–2,5 раза, уменьшение — в 0,45–0,8 раза. [roll] — число из [0, 1). */
    fun scaleFactor(grow: Boolean, roll: Double): Double {
        val t = roll.coerceIn(0.0, 1.0)
        return if (grow) 1.3 + 1.2 * t else 0.45 + 0.35 * t
    }

    fun applyScale(current: Double, factor: Double): Double =
        (normalizeScaleBase(current) * factor).coerceIn(MIN_SCALE, MAX_SCALE)

    fun summonCount(roll: Int): Int = 1 + Math.floorMod(roll, 16)

    fun petCount(roll: Int): Int = 1 + Math.floorMod(roll, 8)

    fun phantomSize(roll: Int): Int = 8 + Math.floorMod(roll, 5)

    fun explosionPower(roll: Int): Float = 120f + 10f * Math.floorMod(roll, 5)

    fun hostileSummonChance(): Double = 0.18
}
