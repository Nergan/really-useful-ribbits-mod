package com.reallyusefulribbits.mod.mixin

import com.reallyusefulribbits.mod.spell.GiantScale
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.ai.navigation.PathNavigation
import net.minecraft.world.level.pathfinder.Node
import net.minecraft.world.level.pathfinder.Path
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable

/** Поиск пути проверяет каждый блок под шириной моба. Гигант идёт к цели по прямой. */
@Mixin(PathNavigation::class)
abstract class GiantPathMixin {
    @Inject(
        method = ["createPath(Ljava/util/Set;IZIF)Lnet/minecraft/world/level/pathfinder/Path;"],
        at = [At("HEAD")],
        cancellable = true,
    )
    private fun `reallyusefulribbits$giantWalksStraight`(
        targets: Set<BlockPos>,
        regionOffset: Int,
        offsetUpward: Boolean,
        accuracy: Int,
        followRange: Float,
        cir: CallbackInfoReturnable<Path?>,
    ) {
        val mob = ((this as Any) as PathNavigationAccessor).rurMob()
        if (!GiantScale.isGiant(mob) || targets.isEmpty()) return
        val target = targets.first()
        val start = mob.blockPosition()
        cir.returnValue = Path(mutableListOf(Node(start.x, start.y, start.z), Node(target.x, target.y, target.z)), target, true)
    }
}
