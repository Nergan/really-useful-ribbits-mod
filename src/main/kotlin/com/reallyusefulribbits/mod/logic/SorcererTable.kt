package com.reallyusefulribbits.mod.logic

enum class SorcererAction {
    EFFECT_OR_CLEANSE,
    ENCHANT,
    LAUNCH,
    GROW_PLANTS,
    WEATHER,
    DAY_NIGHT,
    SUMMON_MOBS,
    LIGHTNING,
    FLIP,
    SCALE,
    MAX_HEALTH,
    IGNITE_AREA,
    STRUCTURE_TRIP,
    SUMMON_PETS,
    DIAMOND_RAIN,
    MORPH,
    SUMMON_GIANT_PHANTOM,
    RANDOM_DIMENSION,
    SPECTATOR,
    MEGA_EXPLOSION,
    CREATIVE_FLIGHT,
    SUMMON_DRAGON,
}

data class Weighted<T>(val value: T, val weight: Int)

object SorcererTable {
    val WEIGHTS: List<Weighted<SorcererAction>> = listOf(
        Weighted(SorcererAction.EFFECT_OR_CLEANSE, 1592),
        Weighted(SorcererAction.ENCHANT, 1220),
        Weighted(SorcererAction.LAUNCH, 1040),
        Weighted(SorcererAction.GROW_PLANTS, 850),
        Weighted(SorcererAction.WEATHER, 850),
        Weighted(SorcererAction.DAY_NIGHT, 760),
        Weighted(SorcererAction.SUMMON_MOBS, 750),
        Weighted(SorcererAction.LIGHTNING, 600),
        Weighted(SorcererAction.FLIP, 500),
        Weighted(SorcererAction.SCALE, 500),
        Weighted(SorcererAction.IGNITE_AREA, 400),
        Weighted(SorcererAction.SUMMON_PETS, 250),
        Weighted(SorcererAction.MAX_HEALTH, 225),
        Weighted(SorcererAction.STRUCTURE_TRIP, 200),
        Weighted(SorcererAction.DIAMOND_RAIN, 100),
        Weighted(SorcererAction.MORPH, 18),
        Weighted(SorcererAction.SUMMON_GIANT_PHANTOM, 35),
        Weighted(SorcererAction.RANDOM_DIMENSION, 30),
        Weighted(SorcererAction.SPECTATOR, 25),
        Weighted(SorcererAction.MEGA_EXPLOSION, 25),
        Weighted(SorcererAction.CREATIVE_FLIGHT, 20),
        Weighted(SorcererAction.SUMMON_DRAGON, 10),
    )

    val TOTAL_WEIGHT: Int = WEIGHTS.sumOf { it.weight }

    fun blendedWeights(chaos: Int): List<Weighted<SorcererAction>> {
        val t = chaos.coerceIn(0, 100) / 100.0
        val average = TOTAL_WEIGHT.toDouble() / WEIGHTS.size
        return WEIGHTS.map { entry ->
            Weighted(entry.value, kotlin.math.round(entry.weight * (1.0 - t) + average * t).toInt().coerceAtLeast(1))
        }
    }

    fun pick(roll: Int, chaos: Int = 0, allowed: Collection<SorcererAction>? = null): SorcererAction {
        val table = blendedWeights(chaos).filter { allowed == null || it.value in allowed }
        if (table.isEmpty()) return SorcererAction.EFFECT_OR_CLEANSE
        val total = table.sumOf { it.weight }.coerceAtLeast(1)
        val wrapped = Math.floorMod(roll, total)
        var cursor = 0
        for (entry in table) {
            cursor += entry.weight
            if (wrapped < cursor) return entry.value
        }
        return table.last().value
    }

    fun chance(action: SorcererAction, chaos: Int = 0): Double {
        val table = blendedWeights(chaos)
        val total = table.sumOf { it.weight }.toDouble()
        val weight = table.first { it.value == action }.weight
        return weight / total
    }

    fun launchHeight(roll: Int): Int = 4 + Math.floorMod(roll, 13)

    fun maxHealth(roll: Int): Int = 1 + Math.floorMod(roll, 128)

    fun scaleSteps(roll: Int): Int = Math.floorMod(roll, 7) - 3

    fun scaleDelta(grow: Boolean, roll: Int): Int {
        val magnitude = 1 + Math.floorMod(roll, 3)
        return if (grow) magnitude else -magnitude
    }

    fun normalizeScaleBase(current: Double): Double = if (current <= 0.05) 1.0 else current

    fun applyScaleSteps(current: Double, steps: Int): Double {
        val next = normalizeScaleBase(current) + steps * 0.2
        return next.coerceIn(0.25, 3.0)
    }

    fun summonCount(roll: Int): Int = 1 + Math.floorMod(roll, 16)

    fun petCount(roll: Int): Int = 1 + Math.floorMod(roll, 8)

    fun phantomSize(roll: Int): Int = 8 + Math.floorMod(roll, 5)

    fun explosionPower(roll: Int): Float = 120f + 10f * Math.floorMod(roll, 5)

    fun hostileSummonChance(): Double = 0.18
}
