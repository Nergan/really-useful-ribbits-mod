package com.reallyusefulribbits.mod.event

import com.reallyusefulribbits.mod.config.ModConfig
import com.reallyusefulribbits.mod.config.ServerConfig
import com.reallyusefulribbits.mod.highlight.HighlightMarkers
import com.reallyusefulribbits.mod.logic.ProfessionKind
import com.reallyusefulribbits.mod.profession.MerchantAi
import com.reallyusefulribbits.mod.profession.SorcererAi
import com.reallyusefulribbits.mod.ride.HeadRide
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
import net.neoforged.bus.api.EventPriority
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent

object RibbitInteractionHandler {

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    fun onInteract(event: PlayerInteractEvent.EntityInteract) {
        val ribbit = event.target as? RibbitEntity ?: return
        if (event.hand != InteractionHand.MAIN_HAND) return
        val player = event.entity
        if (player.isShiftKeyDown) {
            event.isCanceled = true
            event.cancellationResult = InteractionResult.sidedSuccess(event.level.isClientSide)
            if (event.level.isClientSide) return
            if (ribbit.vehicle == player) {
                HeadRide.dismount(player, ribbit)
            } else {
                HeadRide.mount(player, ribbit)
            }
            return
        }
        if (event.level.isClientSide) return
        val level = event.level as? ServerLevel ?: return
        val serverPlayer = player as? ServerPlayer ?: return
        when (ribbit.professionKind()) {
            ProfessionKind.FISHERMAN, ProfessionKind.FARMER -> {
                event.isCanceled = true
                event.cancellationResult = InteractionResult.SUCCESS
                highlightWork(level, ribbit, serverPlayer)
            }
            ProfessionKind.SORCERER -> {
                event.isCanceled = true
                event.cancellationResult = InteractionResult.SUCCESS
                SorcererAi.onRightClick(level, ribbit, serverPlayer)
            }
            ProfessionKind.MERCHANT -> MerchantAi.onPlayerOpenedTrade(ribbit)
            else -> Unit
        }
    }

    private fun highlightWork(level: ServerLevel, ribbit: RibbitEntity, player: ServerPlayer) {
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
        data.farmOrigin?.let { origin ->
            blocks += WorldScan.farmBlocks(level, origin, ServerConfig.scanRadius())
        }
        HighlightMarkers.glowBlocks(level, blocks)
    }
}
