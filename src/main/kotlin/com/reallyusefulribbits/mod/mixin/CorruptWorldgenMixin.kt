package com.reallyusefulribbits.mod.mixin

import com.reallyusefulribbits.mod.spell.WorldCorruption
import net.minecraft.world.level.StructureManager
import net.minecraft.world.level.WorldGenLevel
import net.minecraft.world.level.chunk.ChunkAccess
import net.minecraft.world.level.chunk.ChunkGenerator
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@Mixin(ChunkGenerator::class)
abstract class CorruptWorldgenMixin {
    @Inject(method = ["applyBiomeDecoration"], at = [At("TAIL")])
    private fun `reallyusefulribbits$corrupt`(level: WorldGenLevel, chunk: ChunkAccess, structures: StructureManager, ci: CallbackInfo) {
        if (WorldCorruption.active) WorldCorruption.corrupt(level, chunk)
    }
}
