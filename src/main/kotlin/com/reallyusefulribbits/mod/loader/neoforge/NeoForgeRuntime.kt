package com.reallyusefulribbits.mod.loader.neoforge

import com.reallyusefulribbits.mod.attach.ModAttachments
import com.reallyusefulribbits.mod.config.ServerConfig
import com.reallyusefulribbits.mod.event.MorphInteractGuard
import com.reallyusefulribbits.mod.highlight.HighlightMarkers
import com.reallyusefulribbits.mod.event.PlayerTickHandler
import com.reallyusefulribbits.mod.event.RibbitGuideHandler
import com.reallyusefulribbits.mod.event.RibbitInteractionHandler
import com.reallyusefulribbits.mod.event.RibbitTickHandler
import com.reallyusefulribbits.mod.loader.RuntimeHooks
import com.reallyusefulribbits.mod.profession.RibbitCombat
import com.reallyusefulribbits.mod.profession.SorcererEvents
import net.neoforged.fml.ModList
import net.neoforged.neoforge.common.NeoForge
import net.neoforged.neoforge.common.util.FakePlayerFactory
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent
import net.neoforged.neoforge.event.entity.player.PlayerEvent
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent
import net.neoforged.neoforge.event.level.BlockEvent
import net.neoforged.neoforge.event.level.ExplosionEvent
import net.neoforged.neoforge.event.tick.EntityTickEvent
import net.neoforged.neoforge.event.tick.PlayerTickEvent
import net.neoforged.neoforge.event.tick.ServerTickEvent
import net.neoforged.neoforge.network.PacketDistributor

object NeoForgeRuntime {
    private const val GUIDE_TAG = "reallyusefulribbits.received_guide"

    fun install() {
        RuntimeHooks.work = { ribbit -> ribbit.getData(ModAttachments.WORK.get()) }
        RuntimeHooks.visual = { player -> player.getData(ModAttachments.VISUAL.get()) }
        RuntimeHooks.sendTrackingAndSelf = { player, payload ->
            PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, payload)
        }
        RuntimeHooks.sendToPlayer = { player, payload ->
            PacketDistributor.sendToPlayer(player, payload)
        }
        RuntimeHooks.scanRadius = { ServerConfig.scanRadius() }
        RuntimeHooks.chaosLevel = { ServerConfig.chaosLevel() }
        RuntimeHooks.itemSlots = { level, pos -> NeoForgeContainers.slots(level, pos) }
        RuntimeHooks.containerOpener = { level -> FakePlayerFactory.getMinecraft(level) }
        RuntimeHooks.patchouliLoaded = { ModList.get().isLoaded("patchouli") }
        RuntimeHooks.guideReceived = { player -> player.persistentData.getBoolean(GUIDE_TAG) }
        RuntimeHooks.markGuideReceived = { player -> player.persistentData.putBoolean(GUIDE_TAG, true) }
    }

    fun register(modBus: net.neoforged.bus.api.IEventBus) {
        val game = NeoForge.EVENT_BUS
        game.addListener<EntityTickEvent.Post> { event -> RibbitTickHandler.onEntityTick(event.entity) }
        game.addListener<ServerTickEvent.Post> { event ->
            RibbitTickHandler.onServerTick(event.server)
            HighlightMarkers.onServerTick(event.server)
        }
        game.addListener<PlayerInteractEvent.EntityInteract> { event ->
            val result = RibbitInteractionHandler.onEntityInteract(
                event.entity,
                event.target,
                event.hand,
                event.level,
            )
            if (result != null) {
                event.isCanceled = true
                event.cancellationResult = result
            }
        }
        game.addListener<PlayerTickEvent.Post> { event -> PlayerTickHandler.onPlayerTick(event.entity) }
        game.addListener<PlayerEvent.PlayerLoggedInEvent> { event ->
            val player = event.entity as? net.minecraft.server.level.ServerPlayer ?: return@addListener
            PlayerTickHandler.onLogin(player)
            RibbitGuideHandler.onLogin(player)
        }
        game.addListener<PlayerEvent.StartTracking> { event ->
            val watcher = event.entity as? net.minecraft.server.level.ServerPlayer ?: return@addListener
            val target = event.target as? net.minecraft.server.level.ServerPlayer ?: return@addListener
            PlayerTickHandler.onStartTrack(watcher, target)
        }
        game.addListener<PlayerEvent.PlayerLoggedOutEvent> { event ->
            val player = event.entity as? net.minecraft.server.level.ServerPlayer ?: return@addListener
            PlayerTickHandler.onLoggedOut(player)
        }
        game.addListener<LivingDeathEvent> { event ->
            val player = event.entity as? net.minecraft.server.level.ServerPlayer ?: return@addListener
            PlayerTickHandler.onDeath(player, event.source)
        }
        game.addListener<LivingIncomingDamageEvent> { event ->
            RibbitCombat.onHurt(event.entity, event.amount, event.source)
            if (SorcererEvents.blocksDamage(event.entity, event.source)) {
                event.isCanceled = true
            }
        }
        game.addListener<ExplosionEvent.Detonate> { event ->
            SorcererEvents.protectFromExplosion(event.affectedEntities)
        }
        game.addListener<PlayerInteractEvent.RightClickBlock> { event ->
            if (MorphInteractGuard.blocksWorld(event.entity)) event.isCanceled = true
        }
        game.addListener<PlayerInteractEvent.LeftClickBlock> { event ->
            if (MorphInteractGuard.blocksWorld(event.entity)) event.isCanceled = true
        }
        game.addListener<PlayerInteractEvent.RightClickItem> { event ->
            if (MorphInteractGuard.blocksItem(event.entity, event.itemStack)) event.isCanceled = true
        }
        game.addListener<PlayerInteractEvent.EntityInteract> { event ->
            if (MorphInteractGuard.blocksEntity(event.entity, event.target)) event.isCanceled = true
        }
        game.addListener<BlockEvent.BreakEvent> { event ->
            if (MorphInteractGuard.blocksWorld(event.player)) event.isCanceled = true
        }
        modBus.addListener<BuildCreativeModeTabContentsEvent> { event ->
            if (!RibbitGuideHandler.acceptsCreativeTab(event.tabKey.location().namespace)) return@addListener
            RibbitGuideHandler.creativeBook()?.let { event.accept(it) }
        }
    }
}
