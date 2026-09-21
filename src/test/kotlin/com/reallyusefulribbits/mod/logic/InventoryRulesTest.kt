package com.reallyusefulribbits.mod.logic

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class InventoryRulesTest {

    @Test
    @DisplayName("У рыбака один слот на 16, нестакаемое сразу забивает сумку")
    fun fisherUnstackableFillsBag() {
        val slots = mutableListOf<LogicSlot?>(null)
        val leftover = InventoryRules.insert(
            slots,
            InventoryRules.FISHER_STACK,
            LogicSlot("minecraft:saddle", 1, 1),
        )
        assertNull(leftover)
        assertTrue(InventoryRules.isFull(slots, InventoryRules.FISHER_STACK))
    }

    @Test
    @DisplayName("Рыбак может сложить 16 трески в один слот")
    fun fisherStacksFishToSixteen() {
        val slots = mutableListOf<LogicSlot?>(null)
        val leftover = InventoryRules.insert(
            slots,
            InventoryRules.FISHER_STACK,
            LogicSlot("minecraft:cod", 16, 64),
        )
        assertNull(leftover)
        assertEquals(16, slots[0]!!.count)
        assertTrue(InventoryRules.isFull(slots, InventoryRules.FISHER_STACK))
        assertFalse(
            InventoryRules.canInsert(
                slots,
                InventoryRules.FISHER_STACK,
                LogicSlot("minecraft:cod", 1, 64),
            ),
        )
    }

    @Test
    @DisplayName("Фермер складывает в четыре слота по 64")
    fun farmerFourStacks() {
        val slots = MutableList<LogicSlot?>(4) { null }
        repeat(4) {
            InventoryRules.insert(slots, 64, LogicSlot("minecraft:wheat", 64, 64))
        }
        assertTrue(InventoryRules.isFull(slots, 64))
        val dumped = InventoryRules.extractAll(slots)
        assertEquals(4, dumped.size)
        assertTrue(slots.all { it == null })
    }

    @Test
    @DisplayName("Торговец держит стопки до 256")
    fun merchantAllows256() {
        val slots = MutableList<LogicSlot?>(27) { null }
        InventoryRules.insert(slots, InventoryRules.MERCHANT_STACK, LogicSlot("minecraft:emerald", 256, 64))
        assertEquals(256, slots[0]!!.count)
    }
}
