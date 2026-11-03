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

    fun waterDoublesGrowth(): Boolean = false

    /** Верхняя половина блока занята: верхний полублок, в том числе в воде. */
    fun blocksUpperHalf(maxCollisionY: Double): Boolean = maxCollisionY > 0.5

    /**
     * Куст с возрастом готов к сбору, когда больше не растёт.
     * Стебель кукурузы и риса не готов никогда: плод стоит над ним.
     */
    fun ageCropIsReady(atMaxAge: Boolean, bonemealStillWorks: Boolean, supportStem: Boolean): Boolean =
        atMaxAge && !bonemealStillWorks && !supportStem

    /** Полив имеет смысл сухой пашне под культурой и голой пещерной лозе. */
    fun wantsWater(drySoilWithCrop: Boolean, bareCaveVine: Boolean): Boolean =
        drySoilWithCrop || bareCaveVine

    fun isPricklyPath(path: String): Boolean {
        val name = path.substringAfter(':')
        return name.contains("sweet_berry") || name.contains("prickly") ||
            name.contains("thorny") || name.contains("bramble")
    }
}
