package com.reallyusefulribbits.mod.event

import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.IEventBus
import net.neoforged.fml.ModContainer
import net.neoforged.fml.loading.FMLEnvironment

object ModSetup {
    fun init(modBus: IEventBus, modContainer: ModContainer) {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            com.reallyusefulribbits.mod.client.ClientModEvents.init(modBus, modContainer)
        }
    }
}
