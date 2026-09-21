package com.reallyusefulribbits.mod.ride

import com.reallyusefulribbits.mod.event.ModAdvancements
import com.reallyusefulribbits.mod.mixin.RibbitEntityAccessor
import com.reallyusefulribbits.mod.util.professionKind
import com.reallyusefulribbits.mod.util.work
import com.yungnickyoung.minecraft.ribbits.entity.RibbitEntity
import com.yungnickyoung.minecraft.ribbits.module.RibbitInstrumentModule
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.player.Player

object HeadRide {
    fun holdOnHead(player: Player, ribbit: RibbitEntity) {
        ribbit.setPos(player.x, player.y + player.bbHeight + 0.15, player.z)
        ribbit.xo = player.x
        ribbit.yo = player.y + player.bbHeight + 0.15
        ribbit.zo = player.z
        ribbit.setDeltaMovement(player.deltaMovement)
    }

    fun mount(player: Player, ribbit: RibbitEntity): Boolean {
        if (ribbit.vehicle == player || player.passengers.any { it is RibbitEntity && it !== ribbit }) {
            return ribbit.vehicle == player
        }
        val data = ribbit.work()
        ribbit.setFishing(false)
        ribbit.setWatering(false)
        ribbit.setBuffing(false)
        ribbit.navigation.stop()
        ribbit.setDeltaMovement(0.0, 0.0, 0.0)
        holdOnHead(player, ribbit)
        val mounted = ribbit.startRiding(player, true)
        if (!mounted || ribbit.vehicle != player) {
            data.riding = false
            return false
        }
        data.riding = true
        holdOnHead(player, ribbit)
        data.savedInstrument = ribbit.ribbitData.instrument.id.path
        if (ribbit.ribbitData.instrument == RibbitInstrumentModule.NONE) {
            ribbit.setInstrument(RibbitInstrumentModule.getRandomInstrument())
        }
        ribbit.setPlayingInstrument(true)
        return true
    }

    fun dismount(player: Player, ribbit: RibbitEntity) {
        val data = ribbit.work()
        ribbit.stopRiding()
        ribbit.setPlayingInstrument(false)
        if (data.savedInstrument == "none" || data.savedInstrument.isBlank()) {
            ribbit.setInstrument(RibbitInstrumentModule.NONE)
        }
        data.riding = false
        data.savedInstrument = "none"
        ribbit.setPos(player.x, player.y, player.z)
        (ribbit as RibbitEntityAccessor).rurSetHomePosition(ribbit.blockPosition())
        if (player is ServerPlayer && !player.level().isClientSide) {
            ModAdvancements.grantNewHome(player)
        }
        @Suppress("UNUSED_VARIABLE")
        val ignored = ribbit.professionKind()
    }
}
