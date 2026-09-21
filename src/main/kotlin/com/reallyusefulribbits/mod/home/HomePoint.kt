package com.reallyusefulribbits.mod.home

import com.reallyusefulribbits.mod.event.ModAdvancements
import com.reallyusefulribbits.mod.mixin.RibbitEntityAccessor
import com.yungnickyoung.minecraft.ribbits.entity.RibbitEntity
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack

object HomePoint {
    private val MARACA = ResourceLocation.fromNamespaceAndPath("ribbits", "maraca")

    fun isMaraca(stack: ItemStack): Boolean {
        if (stack.isEmpty) return false
        return BuiltInRegistries.ITEM.getKey(stack.item) == MARACA
    }

    fun setHere(player: Player, ribbit: RibbitEntity) {
        (ribbit as RibbitEntityAccessor).rurSetHomePosition(ribbit.blockPosition())
        ribbit.lookAt(player, 180f, 180f)
        val level = ribbit.level()
        if (level is ServerLevel) {
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, ribbit.x, ribbit.y + 0.6, ribbit.z, 10, 0.3, 0.25, 0.3, 0.02)
            level.playSound(null, ribbit.blockPosition(), SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.NEUTRAL, 0.8f, 1.2f)
        }
        if (player is ServerPlayer && !player.level().isClientSide) {
            ModAdvancements.grantNewHome(player)
        }
    }
}
