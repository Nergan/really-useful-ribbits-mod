package com.reallyusefulribbits.mod.logic

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class ProfessionAndCropTest {

    @Test
    @DisplayName("Профессии Ribbits распознаются по id")
    fun professionIds() {
        assertEquals(ProfessionKind.FISHERMAN, ProfessionKind.fromId("fisherman"))
        assertEquals(ProfessionKind.FARMER, ProfessionKind.fromId("gardener"))
        assertEquals(ProfessionKind.MERCHANT, ProfessionKind.fromId("merchant"))
        assertEquals(ProfessionKind.SORCERER, ProfessionKind.fromId("sorcerer"))
        assertEquals(ProfessionKind.NITWIT, ProfessionKind.fromId("nitwit"))
        assertEquals(ProfessionKind.OTHER, ProfessionKind.fromId("unknown"))
    }

    @Test
    @DisplayName("Арбуз без стебля не собирают, тростник оставляют нижний блок")
    fun specialCrops() {
        assertFalse(CropRules.shouldHarvestStemFruit(false))
        assertTrue(CropRules.shouldHarvestStemFruit(true))
        assertEquals(5..7, CropRules.caneHarvestYs(4, 7))
        assertTrue(CropRules.caneHarvestYs(4, 4).isEmpty())
        assertTrue(CropRules.canReachBerries(1.5, 2.0))
        assertFalse(CropRules.canReachBerries(1.5, 10.0))
    }

    @Test
    @DisplayName("Фазы рыбалки идут wait → approach → bite → caught")
    fun fishingPhases() {
        val times = FishingPhaseTimes(10, 5, 3)
        assertEquals(FishingTiming.Phase.WAIT, FishingTiming.phase(3, times))
        assertEquals(FishingTiming.Phase.APPROACH, FishingTiming.phase(12, times))
        assertEquals(FishingTiming.Phase.BITE, FishingTiming.phase(16, times))
        assertEquals(FishingTiming.Phase.CAUGHT, FishingTiming.phase(20, times))
        assertEquals(0.4, FishingTiming.approachProgress(12, times), 1e-6)
    }

    @Test
    @DisplayName("Торговец чередует продажу и скупку за аметисты")
    fun merchantPlans() {
        var n = 0
        val random = { _: Int, _: Int -> n++ }
        val plans = MerchantEconomy.plan(
            inventoryIds = listOf("minecraft:cod", "minecraft:wheat"),
            copiedId = "minecraft:book",
            random = random,
            itemMax = { if (it == "minecraft:book") 16 else 64 },
        )
        assertEquals(MerchantEconomy.OFFER_COUNT, plans.size)
        assertTrue(plans.any { it.kind == MerchantOfferKind.SELL_FOR_AMETHYST })
        assertTrue(plans.any { it.kind == MerchantOfferKind.BUY_FOR_AMETHYST })
        assertTrue(plans.all { it.amethystCount in 1..32 })
    }
}
