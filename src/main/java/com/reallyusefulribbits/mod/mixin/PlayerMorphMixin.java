package com.reallyusefulribbits.mod.mixin;

import com.reallyusefulribbits.mod.morph.PlayerMorph;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * В 1.21.1 {@code getDimensions} объявлен у {@link Entity}, у {@link Player} его нет.
 * Высота глаз берётся из {@link EntityDimensions#eyeHeight}, отдельного
 * {@code getEyeHeight(Pose, EntityDimensions)} больше нет.
 */
@Mixin(Entity.class)
public abstract class PlayerMorphMixin {
    @Inject(
        method = "getDimensions(Lnet/minecraft/world/entity/Pose;)Lnet/minecraft/world/entity/EntityDimensions;",
        at = @At("HEAD"),
        cancellable = true
    )
    private void reallyusefulribbits$morphDimensions(Pose pose, CallbackInfoReturnable<EntityDimensions> cir) {
        Entity self = (Entity) (Object) this;
        if (!(self instanceof Player player)) {
            return;
        }
        EntityDimensions dims = PlayerMorph.dimensionsOf(player, pose);
        if (dims != null) {
            cir.setReturnValue(dims);
        }
    }
}
