package com.reallyusefulribbits.mod.mixin

import com.reallyusefulribbits.mod.logic.ProfessionKind
import com.reallyusefulribbits.mod.util.professionKind
import com.yungnickyoung.minecraft.ribbits.entity.RibbitEntity
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.Entity
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.FarmBlock
import net.minecraft.world.level.block.state.BlockState
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

/** Фермер может прыгать по пашне: грядка остаётся грядкой, урон от падения считается отдельно. */
@Mixin(FarmBlock::class)
abstract class FarmlandTrampleMixin {
    @Inject(method = ["fallOn"], at = [At("HEAD")], cancellable = true)
    @Suppress("UNUSED_PARAMETER")
    private fun farmerDoesNotTrample(
        level: Level,
        state: BlockState,
        pos: BlockPos,
        entity: Entity,
        fallDistance: Float,
        ci: CallbackInfo,
    ) {
        val ribbit = entity as? RibbitEntity ?: return
        if (ribbit.professionKind() != ProfessionKind.FARMER) return
        entity.causeFallDamage(fallDistance, 1.0f, entity.damageSources().fall())
        ci.cancel()
    }
}
