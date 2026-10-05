package com.reallyusefulribbits.mod.event

import com.reallyusefulribbits.mod.loader.RuntimeHooks
import com.reallyusefulribbits.mod.morph.PlayerMorph
import com.reallyusefulribbits.mod.morph.SorcererCurse
import com.reallyusefulribbits.mod.network.PlayerVisualPayload
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.damagesource.DamageTypes
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.GameType

object PlayerTickHandler {
    fun onPlayerTick(player: Player) {
        if (player.level().isClientSide) return
        val serverPlayer = player as? ServerPlayer ?: return
        val visual = RuntimeHooks.visual(player)
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
    }

    fun grantTemporaryFlight(player: ServerPlayer, ticks: Int) {
        val visual = RuntimeHooks.visual(player)
        visual.flightTicks = ticks
        val abilities = player.abilities
        abilities.mayfly = true
        player.onUpdateAbilities()
    }

    fun revokeTemporaryFlight(player: ServerPlayer) {
        val visual = RuntimeHooks.visual(player)
        visual.flightTicks = 0
        if (player.isCreative || player.isSpectator || visual.morphFlight) return
        val abilities = player.abilities
        abilities.mayfly = false
        abilities.flying = false
        player.onUpdateAbilities()
    }

    fun onLogin(player: ServerPlayer) {
        SorcererCurse.sync(player)
        val visual = RuntimeHooks.visual(player)
        if (visual.flightTicks > 0 || visual.morphFlight) {
            player.abilities.mayfly = true
            player.onUpdateAbilities()
        }
        if (visual.morphId.isNotBlank()) {
            player.refreshDimensions()
        }
    }

    fun onStartTrack(watcher: ServerPlayer, target: ServerPlayer) {
        val visual = RuntimeHooks.visual(target)
        RuntimeHooks.sendToPlayer(
            watcher,
            PlayerVisualPayload(target.uuid, visual.upsideDown, visual.morphId),
        )
    }

    fun onLoggedOut(player: ServerPlayer) {
        if (RuntimeHooks.visual(player).flightTicks > 0) {
            revokeTemporaryFlight(player)
        }
    }

    fun onDeath(player: ServerPlayer, source: DamageSource) {
        if (source.`is`(DamageTypes.GENERIC_KILL)) return
        SorcererCurse.clear(player)
    }
}
