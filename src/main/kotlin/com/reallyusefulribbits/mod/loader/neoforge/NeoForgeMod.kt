package com.reallyusefulribbits.mod.loader.neoforge

import com.reallyusefulribbits.mod.ReallyUsefulRibbitsMod
import com.reallyusefulribbits.mod.attach.ModAttachments
import com.reallyusefulribbits.mod.config.ServerConfig
import com.reallyusefulribbits.mod.event.ModSetup
import com.reallyusefulribbits.mod.logic.ServerSettingsText
import com.reallyusefulribbits.mod.network.ModNetworking
import net.neoforged.bus.api.IEventBus
import net.neoforged.fml.ModContainer
import net.neoforged.fml.common.Mod
import net.neoforged.fml.config.ModConfig
import net.neoforged.fml.loading.FMLPaths
import java.nio.file.Files

@Mod(ReallyUsefulRibbitsMod.MOD_ID)
class NeoForgeMod(modEventBus: IEventBus, modContainer: ModContainer) {
    init {
        ReallyUsefulRibbitsMod.LOGGER.info("Initializing Really Useful Ribbits ({})", ReallyUsefulRibbitsMod.MOD_ID)
        NeoForgeRuntime.install()
        ModAttachments.ATTACHMENT_TYPES.register(modEventBus)
        migrateLegacyConfig()
        modContainer.registerConfig(ModConfig.Type.SERVER, ServerConfig.SPEC)
        ModSetup.init(modEventBus, modContainer)
        ModNetworking.init(modEventBus)
        NeoForgeRuntime.register(modEventBus)
    }

    /** Серверный конфиг NeoForge читает при запуске мира, позже конструктора мода. */
    private fun migrateLegacyConfig() {
        val file = FMLPaths.CONFIGDIR.get().resolve("${ReallyUsefulRibbitsMod.MOD_ID}-server.toml")
        if (!Files.isRegularFile(file)) return
        runCatching {
            val migrated = ServerSettingsText.migrateLegacyToml(Files.readString(file)) ?: return
            Files.writeString(file, migrated)
            ReallyUsefulRibbitsMod.LOGGER.info("Moved untouched old sorcerer defaults in {} to the current ones", file.fileName)
        }.onFailure { ReallyUsefulRibbitsMod.LOGGER.warn("Could not update {}", file, it) }
    }
}
