package com.reallyusefulribbits.mod.loader.fabric

import com.reallyusefulribbits.mod.client.BeamRenderer
import com.reallyusefulribbits.mod.client.ClientBeams
import com.reallyusefulribbits.mod.client.ClientDoom
import com.reallyusefulribbits.mod.client.ClientSpells
import com.reallyusefulribbits.mod.client.ClientVisuals
import com.reallyusefulribbits.mod.loader.RuntimeHooks
import com.reallyusefulribbits.mod.network.DoomSkyPayload
import com.reallyusefulribbits.mod.network.LightningBeamPayload
import com.reallyusefulribbits.mod.network.PlayerVisualPayload
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents

class ReallyUsefulRibbitsFabricClient : ClientModInitializer {
    override fun onInitializeClient() {
        RuntimeHooks.sendToServer = { payload -> ClientPlayNetworking.send(payload) }
        ClientPlayNetworking.registerGlobalReceiver(PlayerVisualPayload.TYPE) { payload, context ->
            context.client().execute { ClientVisuals.set(payload) }
        }
        ClientPlayNetworking.registerGlobalReceiver(LightningBeamPayload.TYPE) { payload, context ->
            context.client().execute { ClientBeams.add(payload.from, payload.to, payload.seed) }
        }
        ClientPlayNetworking.registerGlobalReceiver(DoomSkyPayload.TYPE) { payload, context ->
            context.client().execute { ClientDoom.active = payload.active }
        }
        ClientTickEvents.START_CLIENT_TICK.register { client -> ClientSpells.startTick(client) }
        ClientTickEvents.END_CLIENT_TICK.register { client -> ClientSpells.endTick(client) }
        WorldRenderEvents.AFTER_TRANSLUCENT.register { context ->
            BeamRenderer.render(context.tickCounter().getGameTimeDeltaPartialTick(false))
        }
    }
}
