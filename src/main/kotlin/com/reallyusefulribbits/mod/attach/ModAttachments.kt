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
                    ): PlayerVisualData = PlayerVisualData(
                        upsideDown = tag.getBoolean("UpsideDown"),
                        flightTicks = tag.getInt("FlightTicks"),
                        morphId = tag.getString("MorphId"),
                        morphFlight = tag.getBoolean("MorphFlight"),
                        spectatorTicks = tag.getInt("SpectatorTicks"),
                        previousGameMode = tag.getString("PrevGameMode").ifEmpty { "survival" },
                        attributesSaved = tag.getBoolean("AttrsSaved"),
                        savedMaxHealth = if (tag.contains("SavedMaxHealth")) tag.getDouble("SavedMaxHealth") else 20.0,
                        savedScale = if (tag.contains("SavedScale")) tag.getDouble("SavedScale") else 1.0,
                        savedSpeed = if (tag.contains("SavedSpeed")) tag.getDouble("SavedSpeed") else 0.1,
                        savedAttack = if (tag.contains("SavedAttack")) tag.getDouble("SavedAttack") else 1.0,
                        savedKnockback = tag.getDouble("SavedKnockback"),
                        savedStep = if (tag.contains("SavedStep")) tag.getDouble("SavedStep") else 0.6,
                    )

                    override fun write(attachment: PlayerVisualData, provider: HolderLookup.Provider): CompoundTag {
                        val out = CompoundTag()
                        out.putBoolean("UpsideDown", attachment.upsideDown)
                        out.putInt("FlightTicks", attachment.flightTicks)
                        out.putString("MorphId", attachment.morphId)
                        out.putBoolean("MorphFlight", attachment.morphFlight)
                        out.putInt("SpectatorTicks", attachment.spectatorTicks)
                        out.putString("PrevGameMode", attachment.previousGameMode)
                        out.putBoolean("AttrsSaved", attachment.attributesSaved)
                        out.putDouble("SavedMaxHealth", attachment.savedMaxHealth)
                        out.putDouble("SavedScale", attachment.savedScale)
                        out.putDouble("SavedSpeed", attachment.savedSpeed)
                        out.putDouble("SavedAttack", attachment.savedAttack)
                        out.putDouble("SavedKnockback", attachment.savedKnockback)
                        out.putDouble("SavedStep", attachment.savedStep)
                        return out
                    }
                })
                .build()
        },
    )
}
