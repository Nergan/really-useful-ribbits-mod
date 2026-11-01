package com.reallyusefulribbits.mod.logic

/**
 * Заклинания чародея. [id] — ключ в серверном конфиге, [defaultPoints] — баллы по умолчанию.
 * Шанс заклинания — его баллы, делённые на сумму баллов всех заклинаний. 0 баллов выключает заклинание.
 */
enum class SorcererAction(val id: String, val defaultPoints: Int) {
    EFFECT_OR_CLEANSE("effect_or_cleanse", 956),
    ENCHANT("enchant", 1000),
    LAUNCH("launch", 500),
    GROW_PLANTS("grow_plants", 700),
    WEATHER("weather", 500),
    DAY_NIGHT("day_night", 450),
    SUMMON_MOBS("summon_mobs", 450),
    LIGHTNING("lightning", 350),
    FLIP("flip", 300),
    SCALE("scale", 350),
    MAX_HEALTH("max_health", 300),
    IGNITE_AREA("ignite_area", 250),
    STRUCTURE_TRIP("structure_trip", 200),
    SUMMON_PETS("summon_pets", 400),
    DIAMOND_RAIN("diamond_rain", 150),
    MORPH("morph", 100),
    SUMMON_GIANT_PHANTOM("giant_phantom", 70),
    RANDOM_DIMENSION("random_dimension", 80),
    SPECTATOR("spectator", 60),
    MEGA_EXPLOSION("mega_explosion", 50),
    CREATIVE_FLIGHT("creative_flight", 60),
    SUMMON_DRAGON("ender_dragon", 93),
    SUPER_SECRET_SETTINGS("super_secret_settings", 550),
    INVERTED_CONTROLS("inverted_controls", 400),
    VVVVVV("vvvvvv", 400),
    NICKNAME("nickname", 450),
    RANDOM_STRUCTURE("random_structure", 300),
    FIREBALLS("fireballs", 200),
    LIGHTNING_HANDS("lightning_hands", 150),
    GIANT_CAT("giant_cat", 120),
    BEDROCK_ROOM("bedrock_room", 25),
    FIREBALL_RAIN("fireball_rain", 20),
    LAVA_LAKE("lava_lake", 8),
    DELETE_WORLD("delete_world", 5),
    CORRUPT_WORLDGEN("corrupt_worldgen", 3),
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

    const val MAX_POINTS: Int = 1_000_000

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
