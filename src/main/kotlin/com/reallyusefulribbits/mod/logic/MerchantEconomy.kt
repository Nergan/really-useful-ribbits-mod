package com.reallyusefulribbits.mod.logic

enum class MerchantOfferKind {
    SELL_FOR_AMETHYST,
    BUY_FOR_AMETHYST,
}

data class MerchantOfferPlan(
    val kind: MerchantOfferKind,
    val itemId: String,
    val itemCount: Int,
    val amethystCount: Int,
)

object MerchantEconomy {
    const val INITIAL_EMERALDS = 256
    const val INITIAL_AMETHYSTS = 256
    const val RANDOM_STARTER_STACKS = 6
    const val OFFER_COUNT = 7
    const val QUACK_INTERVAL_TICKS = 18
    const val HARASS_TICKS = 15 * 20
    const val COOLDOWN_TICKS = 12 * 20

    fun priceFor(itemMaxStack: Int, random: (Int, Int) -> Int): Int {
        val base = when {
            itemMaxStack <= 1 -> random(8, 25)
            itemMaxStack <= 16 -> random(2, 9)
            else -> random(1, 5)
        }
        return base.coerceIn(1, 32)
    }

    fun itemCountFor(itemMaxStack: Int, random: (Int, Int) -> Int): Int {
        if (itemMaxStack <= 1) return 1
        return random(1, minOf(itemMaxStack, 16) + 1)
    }

    fun plan(
        inventoryIds: List<String>,
        copiedId: String?,
        random: (Int, Int) -> Int,
        itemMax: (String) -> Int,
    ): List<MerchantOfferPlan> {
        val pool = (inventoryIds + listOfNotNull(copiedId)).distinct()
        if (pool.isEmpty()) return emptyList()
        val offers = ArrayList<MerchantOfferPlan>(OFFER_COUNT)
        repeat(OFFER_COUNT) { index ->
            val id = pool[Math.floorMod(random(0, pool.size), pool.size)]
            val max = itemMax(id).coerceAtLeast(1)
            val kind = if (index % 2 == 0) {
                MerchantOfferKind.SELL_FOR_AMETHYST
            } else {
                MerchantOfferKind.BUY_FOR_AMETHYST
            }
            offers += MerchantOfferPlan(
                kind = kind,
                itemId = id,
                itemCount = itemCountFor(max, random),
                amethystCount = priceFor(max, random),
            )
        }
        return offers
    }
}

enum class MerchantPhase {
    INIT,
    SEEK_TRADER,
    BUILD_OFFERS,
    SEEK_PLAYER,
    HARASS,
    COOLDOWN,
}
