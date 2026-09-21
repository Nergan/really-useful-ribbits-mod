package com.reallyusefulribbits.mod.event

import com.reallyusefulribbits.mod.attach.ModAttachments
import com.reallyusefulribbits.mod.morph.PlayerMorph
import com.reallyusefulribbits.mod.morph.SorcererCurse
import com.reallyusefulribbits.mod.network.PlayerVisualPayload
import com.reallyusefulribbits.mod.ride.HeadRide
import com.yungnickyoung.minecraft.ribbits.entity.RibbitEntity
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.damagesource.DamageTypes
import net.minecraft.world.level.GameType
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent
import net.neoforged.neoforge.event.entity.player.PlayerEvent
import net.neoforged.neoforge.event.tick.PlayerTickEvent
import net.neoforged.neoforge.network.PacketDistributor

object PlayerTickHandler {
    @SubscribeEvent
    fun onPlayerTick(event: PlayerTickEvent.Post) {
        val player = event.entity
        if (player.level().isClientSide) return
        val serverPlayer = player as? ServerPlayer ?: return
        val visual = player.getData(ModAttachments.VISUAL.get())
        if (visual.flightTicks > 0) {
            visual.flightTicks--
            if (visual.flightTicks <= 0 && !visual.morphFlight) {
                revokeTemporaryFlight(serverPlayer)
            }
        }
        if (visual.spectatorTicks > 0) {
            visual.spectatorTicks--
            if (visual.spectatorTicks <= 0 && player.gameMode.gameModeForPlayer == GameType.SPECTATOR) {
                val mode = GameType.byName(visual.previousGameMode) ?: GameType.SURVIVAL
                player.setGameMode(mode)
            }
        }
        val morphType = PlayerMorph.typeOf(player)
        if (morphType != null && PlayerMorph.diesOnLand(player.level(), morphType) && !player.isInWaterOrBubble) {
            if (player.airSupply > 0) {
                player.airSupply = player.airSupply - 5
            } else {
                player.hurt(player.damageSources().drown(), 2f)
            }
        }
        if (visual.morphFlight && !player.isCreative && !player.isSpectator) {
            player.abilities.mayfly = true
        }
        val riders = player.passengers.filterIsInstance<RibbitEntity>()
        if (riders.isEmpty()) return
        if (!player.isShiftKeyDown) {
            visual.headRideDismountArmed = true
            return
        }
        if (!visual.headRideDismountArmed) return
        for (ribbit in riders) {
            HeadRide.dismount(player, ribbit)
        }
        visual.headRideDismountArmed = false
    }

    fun grantTemporaryFlight(player: ServerPlayer, ticks: Int) {
        val visual = player.getData(ModAttachments.VISUAL.get())
        visual.flightTicks = ticks
        val abilities = player.abilities
        abilities.mayfly = true
        player.onUpdateAbilities()
    }

    fun revokeTemporaryFlight(player: ServerPlayer) {
        val visual = player.getData(ModAttachments.VISUAL.get())
        visual.flightTicks = 0
        if (player.isCreative || player.isSpectator || visual.morphFlight) return
        val abilities = player.abilities
        abilities.mayfly = false
        abilities.flying = false
        player.onUpdateAbilities()
    }

    @SubscribeEvent
    fun onLogin(event: PlayerEvent.PlayerLoggedInEvent) {
        val player = event.entity as? ServerPlayer ?: return
        SorcererCurse.sync(player)
        val visual = player.getData(ModAttachments.VISUAL.get())
        if (visual.flightTicks > 0 || visual.morphFlight) {
            player.abilities.mayfly = true
            player.onUpdateAbilities()
        }
        if (visual.morphId.isNotBlank()) {
            player.refreshDimensions()
        }
    }

    @SubscribeEvent
    fun onStartTrack(event: PlayerEvent.StartTracking) {
        val player = event.entity as? ServerPlayer ?: return
        val target = event.target as? ServerPlayer ?: return
        val visual = target.getData(ModAttachments.VISUAL.get())
        PacketDistributor.sendToPlayer(
            player,
            PlayerVisualPayload(target.uuid, visual.upsideDown, visual.morphId),
        )
    }

    @SubscribeEvent
    fun onLoggedOut(event: PlayerEvent.PlayerLoggedOutEvent) {
        val player = event.entity as? ServerPlayer ?: return
        if (player.getData(ModAttachments.VISUAL.get()).flightTicks > 0) {
            revokeTemporaryFlight(player)
        }
    }

    @SubscribeEvent
    fun onDeath(event: LivingDeathEvent) {
        val player = event.entity as? ServerPlayer ?: return
        if (event.source.`is`(DamageTypes.GENERIC_KILL)) return
        SorcererCurse.clear(player)
    }
}
