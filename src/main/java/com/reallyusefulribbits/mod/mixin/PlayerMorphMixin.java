package com.reallyusefulribbits.mod.mixin;

import com.reallyusefulribbits.mod.morph.PlayerMorph;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerMorphMixin {
    @Inject(
        method = "getDimensions(Lnet/minecraft/world/entity/Pose;)Lnet/minecraft/world/entity/EntityDimensions;",
        at = @At("HEAD"),
        cancellable = true
    )
    private void reallyusefulribbits$morphDimensions(Pose pose, CallbackInfoReturnable<EntityDimensions> cir) {
        EntityDimensions dims = PlayerMorph.dimensionsOf((Player) (Object) this, pose);
        if (dims != null) {
            cir.setReturnValue(dims);
        }
    }

    @Inject(
        method = "getEyeHeight(Lnet/minecraft/world/entity/Pose;Lnet/minecraft/world/entity/EntityDimensions;)F",
        at = @At("HEAD"),
        cancellable = true
    )
    private void reallyusefulribbits$morphEyes(Pose pose, EntityDimensions dimensions, CallbackInfoReturnable<Float> cir) {
        Float height = PlayerMorph.eyeHeightOf((Player) (Object) this, pose);
        if (height != null) {
            cir.setReturnValue(height);
        }
    }
}
