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
    fun mount(player: Player, ribbit: RibbitEntity): Boolean {
        if (ribbit.isPassenger) return false
        if (player.passengers.any { it is RibbitEntity }) return false
        val data = ribbit.work()
        data.riding = true
        data.savedInstrument = ribbit.ribbitData.instrument.id.path
        if (ribbit.ribbitData.instrument == RibbitInstrumentModule.NONE) {
            ribbit.setInstrument(RibbitInstrumentModule.getRandomInstrument())
        }
        // Сидячая анимация — игра на инструменте. Пакеты музыки группы не шлём.
        ribbit.setPlayingInstrument(true)
        ribbit.setFishing(false)
        ribbit.setWatering(false)
        ribbit.setBuffing(false)
        ribbit.navigation.stop()
        return ribbit.startRiding(player, true)
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
        (ribbit as RibbitEntityAccessor).rurSetHomePosition(ribbit.blockPosition())
        if (player is ServerPlayer) {
            ModAdvancements.grantNewHome(player)
        }
        @Suppress("UNUSED_VARIABLE")
        val ignored = ribbit.professionKind()
    }
}
