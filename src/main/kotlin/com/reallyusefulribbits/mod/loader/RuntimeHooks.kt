package com.reallyusefulribbits.mod.loader

import com.reallyusefulribbits.mod.attach.PlayerVisualData
import com.reallyusefulribbits.mod.attach.RibbitWorkData
import com.reallyusefulribbits.mod.network.PlayerVisualPayload
import com.yungnickyoung.minecraft.ribbits.entity.RibbitEntity
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level

/**
 * Точки, которые NeoForge и Fabric заполняют своими API.
 * Общий код мода их только вызывает.
 */
object RuntimeHooks {
    lateinit var work: (RibbitEntity) -> RibbitWorkData
    lateinit var visual: (Player) -> PlayerVisualData
    lateinit var sendTrackingAndSelf: (ServerPlayer, PlayerVisualPayload) -> Unit
    lateinit var sendToPlayer: (ServerPlayer, PlayerVisualPayload) -> Unit
    lateinit var scanRadius: () -> Int
    lateinit var chaosLevel: () -> Int
    lateinit var itemSlots: (Level, BlockPos) -> ItemSlots?
    lateinit var containerOpener: (ServerLevel) -> Player
    lateinit var patchouliLoaded: () -> Boolean
    lateinit var guideReceived: (Player) -> Boolean
    lateinit var markGuideReceived: (Player) -> Unit
}

class ItemSlots(
    val slots: Int,
    private val getStack: (Int) -> ItemStack,
    private val extract: (Int, Int) -> ItemStack,
    private val insertStacked: (ItemStack) -> ItemStack,
) {
    fun getStackInSlot(slot: Int): ItemStack = getStack(slot)

    fun extractItem(slot: Int, amount: Int): ItemStack = extract(slot, amount)

    fun insertItemStacked(stack: ItemStack): ItemStack = insertStacked(stack)
}
