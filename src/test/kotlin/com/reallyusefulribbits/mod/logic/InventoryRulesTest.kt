package com.reallyusefulribbits.mod.logic

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class InventoryRulesTest {

    @Test
    @DisplayName("Одно седло не заполняет сумку рыбака, четыре предмета заполняют")
    fun fisherCarryFillsAtFourItems() {
        val slots = MutableList<LogicSlot?>(InventoryRules.FISHER_SLOTS) { null }
        val leftover = InventoryRules.insert(
            slots,
            InventoryRules.FISHER_STACK,
            LogicSlot("minecraft:saddle", 1, 1),
            InventoryRules.FISHER_CARRY,
        )
        assertNull(leftover)
        assertFalse(InventoryRules.isFull(slots, InventoryRules.FISHER_STACK, InventoryRules.FISHER_CARRY))
        InventoryRules.insert(slots, InventoryRules.FISHER_STACK, LogicSlot("minecraft:cod", 1, 64), InventoryRules.FISHER_CARRY)
        InventoryRules.insert(slots, InventoryRules.FISHER_STACK, LogicSlot("minecraft:salmon", 1, 64), InventoryRules.FISHER_CARRY)
        InventoryRules.insert(slots, InventoryRules.FISHER_STACK, LogicSlot("minecraft:pufferfish", 1, 64), InventoryRules.FISHER_CARRY)
        assertEquals(4, InventoryRules.carried(slots))
        assertTrue(InventoryRules.isFull(slots, InventoryRules.FISHER_STACK, InventoryRules.FISHER_CARRY))
        assertFalse(
            InventoryRules.canInsert(
                slots,
                InventoryRules.FISHER_STACK,
                LogicSlot("minecraft:tropical_fish", 1, 64),
                InventoryRules.FISHER_CARRY,
            ),
        )
    }

    @Test
    @DisplayName("Рыбак уносит только 4 трески и складывает их в одну стопку")
    fun fisherStacksFishToFour() {
        val slots = MutableList<LogicSlot?>(InventoryRules.FISHER_SLOTS) { null }
        val leftover = InventoryRules.insert(
            slots,
            InventoryRules.FISHER_STACK,
            LogicSlot("minecraft:cod", 16, 64),
            InventoryRules.FISHER_CARRY,
        )
        assertEquals(12, leftover!!.count)
        assertEquals(4, slots[0]!!.count)
        assertTrue(InventoryRules.isFull(slots, InventoryRules.FISHER_STACK, InventoryRules.FISHER_CARRY))
        assertFalse(
            InventoryRules.canInsert(
                slots,
                InventoryRules.FISHER_STACK,
                LogicSlot("minecraft:cod", 1, 64),
                InventoryRules.FISHER_CARRY,
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

    @Test
    @DisplayName("Nitwit носит 27 слотов по 64")
    fun nitwitBagSize() {
        assertEquals(27, InventoryRules.slotCount(ProfessionKind.NITWIT))
        assertEquals(64, InventoryRules.slotLimit(ProfessionKind.NITWIT))
    }
}
