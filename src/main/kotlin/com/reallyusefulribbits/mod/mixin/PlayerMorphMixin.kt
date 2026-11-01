package com.reallyusefulribbits.mod.mixin

import com.reallyusefulribbits.mod.morph.PlayerMorph
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityDimensions
import net.minecraft.world.entity.Pose
import net.minecraft.world.entity.player.Player
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable

/**
 * В 1.21.1 `getDimensions` объявлен у [Entity], у [Player] его нет.
 * Высота глаз берётся из [EntityDimensions.eyeHeight], отдельного
 * `getEyeHeight(Pose, EntityDimensions)` больше нет.
 */
@Mixin(Entity::class)
abstract class PlayerMorphMixin {
    @Inject(
        method = ["getDimensions(Lnet/minecraft/world/entity/Pose;)Lnet/minecraft/world/entity/EntityDimensions;"],
        at = [At("HEAD")],
        cancellable = true,
    )
    private fun `reallyusefulribbits$morphDimensions`(pose: Pose, cir: CallbackInfoReturnable<EntityDimensions>) {
        val player = (this as Any) as? Player ?: return
        PlayerMorph.dimensionsOf(player, pose)?.let { cir.returnValue = it }
    }
}
