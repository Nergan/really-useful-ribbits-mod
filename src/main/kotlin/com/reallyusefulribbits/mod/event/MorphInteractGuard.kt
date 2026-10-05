package com.reallyusefulribbits.mod.event

import com.reallyusefulribbits.mod.logic.ProfessionKind
import com.reallyusefulribbits.mod.morph.PlayerMorph
import com.reallyusefulribbits.mod.util.professionKind
import com.yungnickyoung.minecraft.ribbits.entity.RibbitEntity
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items

object MorphInteractGuard {
    fun blocksWorld(player: Player): Boolean = !allowWorld(player)

    fun blocksItem(player: Player, stack: ItemStack): Boolean {
        if (allowWorld(player)) return false
        if (stack.`is`(Items.ENCHANTED_GOLDEN_APPLE)) return false
        return true
    }

    fun blocksEntity(player: Player, target: Entity): Boolean {
        if (allowWorld(player)) return false
        val ribbit = target as? RibbitEntity
        val appleOnSorcerer = ribbit != null &&
            ribbit.professionKind() == ProfessionKind.SORCERER &&
            player.mainHandItem.`is`(Items.ENCHANTED_GOLDEN_APPLE)
        return !appleOnSorcerer
    }

    private fun allowWorld(player: Player): Boolean {
        if (player.abilities.instabuild) return true
        val type = PlayerMorph.typeOf(player) ?: return true
        return PlayerMorph.isHumanoidType(type)
    }
}
