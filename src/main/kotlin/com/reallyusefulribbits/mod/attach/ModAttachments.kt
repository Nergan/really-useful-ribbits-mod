package com.reallyusefulribbits.mod.attach

import com.reallyusefulribbits.mod.ReallyUsefulRibbitsMod
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.neoforged.neoforge.attachment.AttachmentType
import net.neoforged.neoforge.attachment.IAttachmentHolder
import net.neoforged.neoforge.attachment.IAttachmentSerializer
import net.neoforged.neoforge.registries.DeferredRegister
import net.neoforged.neoforge.registries.NeoForgeRegistries
import java.util.function.Supplier

object ModAttachments {
    val ATTACHMENT_TYPES: DeferredRegister<AttachmentType<*>> =
        DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, ReallyUsefulRibbitsMod.MOD_ID)

    val WORK: Supplier<AttachmentType<RibbitWorkData>> = ATTACHMENT_TYPES.register(
        "work",
        Supplier {
            AttachmentType.builder(::RibbitWorkData)
                .serialize(object : IAttachmentSerializer<CompoundTag, RibbitWorkData> {
                    override fun read(
                        holder: IAttachmentHolder,
                        tag: CompoundTag,
                        provider: HolderLookup.Provider,
                    ): RibbitWorkData = RibbitWorkData.load(tag, provider)

                    override fun write(attachment: RibbitWorkData, provider: HolderLookup.Provider): CompoundTag =
                        attachment.save(provider)
                })
                .build()
        },
    )

    val VISUAL: Supplier<AttachmentType<PlayerVisualData>> = ATTACHMENT_TYPES.register(
        "visual",
        Supplier {
            AttachmentType.builder(::PlayerVisualData)
                .serialize(object : IAttachmentSerializer<CompoundTag, PlayerVisualData> {
                    override fun read(
                        holder: IAttachmentHolder,
                        tag: CompoundTag,
                        provider: HolderLookup.Provider,
                    ): PlayerVisualData = PlayerVisualData(tag.getBoolean("UpsideDown"))

                    override fun write(attachment: PlayerVisualData, provider: HolderLookup.Provider): CompoundTag {
                        val out = CompoundTag()
                        out.putBoolean("UpsideDown", attachment.upsideDown)
                        return out
                    }
                })
                .copyOnDeath()
                .build()
        },
    )
}
