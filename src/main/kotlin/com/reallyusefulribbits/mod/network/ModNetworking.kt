package com.reallyusefulribbits.mod.network

import com.reallyusefulribbits.mod.client.ClientBeams
import com.reallyusefulribbits.mod.client.ClientDoom
import com.reallyusefulribbits.mod.client.ClientVisuals
import com.reallyusefulribbits.mod.spell.HandSpells
import net.minecraft.server.level.ServerPlayer
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent

object ModNetworking {
    fun init(modBus: IEventBus) {
        modBus.addListener(::onRegister)
    }

    private fun onRegister(event: RegisterPayloadHandlersEvent) {
        val registrar = event.registrar("2")
        registrar.playToClient(PlayerVisualPayload.TYPE, PlayerVisualPayload.STREAM_CODEC) { payload, context ->
            context.enqueueWork { ClientVisuals.set(payload) }
        }
        registrar.playToClient(LightningBeamPayload.TYPE, LightningBeamPayload.STREAM_CODEC) { payload, context ->
            context.enqueueWork { ClientBeams.add(payload.from, payload.to, payload.seed) }
        }
        registrar.playToClient(DoomSkyPayload.TYPE, DoomSkyPayload.STREAM_CODEC) { payload, context ->
            context.enqueueWork { ClientDoom.active = payload.active }
        }
        registrar.playToServer(HandCastPayload.TYPE, HandCastPayload.STREAM_CODEC) { payload, context ->
            context.enqueueWork {
                val player = context.player() as? ServerPlayer ?: return@enqueueWork
                HandSpells.handle(player, payload.kind)
            }
        }
    }
}
