package com.reallyusefulribbits.mod.mixin;

import com.yungnickyoung.minecraft.ribbits.entity.RibbitEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerHeadRideMixin {
    @Inject(method = "getPassengerAttachmentPoint", at = @At("HEAD"), cancellable = true)
    private void reallyusefulribbits$ribbitOnHead(
        Entity passenger,
        EntityDimensions dimensions,
        float scale,
        CallbackInfoReturnable<Vec3> cir
    ) {
        if (passenger instanceof RibbitEntity) {
            cir.setReturnValue(new Vec3(0.0, dimensions.height() + 0.15f * scale, 0.0));
        }
    }
}
