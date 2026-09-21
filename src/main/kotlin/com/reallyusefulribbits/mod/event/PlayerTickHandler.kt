package com.reallyusefulribbits.mod.event

import com.reallyusefulribbits.mod.attach.ModAttachments
import com.reallyusefulribbits.mod.network.PlayerVisualPayload
import com.reallyusefulribbits.mod.ride.HeadRide
import com.yungnickyoung.minecraft.ribbits.entity.RibbitEntity
import net.minecraft.server.level.ServerPlayer
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.neoforge.event.entity.player.PlayerEvent
import net.neoforged.neoforge.event.tick.PlayerTickEvent
import net.neoforged.neoforge.network.PacketDistributor

object PlayerTickHandler {
    @SubscribeEvent
    fun onPlayerTick(event: PlayerTickEvent.Post) {
        val player = event.entity
        if (player.level().isClientSide) return
        if (!player.isShiftKeyDown) return
        val riders = player.passengers.filterIsInstance<RibbitEntity>()
        if (riders.isEmpty()) return
        for (ribbit in riders) {
            HeadRide.dismount(player, ribbit)
        }
    }

    @SubscribeEvent
    fun onLogin(event: PlayerEvent.PlayerLoggedInEvent) {
        val player = event.entity as? ServerPlayer ?: return
        val visual = player.getData(ModAttachments.VISUAL.get())
        PacketDistributor.sendToPlayer(player, PlayerVisualPayload(player.uuid, visual.upsideDown))
    }

    @SubscribeEvent
    fun onStartTrack(event: PlayerEvent.StartTracking) {
        val player = event.entity as? ServerPlayer ?: return
        val target = event.target as? ServerPlayer ?: return
        val visual = target.getData(ModAttachments.VISUAL.get())
        PacketDistributor.sendToPlayer(player, PlayerVisualPayload(target.uuid, visual.upsideDown))
    }
}
