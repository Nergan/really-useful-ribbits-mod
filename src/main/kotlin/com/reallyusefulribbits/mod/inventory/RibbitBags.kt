package com.reallyusefulribbits.mod.inventory

import com.reallyusefulribbits.mod.attach.RibbitWorkData
import com.reallyusefulribbits.mod.logic.InventoryRules
import com.reallyusefulribbits.mod.logic.ProfessionKind
import net.minecraft.core.component.DataComponents
import net.minecraft.world.item.ItemStack

object RibbitBags {
    fun isFull(data: RibbitWorkData, kind: ProfessionKind): Boolean {
        val used = data.usedSlots(kind)
        if (kind == ProfessionKind.FISHERMAN) {
            return carriedCount(data, used) >= data.slotLimit(kind)
        }
        val limit = data.slotLimit(kind)
        for (i in 0 until used) {
            val stack = data.items[i]
            if (stack.isEmpty) return false
            val cap = minOf(limit, stack.maxStackSize)
            if (stack.count < cap) return false
        }
        return used > 0
    }

    fun carriedCount(data: RibbitWorkData, used: Int): Int {
        var total = 0
        for (i in 0 until used) {
            val stack = data.items[i]
            if (!stack.isEmpty) total += stack.count
        }
        return total
    }

    fun hasItems(data: RibbitWorkData, kind: ProfessionKind): Boolean {
        val used = data.usedSlots(kind)
        return (0 until used).any { !data.items[it].isEmpty }
    }

    fun insert(data: RibbitWorkData, kind: ProfessionKind, incoming: ItemStack): ItemStack {
        if (incoming.isEmpty) return ItemStack.EMPTY
        val used = data.usedSlots(kind)
        for (i in 0 until used) data.releaseCarryCap(data.items[i])
        val limit = data.slotLimit(kind)
        val budget = if (kind == ProfessionKind.FISHERMAN) {
            (limit - carriedCount(data, used)).coerceAtLeast(0)
        } else {
            incoming.count
        }
        if (budget <= 0) return incoming
        val accepted = minOf(incoming.count, budget)
        val heldBack = incoming.count - accepted
        val piece = incoming.copy()
        piece.count = accepted
        data.applyLimit(piece, kind)
        var remaining = piece.copy()
        for (i in 0 until used) {
            val slot = data.items[i]
            if (slot.isEmpty || !ItemStack.isSameItemSameComponents(slot, remaining)) continue
            val cap = minOf(limit, remaining.maxStackSize)
            val room = cap - slot.count
            if (room <= 0) continue
            val moved = minOf(room, remaining.count)
            slot.grow(moved)
            remaining.shrink(moved)
            if (remaining.isEmpty) return unplaced(incoming, heldBack)
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
                return unplaced(incoming, heldBack)
            }
        }
        if (heldBack > 0 && !remaining.isEmpty) remaining.grow(heldBack)
        if (remaining.isEmpty) return unplaced(incoming, heldBack)
        return remaining
    }

    private fun unplaced(incoming: ItemStack, count: Int): ItemStack {
        if (count <= 0) return ItemStack.EMPTY
        val out = incoming.copy()
        out.count = count
        return out
    }

    fun extractAll(data: RibbitWorkData, kind: ProfessionKind): List<ItemStack> {
        val used = data.usedSlots(kind)
        val out = ArrayList<ItemStack>()
        for (i in 0 until used) {
            if (!data.items[i].isEmpty) out += plainStack(data.items[i].copy())
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

    fun canInsertAll(data: RibbitWorkData, kind: ProfessionKind, stacks: List<ItemStack>): Boolean {
        val copy = RibbitWorkData()
        val used = data.usedSlots(kind)
        for (i in 0 until used) {
            copy.items[i] = data.items[i].copy()
        }
        for (stack in stacks) {
            if (stack.isEmpty) continue
            if (!insert(copy, kind, stack.copy()).isEmpty) return false
        }
        return true
    }

    fun takeSlot(data: RibbitWorkData, kind: ProfessionKind): ItemStack? {
        val used = data.usedSlots(kind)
        for (i in 0 until used) {
            val stack = data.items[i]
            if (stack.isEmpty) continue
            data.items[i] = ItemStack.EMPTY
            return stack
        }
        return null
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

    /** В сундук уходит обычная стопка, без лимита слота лягушки. */
    private fun plainStack(stack: ItemStack): ItemStack {
        val vanillaMax = stack.item.defaultMaxStackSize
        if (stack.count <= vanillaMax && stack.has(DataComponents.MAX_STACK_SIZE)) {
            stack.remove(DataComponents.MAX_STACK_SIZE)
        }
        return stack
    }
}
