package com.reallyusefulribbits.mod.inventory

import com.reallyusefulribbits.mod.attach.RibbitWorkData
import com.reallyusefulribbits.mod.logic.InventoryRules
import com.reallyusefulribbits.mod.logic.ProfessionKind
import net.minecraft.world.item.ItemStack

object RibbitBags {
    fun isFull(data: RibbitWorkData, kind: ProfessionKind): Boolean {
        val used = data.usedSlots(kind)
        val limit = data.slotLimit(kind)
        for (i in 0 until used) {
            val stack = data.items[i]
            if (stack.isEmpty) return false
            val cap = minOf(limit, stack.maxStackSize)
            if (stack.count < cap) return false
        }
        return used > 0
    }

    fun hasItems(data: RibbitWorkData, kind: ProfessionKind): Boolean {
        val used = data.usedSlots(kind)
        return (0 until used).any { !data.items[it].isEmpty }
    }

    fun insert(data: RibbitWorkData, kind: ProfessionKind, incoming: ItemStack): ItemStack {
        if (incoming.isEmpty) return ItemStack.EMPTY
        data.applyLimit(incoming, kind)
        val used = data.usedSlots(kind)
        val limit = data.slotLimit(kind)
        var remaining = incoming.copy()
        for (i in 0 until used) {
            val slot = data.items[i]
            if (slot.isEmpty || !ItemStack.isSameItemSameComponents(slot, remaining)) continue
            val cap = minOf(limit, remaining.maxStackSize)
            val room = cap - slot.count
            if (room <= 0) continue
            val moved = minOf(room, remaining.count)
            slot.grow(moved)
            remaining.shrink(moved)
            if (remaining.isEmpty) return ItemStack.EMPTY
        }
        for (i in 0 until used) {
            if (!data.items[i].isEmpty) continue
            val placed = remaining.copy()
            data.applyLimit(placed, kind)
            val cap = minOf(limit, placed.maxStackSize)
            if (placed.count > cap) {
                remaining = placed.copy()
                remaining.count = placed.count - cap
                placed.count = cap
                data.items[i] = placed
            } else {
                data.items[i] = placed
                return ItemStack.EMPTY
            }
        }
        return remaining
    }

    fun extractAll(data: RibbitWorkData, kind: ProfessionKind): List<ItemStack> {
        val used = data.usedSlots(kind)
        val out = ArrayList<ItemStack>()
        for (i in 0 until used) {
            if (!data.items[i].isEmpty) out += data.items[i].copy()
            data.items[i] = ItemStack.EMPTY
        }
        return out
    }

    fun find(data: RibbitWorkData, kind: ProfessionKind, test: (ItemStack) -> Boolean): ItemStack {
        val used = data.usedSlots(kind)
        for (i in 0 until used) {
            val stack = data.items[i]
            if (!stack.isEmpty && test(stack)) return stack
        }
        return ItemStack.EMPTY
    }

    fun takeOne(data: RibbitWorkData, kind: ProfessionKind, test: (ItemStack) -> Boolean): ItemStack {
        val used = data.usedSlots(kind)
        for (i in 0 until used) {
            val stack = data.items[i]
            if (stack.isEmpty || !test(stack)) continue
            val taken = stack.split(1)
            if (stack.isEmpty) data.items[i] = ItemStack.EMPTY
            return taken
        }
        return ItemStack.EMPTY
    }

    @Suppress("unused")
    fun capacityHint(kind: ProfessionKind): Pair<Int, Int> =
        InventoryRules.slotCount(kind) to InventoryRules.slotLimit(kind)
}
