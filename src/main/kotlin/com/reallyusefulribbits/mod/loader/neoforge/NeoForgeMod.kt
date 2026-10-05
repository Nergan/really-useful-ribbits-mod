package com.reallyusefulribbits.mod.loader.neoforge

import com.reallyusefulribbits.mod.ReallyUsefulRibbitsMod
import com.reallyusefulribbits.mod.attach.ModAttachments
import com.reallyusefulribbits.mod.config.ServerConfig
import com.reallyusefulribbits.mod.event.ModSetup
import com.reallyusefulribbits.mod.network.ModNetworking
import net.neoforged.bus.api.IEventBus
import net.neoforged.fml.ModContainer
import net.neoforged.fml.common.Mod
import net.neoforged.fml.config.ModConfig

@Mod(ReallyUsefulRibbitsMod.MOD_ID)
class NeoForgeMod(modEventBus: IEventBus, modContainer: ModContainer) {
    init {
        ReallyUsefulRibbitsMod.LOGGER.info("Initializing Really Useful Ribbits ({})", ReallyUsefulRibbitsMod.MOD_ID)
        NeoForgeRuntime.install()
        ModAttachments.ATTACHMENT_TYPES.register(modEventBus)
        modContainer.registerConfig(ModConfig.Type.SERVER, ServerConfig.SPEC)
        ModSetup.init(modEventBus, modContainer)
        ModNetworking.init(modEventBus)
        NeoForgeRuntime.register(modEventBus)
    }
}
