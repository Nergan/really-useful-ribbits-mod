package com.reallyusefulribbits.mod.world

import com.reallyusefulribbits.mod.ReallyUsefulRibbitsMod
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceLocation
import net.minecraft.tags.TagKey
import net.minecraft.world.level.block.Block

object ModTags {
    val FARMLAND: TagKey<Block> = TagKey.create(
        Registries.BLOCK,
        ResourceLocation.fromNamespaceAndPath(ReallyUsefulRibbitsMod.MOD_ID, "farmland"),
    )
    val TILLABLE: TagKey<Block> = TagKey.create(
        Registries.BLOCK,
        ResourceLocation.fromNamespaceAndPath(ReallyUsefulRibbitsMod.MOD_ID, "tillable"),
    )
}
