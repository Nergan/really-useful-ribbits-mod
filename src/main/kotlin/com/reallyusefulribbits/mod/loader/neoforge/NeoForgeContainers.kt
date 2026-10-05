package com.reallyusefulribbits.mod.loader.neoforge

import com.reallyusefulribbits.mod.loader.ItemSlots
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.Container
import net.minecraft.world.level.Level
import net.neoforged.neoforge.capabilities.Capabilities
import net.neoforged.neoforge.items.IItemHandler
import net.neoforged.neoforge.items.ItemHandlerHelper
import net.neoforged.neoforge.items.wrapper.InvWrapper

object NeoForgeContainers {
    fun slots(level: Level, pos: BlockPos): ItemSlots? {
        val handler = find(level, pos) ?: return null
        return ItemSlots(
            slots = handler.slots,
            getStack = { slot -> handler.getStackInSlot(slot) },
            extract = { slot, amount -> handler.extractItem(slot, amount, false) },
            insertStacked = { stack -> ItemHandlerHelper.insertItemStacked(handler, stack, false) },
        )
    }

    private fun find(level: Level, pos: BlockPos): IItemHandler? {
        val found = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, null as Direction?)
        if (found != null) return found
        for (side in Direction.entries) {
            val sided = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, side)
            if (sided != null) return sided
        }
        val blockEntity = level.getBlockEntity(pos)
        if (blockEntity is Container) return InvWrapper(blockEntity)
        return null
    }
}
