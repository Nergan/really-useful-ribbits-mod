package com.reallyusefulribbits.mod.loader.fabric

import com.reallyusefulribbits.mod.ReallyUsefulRibbitsMod
import com.reallyusefulribbits.mod.event.MorphInteractGuard
import com.reallyusefulribbits.mod.event.PlayerTickHandler
import com.reallyusefulribbits.mod.event.RibbitGuideHandler
import com.reallyusefulribbits.mod.event.RibbitInteractionHandler
import com.reallyusefulribbits.mod.event.RibbitTickHandler
import com.reallyusefulribbits.mod.highlight.HighlightMarkers
import com.reallyusefulribbits.mod.network.PlayerVisualPayload
import com.reallyusefulribbits.mod.profession.RibbitCombat
import com.reallyusefulribbits.mod.profession.SorcererEvents
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.fabricmc.fabric.api.event.player.AttackBlockCallback
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents
import net.fabricmc.fabric.api.event.player.UseBlockCallback
import net.fabricmc.fabric.api.event.player.UseEntityCallback
import net.fabricmc.fabric.api.event.player.UseItemCallback
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents
import net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionResult
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents

class ReallyUsefulRibbitsFabric : ModInitializer {
    override fun onInitialize() {
        ReallyUsefulRibbitsMod.LOGGER.info("Initializing Really Useful Ribbits (Fabric)")
        FabricRuntime.install()
        PayloadTypeRegistry.playS2C().register(PlayerVisualPayload.TYPE, PlayerVisualPayload.STREAM_CODEC)

        ServerTickEvents.END_SERVER_TICK.register { server ->
            RibbitTickHandler.onServerTick(server)
            HighlightMarkers.onServerTick(server)
            for (level in server.allLevels) {
                for (player in level.players()) {
                    PlayerTickHandler.onPlayerTick(player)
                }
                for (ribbit in level.getAllEntities()) {
                    RibbitTickHandler.onEntityTick(ribbit)
                }
            }
        }

        UseEntityCallback.EVENT.register { player, world, hand, entity, _ ->
            if (MorphInteractGuard.blocksEntity(player, entity)) {
                return@register InteractionResult.FAIL
            }
            RibbitInteractionHandler.onEntityInteract(player, entity, hand, world) ?: InteractionResult.PASS
        }
        UseBlockCallback.EVENT.register { player, _, hand, _ ->
            if (MorphInteractGuard.blocksWorld(player)) InteractionResult.FAIL else InteractionResult.PASS
        }
        AttackBlockCallback.EVENT.register { player, _, _, _, _ ->
            if (MorphInteractGuard.blocksWorld(player)) InteractionResult.FAIL else InteractionResult.PASS
        }
        UseItemCallback.EVENT.register { player, _, hand ->
            val stack = player.getItemInHand(hand)
            if (MorphInteractGuard.blocksItem(player, stack)) {
                net.minecraft.world.InteractionResultHolder.fail(stack)
            } else {
                net.minecraft.world.InteractionResultHolder.pass(stack)
            }
        }
        PlayerBlockBreakEvents.BEFORE.register { _, player, _, _, _ ->
            !MorphInteractGuard.blocksWorld(player)
        }

        ServerLivingEntityEvents.ALLOW_DAMAGE.register { entity, source, amount ->
            RibbitCombat.onHurt(entity, amount, source)
            !SorcererEvents.blocksDamage(entity, source)
        }
        ServerLivingEntityEvents.AFTER_DEATH.register { entity, source ->
            val player = entity as? ServerPlayer ?: return@register
            PlayerTickHandler.onDeath(player, source)
        }
        ServerPlayConnectionEvents.JOIN.register { handler, _, _ ->
            val player = handler.player
            PlayerTickHandler.onLogin(player)
            RibbitGuideHandler.onLogin(player)
        }
        ServerPlayConnectionEvents.DISCONNECT.register { handler, _ ->
            PlayerTickHandler.onLoggedOut(handler.player)
        }
        EntityTrackingEvents.START_TRACKING.register { target, watcher ->
            val tracked = target as? ServerPlayer ?: return@register
            PlayerTickHandler.onStartTrack(watcher, tracked)
        }
        ItemGroupEvents.MODIFY_ENTRIES_ALL.register { group, entries ->
            val key = BuiltInRegistries.CREATIVE_MODE_TAB.getResourceKey(group).orElse(null) ?: return@register
            if (!RibbitGuideHandler.acceptsCreativeTab(key.location().namespace)) return@register
            RibbitGuideHandler.creativeBook()?.let { entries.accept(it) }
        }
    }
}
