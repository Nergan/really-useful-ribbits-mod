package com.reallyusefulribbits.mod.logic

data class FishingPhaseTimes(
    val waitTicks: Int,
    val approachTicks: Int,
    val biteWindowTicks: Int,
)

object FishingTiming {
    fun roll(random: (Int, Int) -> Int, lureLevel: Int = 0): FishingPhaseTimes {
        val lureCut = (lureLevel * 20).coerceAtLeast(0)
        val wait = (random(100, 601) - lureCut).coerceAtLeast(20)
        val approach = random(20, 41)
        val bite = random(8, 21)
        return FishingPhaseTimes(wait, approach, bite)
    }

    enum class Phase { WAIT, APPROACH, BITE, CAUGHT }

    fun phase(elapsed: Int, times: FishingPhaseTimes): Phase {
        if (elapsed < times.waitTicks) return Phase.WAIT
        if (elapsed < times.waitTicks + times.approachTicks) return Phase.APPROACH
        if (elapsed < times.waitTicks + times.approachTicks + times.biteWindowTicks) return Phase.BITE
        return Phase.CAUGHT
    }

    fun approachProgress(elapsed: Int, times: FishingPhaseTimes): Double {
        if (times.approachTicks <= 0) return 1.0
        val local = elapsed - times.waitTicks
        if (local <= 0) return 0.0
        if (local >= times.approachTicks) return 1.0
        return local.toDouble() / times.approachTicks
    }
}
