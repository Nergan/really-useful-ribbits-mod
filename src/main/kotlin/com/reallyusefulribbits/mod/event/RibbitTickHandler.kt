package com.reallyusefulribbits.mod.event

import com.reallyusefulribbits.mod.profession.RibbitBrain
import com.reallyusefulribbits.mod.util.DelayedTasks
import com.yungnickyoung.minecraft.ribbits.entity.RibbitEntity
import net.minecraft.server.level.ServerLevel
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.neoforge.event.tick.EntityTickEvent
import net.neoforged.neoforge.event.tick.ServerTickEvent

object RibbitTickHandler {
    @SubscribeEvent
    fun onEntityTick(event: EntityTickEvent.Post) {
        val ribbit = event.entity as? RibbitEntity ?: return
        val level = ribbit.level() as? ServerLevel ?: return
        RibbitBrain.tick(level, ribbit)
    }

    @SubscribeEvent
    fun onServerTick(event: ServerTickEvent.Post) {
        DelayedTasks.tick(event.server)
    }
}
