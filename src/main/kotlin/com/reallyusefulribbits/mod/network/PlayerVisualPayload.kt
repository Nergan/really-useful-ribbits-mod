package com.reallyusefulribbits.mod.network

import com.reallyusefulribbits.mod.ReallyUsefulRibbitsMod
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.ResourceLocation
import java.util.UUID

data class PlayerVisualPayload(
    val playerId: UUID,
    val upsideDown: Boolean,
    val morphId: String = "",
    val flags: Int = 0,
    val nickname: String = "",
    val nickColor: Int = -1,
    val shader: String = "",
    val timers: String = "",
) : CustomPacketPayload {

    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> = TYPE

    companion object {
        val ID: ResourceLocation = ResourceLocation.fromNamespaceAndPath(ReallyUsefulRibbitsMod.MOD_ID, "player_visual")
        val TYPE: CustomPacketPayload.Type<PlayerVisualPayload> = CustomPacketPayload.Type(ID)

        val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, PlayerVisualPayload> = StreamCodec.of(
            { buf, value ->
                buf.writeUUID(value.playerId)
                buf.writeBoolean(value.upsideDown)
                buf.writeUtf(value.morphId)
                buf.writeVarInt(value.flags)
                buf.writeUtf(value.nickname)
                buf.writeInt(value.nickColor)
                buf.writeUtf(value.shader)
                buf.writeUtf(value.timers)
            },
            { buf ->
                PlayerVisualPayload(
                    playerId = buf.readUUID(),
                    upsideDown = buf.readBoolean(),
                    morphId = buf.readUtf(),
                    flags = buf.readVarInt(),
                    nickname = buf.readUtf(),
                    nickColor = buf.readInt(),
                    shader = buf.readUtf(),
                    timers = if (buf.readableBytes() > 0) buf.readUtf() else "",
                )
            },
        )
    }
}
