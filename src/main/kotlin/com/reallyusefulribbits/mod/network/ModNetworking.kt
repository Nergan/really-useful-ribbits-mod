package com.reallyusefulribbits.mod.network

import com.reallyusefulribbits.mod.client.ClientVisuals
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent

object ModNetworking {
    fun init(modBus: IEventBus) {
        modBus.addListener(::onRegister)
    }

    private fun onRegister(event: RegisterPayloadHandlersEvent) {
        val registrar = event.registrar("1")
        registrar.playToClient(
            PlayerVisualPayload.TYPE,
            PlayerVisualPayload.STREAM_CODEC,
        ) { payload, _ ->
            ClientVisuals.set(payload.playerId, payload.upsideDown, payload.morphId)
        }
    }
}
