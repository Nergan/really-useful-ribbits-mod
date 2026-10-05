package com.reallyusefulribbits.mod.loader.fabric

import com.reallyusefulribbits.mod.network.PlayerVisualPayload
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking

class ReallyUsefulRibbitsFabricClient : ClientModInitializer {
    override fun onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(PlayerVisualPayload.TYPE) { payload, context ->
            context.client().execute { FabricRuntime.onClientPayload(payload) }
        }
    }
}
