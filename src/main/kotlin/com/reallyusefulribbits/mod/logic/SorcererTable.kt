package com.reallyusefulribbits.mod.logic

enum class SorcererAction {
    EFFECT_OR_CLEANSE,
    ENCHANT,
    LAUNCH,
    GROW_PLANTS,
    WEATHER,
    DAY_NIGHT,
    SUMMON_PEACEFUL,
    LIGHTNING,
    FLIP,
    SCALE,
    MAX_HEALTH,
    DIAMOND_RAIN,
    RANDOM_DIMENSION,
    SUMMON_DRAGON,
}

data class Weighted<T>(val value: T, val weight: Int)

object SorcererTable {
    val WEIGHTS: List<Weighted<SorcererAction>> = listOf(
        Weighted(SorcererAction.EFFECT_OR_CLEANSE, 1800),
        Weighted(SorcererAction.ENCHANT, 1400),
        Weighted(SorcererAction.LAUNCH, 1200),
        Weighted(SorcererAction.GROW_PLANTS, 1000),
        Weighted(SorcererAction.WEATHER, 1000),
        Weighted(SorcererAction.DAY_NIGHT, 800),
        Weighted(SorcererAction.SUMMON_PEACEFUL, 800),
        Weighted(SorcererAction.LIGHTNING, 600),
        Weighted(SorcererAction.FLIP, 500),
        Weighted(SorcererAction.SCALE, 500),
        Weighted(SorcererAction.MAX_HEALTH, 250),
        Weighted(SorcererAction.DIAMOND_RAIN, 100),
        Weighted(SorcererAction.RANDOM_DIMENSION, 40),
        Weighted(SorcererAction.SUMMON_DRAGON, 10),
    )

    val TOTAL_WEIGHT: Int = WEIGHTS.sumOf { it.weight }

    fun pick(roll: Int): SorcererAction {
        val wrapped = Math.floorMod(roll, TOTAL_WEIGHT)
        var cursor = 0
        for (entry in WEIGHTS) {
            cursor += entry.weight
            if (wrapped < cursor) return entry.value
        }
        return SorcererAction.EFFECT_OR_CLEANSE
    }

    fun chance(action: SorcererAction): Double {
        val weight = WEIGHTS.first { it.value == action }.weight
        return weight.toDouble() / TOTAL_WEIGHT
    }

    fun launchHeight(roll: Int): Int = 4 + Math.floorMod(roll, 13)

    fun maxHealth(roll: Int): Int = 1 + Math.floorMod(roll, 128)

    fun scaleSteps(roll: Int): Int = Math.floorMod(roll, 7) - 3

    fun applyScaleSteps(current: Double, steps: Int): Double {
        val next = current + steps * 0.2
        return next.coerceIn(0.25, 3.0)
    }
}
