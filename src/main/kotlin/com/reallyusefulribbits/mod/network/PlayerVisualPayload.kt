package com.reallyusefulribbits.mod.network

import com.reallyusefulribbits.mod.ReallyUsefulRibbitsMod
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.ResourceLocation
import java.util.UUID

data class PlayerVisualPayload(
    val playerId: UUID,
    val upsideDown: Boolean,
    val morphId: String = "",
) : CustomPacketPayload {

    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> = TYPE

    companion object {
        val ID: ResourceLocation = ResourceLocation.fromNamespaceAndPath(ReallyUsefulRibbitsMod.MOD_ID, "player_visual")
        val TYPE: CustomPacketPayload.Type<PlayerVisualPayload> = CustomPacketPayload.Type(ID)

        private val UUID_CODEC: StreamCodec<RegistryFriendlyByteBuf, UUID> = StreamCodec.of(
            { buf, value -> buf.writeUUID(value) },
            { buf -> buf.readUUID() },
        )

        val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, PlayerVisualPayload> = StreamCodec.composite(
            UUID_CODEC,
            PlayerVisualPayload::playerId,
            ByteBufCodecs.BOOL,
            PlayerVisualPayload::upsideDown,
            ByteBufCodecs.STRING_UTF8,
            PlayerVisualPayload::morphId,
            ::PlayerVisualPayload,
        )
    }
}
