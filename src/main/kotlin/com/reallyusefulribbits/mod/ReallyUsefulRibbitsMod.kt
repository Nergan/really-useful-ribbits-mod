package com.reallyusefulribbits.mod

import com.reallyusefulribbits.mod.attach.ModAttachments
import com.reallyusefulribbits.mod.config.ServerConfig
import com.reallyusefulribbits.mod.event.ModSetup
import com.reallyusefulribbits.mod.event.PlayerTickHandler
import com.reallyusefulribbits.mod.event.RibbitGuideHandler
import com.reallyusefulribbits.mod.event.RibbitInteractionHandler
import com.reallyusefulribbits.mod.event.RibbitTickHandler
import com.reallyusefulribbits.mod.highlight.HighlightMarkers
import com.reallyusefulribbits.mod.item.ModItems
import com.reallyusefulribbits.mod.network.ModNetworking
import net.neoforged.bus.api.IEventBus
import net.neoforged.fml.ModContainer
import net.neoforged.fml.common.Mod
import net.neoforged.fml.config.ModConfig
import net.neoforged.neoforge.common.NeoForge
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

@Mod(ReallyUsefulRibbitsMod.MOD_ID)
class ReallyUsefulRibbitsMod(modEventBus: IEventBus, modContainer: ModContainer) {

    companion object {
        const val MOD_ID = "reallyusefulribbits"

        @JvmField
        val LOGGER: Logger = LogManager.getLogger(MOD_ID)
    }

    init {
        LOGGER.info("Initializing Really Useful Ribbits ({})", MOD_ID)
        ModAttachments.ATTACHMENT_TYPES.register(modEventBus)
        ModItems.register(modEventBus)
        modContainer.registerConfig(ModConfig.Type.SERVER, ServerConfig.SPEC)
        ModSetup.init(modEventBus, modContainer)
        ModNetworking.init(modEventBus)
        NeoForge.EVENT_BUS.register(RibbitInteractionHandler)
        NeoForge.EVENT_BUS.register(RibbitTickHandler)
        NeoForge.EVENT_BUS.register(PlayerTickHandler)
        NeoForge.EVENT_BUS.register(HighlightMarkers)
        NeoForge.EVENT_BUS.register(RibbitGuideHandler)
    }
}
