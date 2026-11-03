package com.reallyusefulribbits.mod.event

import com.reallyusefulribbits.mod.loader.RuntimeHooks
import com.reallyusefulribbits.mod.morph.CurseClock
import com.reallyusefulribbits.mod.morph.PlayerMorph
import com.reallyusefulribbits.mod.morph.SorcererCurse
import com.reallyusefulribbits.mod.spell.SkinStatue
import com.reallyusefulribbits.mod.spell.DeleteWorld
import com.reallyusefulribbits.mod.spell.HandSpells
import net.minecraft.server.level.ServerPlayer
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
        if ((visual.morphFlight || visual.foreverFlight) && !player.isCreative && !player.isSpectator && !player.abilities.mayfly) {
            player.abilities.mayfly = true
            player.onUpdateAbilities()
        }
        if (visual.vvvvvv) {
            player.resetFallDistance()
            HandSpells.restoreGravityAboveWorld(serverPlayer)
        }
        if (player.tickCount % 20 == 0) SorcererCurse.pruneEffects(serverPlayer)
        CurseClock.tick(serverPlayer)
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
        if (player.isCreative || player.isSpectator || visual.morphFlight || visual.foreverFlight) return
        val abilities = player.abilities
        abilities.mayfly = false
        abilities.flying = false
        player.onUpdateAbilities()
    }

    fun onLogin(player: ServerPlayer) {
        SkinStatue.request(player)
        SorcererCurse.sync(player)
        val visual = RuntimeHooks.visual(player)
        if (visual.flightTicks > 0 || visual.morphFlight || visual.foreverFlight) {
            player.abilities.mayfly = true
            player.onUpdateAbilities()
        }
        if (visual.morphId.isNotBlank()) {
            player.refreshDimensions()
        }
        DeleteWorld.onLogin(player)
    }

    fun onStartTrack(watcher: ServerPlayer, target: ServerPlayer) {
        RuntimeHooks.sendToPlayer(watcher, SorcererCurse.payload(target))
    }

    fun onLoggedOut(player: ServerPlayer) {
        if (RuntimeHooks.visual(player).flightTicks > 0) {
            revokeTemporaryFlight(player)
        }
    }

    /** Новое тело после смерти уже несёт данные чародея: их переносит загрузчик. */
    fun onRespawn(player: ServerPlayer) {
        SorcererCurse.afterRespawn(player)
    }
}
