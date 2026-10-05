package com.reallyusefulribbits.mod.event

import com.reallyusefulribbits.mod.config.ModConfig
import com.reallyusefulribbits.mod.loader.RuntimeHooks
import com.reallyusefulribbits.mod.highlight.HighlightMarkers
import com.reallyusefulribbits.mod.home.HomePoint
import com.reallyusefulribbits.mod.logic.ProfessionKind
import com.reallyusefulribbits.mod.profession.MerchantAi
import com.reallyusefulribbits.mod.profession.SorcererAi
import com.reallyusefulribbits.mod.util.professionKind
import com.reallyusefulribbits.mod.util.work
import com.reallyusefulribbits.mod.world.WorldScan
import com.yungnickyoung.minecraft.ribbits.entity.RibbitEntity
import com.yungnickyoung.minecraft.ribbits.module.SoundModule
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Items
import net.minecraft.world.level.Level

object RibbitInteractionHandler {

    /** Не null — взаимодействие нужно отменить и вернуть этот результат. */
    fun onEntityInteract(player: Player, target: Entity, hand: InteractionHand, level: Level): InteractionResult? {
        val ribbit = target as? RibbitEntity ?: return null
        if (hand != InteractionHand.MAIN_HAND) return null
        if (HomePoint.isMaraca(player.mainHandItem)) {
            if (!level.isClientSide) {
                HomePoint.setHere(player, ribbit)
            }
            return InteractionResult.sidedSuccess(level.isClientSide)
        }
        if (level.isClientSide) return null
        val serverLevel = level as? ServerLevel ?: return null
        val serverPlayer = player as? ServerPlayer ?: return null
        return when (ribbit.professionKind()) {
            ProfessionKind.FISHERMAN, ProfessionKind.FARMER -> {
                highlightWork(serverLevel, ribbit)
                InteractionResult.SUCCESS
            }
            ProfessionKind.SORCERER -> {
                handleSorcerer(serverLevel, ribbit, serverPlayer)
                InteractionResult.SUCCESS
            }
            ProfessionKind.MERCHANT -> {
                MerchantAi.onPlayerOpenedTrade(ribbit)
                null
            }
            else -> null
        }
    }

    private fun handleSorcerer(level: ServerLevel, ribbit: RibbitEntity, player: ServerPlayer) {
        val held = player.mainHandItem
        if (held.`is`(Items.ENCHANTED_GOLDEN_APPLE)) {
            if (SorcererAi.cleanseWithApple(level, ribbit, player) && !player.abilities.instabuild) {
                held.shrink(1)
            }
            return
        }
        if (held.`is`(Items.AMETHYST_SHARD)) {
            if (SorcererAi.tryCast(level, ribbit, player) && !player.abilities.instabuild) {
                held.shrink(1)
            }
            return
        }
        ribbit.lookControl.setLookAt(player, 180f, 180f)
        ribbit.lookAt(player, 180f, 180f)
        level.sendParticles(ParticleTypes.ANGRY_VILLAGER, ribbit.x, ribbit.y + 0.9, ribbit.z, 8, 0.25, 0.2, 0.25, 0.01)
        level.playSound(null, ribbit.blockPosition(), SoundEvents.AMETHYST_BLOCK_HIT, SoundSource.NEUTRAL, 0.5f, 1.6f)
    }

    private fun highlightWork(level: ServerLevel, ribbit: RibbitEntity) {
        val data = ribbit.work()
        val kind = ribbit.professionKind()
        val missing = when (kind) {
            ProfessionKind.FISHERMAN -> data.waterPos == null || data.containerPos == null
            ProfessionKind.FARMER -> data.farmOrigin == null || data.containerPos == null
            else -> false
        }
        if (missing) {
            level.sendParticles(ParticleTypes.ANGRY_VILLAGER, ribbit.x, ribbit.y + 0.9, ribbit.z, 6, 0.25, 0.2, 0.25, 0.01)
            level.playSound(null, ribbit.blockPosition(), SoundModule.ENTITY_RIBBIT_HURT.get(), SoundSource.NEUTRAL, 1f, 1f)
            return
        }
        level.playSound(null, ribbit.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.NEUTRAL, 0.8f, 1.3f)
        ribbit.addEffect(MobEffectInstance(MobEffects.GLOWING, ModConfig.HIGHLIGHT_TICKS, 0, false, false))
        val blocks = ArrayList<net.minecraft.core.BlockPos>()
        data.containerPos?.let { blocks += it }
        data.waterPos?.let { blocks += it }
        if (data.farmMemory.isNotEmpty()) {
            blocks += data.farmMemory
        } else {
            data.farmOrigin?.let { origin ->
                blocks += WorldScan.allWorkBlocks(level, origin, RuntimeHooks.scanRadius())
            }
        }
        HighlightMarkers.glowBlocks(level, blocks)
    }
}
