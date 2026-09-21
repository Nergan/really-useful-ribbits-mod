package com.reallyusefulribbits.mod.world

import com.reallyusefulribbits.mod.config.ModConfig
import com.reallyusefulribbits.mod.util.DelayedTasks
import net.minecraft.core.BlockPos
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.Container
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.EnderChestBlock
import net.minecraft.world.level.block.entity.BlockEntity
import net.neoforged.neoforge.capabilities.Capabilities
import net.neoforged.neoforge.common.util.FakePlayerFactory
import net.neoforged.neoforge.items.IItemHandler
import net.neoforged.neoforge.items.ItemHandlerHelper

object ContainerSupport {
    fun isStorage(level: Level, pos: BlockPos): Boolean {
        val state = level.getBlockState(pos)
        if (state.block is EnderChestBlock) return false
        val be = level.getBlockEntity(pos)
        if (be is Container) return true
        return level.getCapability(Capabilities.ItemHandler.BLOCK, pos, null as net.minecraft.core.Direction?) != null
    }

    fun handler(level: Level, pos: BlockPos): IItemHandler? =
        level.getCapability(Capabilities.ItemHandler.BLOCK, pos, null as net.minecraft.core.Direction?)

    fun insertAll(level: Level, pos: BlockPos, stacks: List<ItemStack>): List<ItemStack> {
        val handler = handler(level, pos) ?: return stacks
        val leftover = ArrayList<ItemStack>()
        for (stack in stacks) {
            val rest = ItemHandlerHelper.insertItemStacked(handler, stack, false)
            if (!rest.isEmpty) leftover += rest
        }
        return leftover
    }

    fun extractMatching(level: Level, pos: BlockPos, test: (ItemStack) -> Boolean, count: Int): ItemStack {
        val handler = handler(level, pos) ?: return ItemStack.EMPTY
        var remaining = count
        var taken = ItemStack.EMPTY
        for (slot in 0 until handler.slots) {
            val peek = handler.getStackInSlot(slot)
            if (peek.isEmpty || !test(peek)) continue
            val extracted = handler.extractItem(slot, remaining, false)
            if (extracted.isEmpty) continue
            if (taken.isEmpty) {
                taken = extracted
            } else if (ItemStack.isSameItemSameComponents(taken, extracted)) {
                taken.grow(extracted.count)
            }
            remaining -= extracted.count
            if (remaining <= 0) break
        }
        return taken
    }

    fun openBriefly(level: ServerLevel, pos: BlockPos, actor: Player? = null) {
        val be = level.getBlockEntity(pos) ?: return
        if (be !is Container) return
        val player = actor ?: FakePlayerFactory.getMinecraft(level)
        be.startOpen(player)
        DelayedTasks.later(level.server, ModConfig.CONTAINER_OPEN_TICKS) {
            val later: BlockEntity = level.getBlockEntity(pos) ?: return@later
            if (later is Container) later.stopOpen(player)
        }
    }

    fun looksLikeContainerId(block: net.minecraft.world.level.block.Block): Boolean {
        val path = BuiltInRegistries.BLOCK.getKey(block).path
        return path.contains("chest") || path.contains("barrel") || path.contains("shulker") ||
            path.contains("crate") || path.contains("cabinet") || path.contains("basket")
    }
}
