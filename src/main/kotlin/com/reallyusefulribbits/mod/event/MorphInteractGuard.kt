package com.reallyusefulribbits.mod.event

import com.reallyusefulribbits.mod.logic.ProfessionKind
import com.reallyusefulribbits.mod.morph.PlayerMorph
import com.reallyusefulribbits.mod.util.professionKind
import com.yungnickyoung.minecraft.ribbits.entity.RibbitEntity
import net.minecraft.world.item.Items
import net.neoforged.bus.api.EventPriority
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent
import net.neoforged.neoforge.event.level.BlockEvent

object MorphInteractGuard {
    @SubscribeEvent(priority = EventPriority.HIGH)
    fun onRightClickBlock(event: PlayerInteractEvent.RightClickBlock) {
        if (allowWorld(event.entity)) return
        event.isCanceled = true
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    fun onLeftClickBlock(event: PlayerInteractEvent.LeftClickBlock) {
        if (allowWorld(event.entity)) return
        event.isCanceled = true
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    fun onRightClickItem(event: PlayerInteractEvent.RightClickItem) {
        if (allowWorld(event.entity)) return
        if (event.itemStack.`is`(Items.ENCHANTED_GOLDEN_APPLE)) return
        event.isCanceled = true
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    fun onEntityInteract(event: PlayerInteractEvent.EntityInteract) {
        if (allowWorld(event.entity)) return
        val ribbit = event.target as? RibbitEntity
        val appleOnSorcerer = ribbit != null &&
            ribbit.professionKind() == ProfessionKind.SORCERER &&
            event.entity.mainHandItem.`is`(Items.ENCHANTED_GOLDEN_APPLE)
        if (appleOnSorcerer) return
        event.isCanceled = true
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    fun onBreak(event: BlockEvent.BreakEvent) {
        if (allowWorld(event.player)) return
        event.isCanceled = true
    }

    private fun allowWorld(player: net.minecraft.world.entity.player.Player): Boolean {
        if (player.abilities.instabuild) return true
        val type = PlayerMorph.typeOf(player) ?: return true
        return PlayerMorph.isHumanoidType(type)
    }
}
