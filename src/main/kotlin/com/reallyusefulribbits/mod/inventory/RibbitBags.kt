package com.reallyusefulribbits.mod.inventory

import com.reallyusefulribbits.mod.attach.RibbitWorkData
import com.reallyusefulribbits.mod.logic.InventoryRules
import com.reallyusefulribbits.mod.logic.ProfessionKind
import com.reallyusefulribbits.mod.world.ContainerSupport
import net.minecraft.world.item.ItemStack

object RibbitBags {
    fun isFull(data: RibbitWorkData, kind: ProfessionKind): Boolean {
        val used = data.usedSlots(kind)
        if (kind == ProfessionKind.FISHERMAN) {
            return carriedCount(data, used) >= data.slotLimit(kind)
        }
        for (i in 0 until used) {
            val stack = data.items[i]
            if (stack.isEmpty) return false
            if (stack.count < data.slotCap(stack, kind)) return false
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
        if (kind == ProfessionKind.FISHERMAN) {
            for (i in 0 until used) {
                if (!data.items[i].isEmpty) data.items[i] = ContainerSupport.normalized(data.items[i])
            }
        }
        val budget = if (kind == ProfessionKind.FISHERMAN) {
            (data.slotLimit(kind) - carriedCount(data, used)).coerceAtLeast(0)
        } else {
            incoming.count
        }
        if (budget <= 0) return incoming
        val sample = if (kind == ProfessionKind.FISHERMAN) ContainerSupport.normalized(incoming) else incoming
        val cap = data.slotCap(sample, kind)
        val heldBack = incoming.count - minOf(incoming.count, budget)
        var left = incoming.count - heldBack
        for (i in 0 until used) {
            if (left <= 0) break
            val slot = data.items[i]
            if (slot.isEmpty || !ItemStack.isSameItemSameComponents(slot, sample)) continue
            val moved = minOf(cap - slot.count, left)
            if (moved <= 0) continue
            slot.grow(moved)
            left -= moved
        }
        for (i in 0 until used) {
            if (left <= 0) break
            if (!data.items[i].isEmpty) continue
            val moved = minOf(cap, left)
            data.items[i] = sample.copyWithCount(moved)
            left -= moved
        }
        return unplaced(incoming, left + heldBack)
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
            if (!data.items[i].isEmpty) out += ContainerSupport.pieces(data.items[i])
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
            val taken = stack.split(stack.maxStackSize)
            if (stack.isEmpty) data.items[i] = ItemStack.EMPTY
            return taken
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
}
