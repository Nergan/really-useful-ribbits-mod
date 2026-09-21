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
    const val SLOT_COUNT = 16
    const val QUARTER_SIZE = 4
    const val OFFER_COUNT = SLOT_COUNT
    const val QUACK_INTERVAL_TICKS = 18
    const val HARASS_TICKS = 15 * 20
    const val COOLDOWN_TICKS = 12 * 20
    const val MAX_COPIED_TRADES = 48

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

    fun quarterStart(quarter: Int): Int = Math.floorMod(quarter, 4) * QUARTER_SIZE

    fun averagePrice(prices: List<Int>, fallback: Int): Int {
        if (prices.isEmpty()) return fallback.coerceIn(1, 32)
        return (prices.sum().toDouble() / prices.size).toInt().coerceIn(1, 32)
    }

    fun plan(
        inventoryIds: List<String>,
        copiedId: String?,
        random: (Int, Int) -> Int,
        itemMax: (String) -> Int,
        visits: List<Pair<String, Int>> = emptyList(),
        count: Int = OFFER_COUNT,
    ): List<MerchantOfferPlan> {
        val visitPool = visits.filter { it.first.isNotBlank() }
        val fallback = (inventoryIds + listOfNotNull(copiedId)).filter { it.isNotBlank() }.distinct()
        if (visitPool.isEmpty() && fallback.isEmpty()) return emptyList()
        val avg = averagePrice(visitPool.map { it.second }, 4)
        val unique = LinkedHashMap<String, Int>()
        for ((id, price) in visitPool) {
            unique.putIfAbsent(id, price)
        }
        for (id in fallback) {
            unique.putIfAbsent(id, avg)
        }
        val offers = ArrayList<MerchantOfferPlan>(count)
        for (pick in unique.entries.take(count)) {
            val max = itemMax(pick.key).coerceAtLeast(1)
            val itemPrices = visitPool.filter { it.first == pick.key }.map { it.second }
            offers += MerchantOfferPlan(
                kind = MerchantOfferKind.SELL_FOR_AMETHYST,
                itemId = pick.key,
                itemCount = itemCountFor(max, random),
                amethystCount = averagePrice(itemPrices, pick.value.coerceAtLeast(1)),
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
