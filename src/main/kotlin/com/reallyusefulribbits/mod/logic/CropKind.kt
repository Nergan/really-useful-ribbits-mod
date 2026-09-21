package com.reallyusefulribbits.mod.logic

enum class CropKind {
    STANDARD,
    STEM_FRUIT,
    SUGAR_CANE,
    NETHER_WART,
    GLOW_BERRIES,
    TILLABLE,
    FARMLAND,
    OTHER,
}

object CropRules {
    fun shouldHarvestStemFruit(hasStemNeighbor: Boolean): Boolean = hasStemNeighbor

    fun caneHarvestYs(bottomY: Int, topY: Int): IntRange {
        if (topY <= bottomY) return IntRange.EMPTY
        return (bottomY + 1)..topY
    }

    fun canReachBerries(entityEyeY: Double, berryY: Double, reach: Double = 2.75): Boolean {
        return kotlin.math.abs(entityEyeY - (berryY + 0.5)) <= reach
    }

    fun waterDoublesGrowth(): Boolean = true
}
