package com.reallyusefulribbits.mod.logic

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class FarmerTaskPlannerTest {

    @Test
    @DisplayName("Полный инвентарь важнее урожая")
    fun depositBeatsHarvest() {
        val view = FarmerWorldView(
            inventoryFull = true,
            inventoryHasItems = true,
            hasProduce = true,
            hasMatureCrop = true,
            hasTillable = true,
            hasEmptyFarmland = true,
            hasPlantable = true,
            hasImmatureCrop = true,
        )
        assertEquals(FarmerTask.DEPOSIT, FarmerTaskPlanner.next(view))
    }

    @Test
    @DisplayName("Посадка важнее вспашки, чтобы поле не расползалось")
    fun plantBeatsTill() {
        val view = FarmerWorldView(
            inventoryFull = false,
            inventoryHasItems = false,
            hasProduce = false,
            hasMatureCrop = false,
            hasTillable = true,
            hasEmptyFarmland = true,
            hasPlantable = true,
            hasImmatureCrop = true,
        )
        assertEquals(FarmerTask.PLANT, FarmerTaskPlanner.next(view))
    }

    @Test
    @DisplayName("Полив важнее вспашки")
    fun waterBeatsTill() {
        val view = FarmerWorldView(
            inventoryFull = false,
            inventoryHasItems = false,
            hasProduce = false,
            hasMatureCrop = false,
            hasTillable = true,
            hasEmptyFarmland = false,
            hasPlantable = false,
            hasImmatureCrop = true,
        )
        assertEquals(FarmerTask.WATER, FarmerTaskPlanner.next(view))
    }

    @Test
    @DisplayName("Сдача в сундук важнее полива, иначе фермер залипает на грядке")
    fun depositBeatsWater() {
        val view = FarmerWorldView(
            inventoryFull = false,
            inventoryHasItems = true,
            hasProduce = true,
            hasMatureCrop = false,
            hasTillable = false,
            hasEmptyFarmland = false,
            hasPlantable = false,
            hasImmatureCrop = true,
        )
        assertEquals(FarmerTask.DEPOSIT, FarmerTaskPlanner.next(view))
    }

    @Test
    @DisplayName("После работы оставшиеся предметы складывают в контейнер")
    fun leftoverGoesToContainer() {
        val view = FarmerWorldView(
            inventoryFull = false,
            inventoryHasItems = true,
            hasProduce = false,
            hasMatureCrop = false,
            hasTillable = false,
            hasEmptyFarmland = false,
            hasPlantable = false,
            hasImmatureCrop = false,
        )
        assertEquals(FarmerTask.DEPOSIT, FarmerTaskPlanner.next(view))
    }

    @Test
    @DisplayName("Пока задача свежая, фермер её не бросает")
    fun doesNotFlickerEarly() {
        val watering = FarmerWorldView(
            inventoryFull = false,
            inventoryHasItems = false,
            hasProduce = false,
            hasMatureCrop = false,
            hasTillable = false,
            hasEmptyFarmland = false,
            hasPlantable = false,
            hasImmatureCrop = true,
        )
        assertTrue(FarmerTaskPlanner.shouldKeep(FarmerTask.WATER, 5, watering))
    }

    @Test
    @DisplayName("Собранный урожай сдаётся раньше новой посадки")
    fun produceBeatsPlant() {
        val view = FarmerWorldView(
            inventoryFull = false,
            inventoryHasItems = true,
            hasProduce = true,
            hasMatureCrop = false,
            hasTillable = false,
            hasEmptyFarmland = true,
            hasPlantable = true,
            hasImmatureCrop = false,
        )
        assertEquals(FarmerTask.DEPOSIT, FarmerTaskPlanner.next(view))
    }

    @Test
    @DisplayName("Спелая пшеница собирается раньше сдачи уже лежащего урожая")
    fun harvestBeatsStoredProduce() {
        val view = FarmerWorldView(
            inventoryFull = false,
            inventoryHasItems = true,
            hasProduce = true,
            hasMatureCrop = true,
            hasTillable = false,
            hasEmptyFarmland = true,
            hasPlantable = true,
            hasImmatureCrop = false,
        )
        assertEquals(FarmerTask.HARVEST, FarmerTaskPlanner.next(view))
    }

    @Test
    @DisplayName("Костная мука важнее полива, но не посадки")
    fun bonemealBetweenPlantAndWater() {
        val grow = FarmerWorldView(
            inventoryFull = false,
            inventoryHasItems = true,
            hasProduce = false,
            hasMatureCrop = false,
            hasTillable = true,
            hasEmptyFarmland = false,
            hasPlantable = false,
            hasImmatureCrop = true,
            hasBonemeal = true,
            hasBonemealTarget = true,
        )
        assertEquals(FarmerTask.BONEMEAL, FarmerTaskPlanner.next(grow))
        val plant = grow.copy(hasEmptyFarmland = true, hasPlantable = true)
        assertEquals(FarmerTask.PLANT, FarmerTaskPlanner.next(plant))
    }
}
