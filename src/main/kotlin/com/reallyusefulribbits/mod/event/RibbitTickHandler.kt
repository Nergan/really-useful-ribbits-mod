package com.reallyusefulribbits.mod.event

import com.reallyusefulribbits.mod.profession.RibbitBrain
import com.reallyusefulribbits.mod.util.DelayedTasks
import com.reallyusefulribbits.mod.world.ContainerSupport
import com.yungnickyoung.minecraft.ribbits.entity.RibbitEntity
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.Entity

object RibbitTickHandler {
    fun onEntityTick(entity: Entity) {
        val ribbit = entity as? RibbitEntity ?: return
        val level = ribbit.level() as? ServerLevel ?: return
        RibbitBrain.tick(level, ribbit)
    }

    fun onServerTick(server: MinecraftServer) {
        DelayedTasks.tick(server)
        ContainerSupport.tick(server)
    }
}
