package com.reallyusefulribbits.mod.network

import com.reallyusefulribbits.mod.ReallyUsefulRibbitsMod
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.phys.Vec3

/** Клиент просит заклинание руки: огненный шар, молнию или переворот гравитации. */
data class HandCastPayload(val kind: Int) : CustomPacketPayload {
    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> = TYPE

    companion object {
        const val HAND = 1
        const val FLIP = 2

        val TYPE: CustomPacketPayload.Type<HandCastPayload> =
            CustomPacketPayload.Type(ResourceLocation.fromNamespaceAndPath(ReallyUsefulRibbitsMod.MOD_ID, "hand_cast"))

        val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, HandCastPayload> = StreamCodec.of(
            { buf, value -> buf.writeVarInt(value.kind) },
            { buf -> HandCastPayload(buf.readVarInt()) },
        )
    }
}

/** Молния из руки: от игрока по прямой до точки удара. */
data class LightningBeamPayload(val from: Vec3, val to: Vec3, val seed: Long) : CustomPacketPayload {
    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> = TYPE

    companion object {
        val TYPE: CustomPacketPayload.Type<LightningBeamPayload> =
            CustomPacketPayload.Type(ResourceLocation.fromNamespaceAndPath(ReallyUsefulRibbitsMod.MOD_ID, "lightning_beam"))

        val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, LightningBeamPayload> = StreamCodec.of(
            { buf, value ->
                buf.writeVec3(value.from)
                buf.writeVec3(value.to)
                buf.writeLong(value.seed)
            },
            { buf -> LightningBeamPayload(buf.readVec3(), buf.readVec3(), buf.readLong()) },
        )
    }
}

/** Пока жив гигантский варден, небо у всех кроваво-красное. */
data class DoomSkyPayload(val active: Boolean) : CustomPacketPayload {
    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> = TYPE

    companion object {
        val TYPE: CustomPacketPayload.Type<DoomSkyPayload> =
            CustomPacketPayload.Type(ResourceLocation.fromNamespaceAndPath(ReallyUsefulRibbitsMod.MOD_ID, "doom_sky"))

        val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, DoomSkyPayload> = StreamCodec.of(
            { buf, value -> buf.writeBoolean(value.active) },
            { buf -> DoomSkyPayload(buf.readBoolean()) },
        )
    }
}
