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
            hasMatureCrop = true,
            hasTillable = true,
            hasEmptyFarmland = true,
            hasPlantable = true,
            hasImmatureCrop = true,
        )
        assertEquals(FarmerTask.DEPOSIT, FarmerTaskPlanner.next(view))
    }

    @Test
    @DisplayName("Пока задача свежая, фермер её не бросает")
    fun doesNotFlickerEarly() {
        val watering = FarmerWorldView(
            inventoryFull = false,
            inventoryHasItems = false,
            hasMatureCrop = false,
            hasTillable = false,
            hasEmptyFarmland = false,
            hasPlantable = false,
            hasImmatureCrop = true,
        )
        assertTrue(FarmerTaskPlanner.shouldKeep(FarmerTask.WATER, 5, watering))
    }
}
