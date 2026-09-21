package com.reallyusefulribbits.mod.logic

data class LogicSlot(
    val itemId: String,
    val count: Int,
    val itemMaxStack: Int,
)

object InventoryRules {
    const val FISHER_SLOTS = 1
    const val FISHER_STACK = 4
    const val FARMER_SLOTS = 4
    const val FARMER_STACK = 64
    const val MERCHANT_SLOTS = 27
    const val MERCHANT_STACK = 256
    const val NITWIT_SLOTS = 27
    const val NITWIT_STACK = 64

    fun slotLimit(kind: ProfessionKind): Int = when (kind) {
        ProfessionKind.FISHERMAN -> FISHER_STACK
        ProfessionKind.FARMER -> FARMER_STACK
        ProfessionKind.MERCHANT -> MERCHANT_STACK
        ProfessionKind.NITWIT -> NITWIT_STACK
        else -> 64
    }

    fun slotCount(kind: ProfessionKind): Int = when (kind) {
        ProfessionKind.FISHERMAN -> FISHER_SLOTS
        ProfessionKind.FARMER -> FARMER_SLOTS
        ProfessionKind.MERCHANT -> MERCHANT_SLOTS
        ProfessionKind.NITWIT -> NITWIT_SLOTS
        else -> 0
    }

    fun maxForSlot(slotLimit: Int, itemMaxStack: Int): Int =
        if (itemMaxStack <= 1) 1 else slotLimit

    fun isFull(slots: List<LogicSlot?>, slotLimit: Int): Boolean {
        if (slots.any { it == null }) return false
        return slots.all { slot ->
            slot != null && slot.count >= maxForSlot(slotLimit, slot.itemMaxStack)
        }
    }

    fun canInsert(slots: List<LogicSlot?>, slotLimit: Int, incoming: LogicSlot): Boolean {
        if (incoming.count <= 0) return true
        var remaining = incoming.count
        val copies = slots.toMutableList()
        for (i in copies.indices) {
            val slot = copies[i] ?: continue
            if (slot.itemId != incoming.itemId) continue
            val room = maxForSlot(slotLimit, incoming.itemMaxStack) - slot.count
            if (room > 0) remaining -= room
            if (remaining <= 0) return true
        }
        val empty = copies.count { it == null }
        val perEmpty = maxForSlot(slotLimit, incoming.itemMaxStack)
        return remaining <= empty * perEmpty
    }

    fun insert(slots: MutableList<LogicSlot?>, slotLimit: Int, incoming: LogicSlot): LogicSlot? {
        if (incoming.count <= 0) return null
        var remaining = incoming.count
        for (i in slots.indices) {
            val slot = slots[i] ?: continue
            if (slot.itemId != incoming.itemId) continue
            val max = maxForSlot(slotLimit, incoming.itemMaxStack)
            val room = max - slot.count
            if (room <= 0) continue
            val moved = minOf(room, remaining)
            slots[i] = slot.copy(count = slot.count + moved)
            remaining -= moved
            if (remaining <= 0) return null
        }
        for (i in slots.indices) {
            if (slots[i] != null) continue
            val max = maxForSlot(slotLimit, incoming.itemMaxStack)
            val moved = minOf(max, remaining)
            slots[i] = incoming.copy(count = moved)
            remaining -= moved
            if (remaining <= 0) return null
        }
        return incoming.copy(count = remaining)
    }

    fun extractAll(slots: MutableList<LogicSlot?>): List<LogicSlot> {
        val out = slots.filterNotNull()
        for (i in slots.indices) slots[i] = null
        return out
    }
}
