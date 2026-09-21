package com.reallyusefulribbits.mod.logic

object ScanRadius {
    const val DEFAULT = 64
    const val MIN = 8
    const val MAX = 128

    fun clamp(raw: Int): Int = raw.coerceIn(MIN, MAX)

    fun inRange(origin: GridPos, target: GridPos, radius: Int): Boolean {
        val r = clamp(radius)
        return origin.distSq(target) <= r * r
    }
}
