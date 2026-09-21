package com.reallyusefulribbits.mod.mixin;

import com.yungnickyoung.minecraft.ribbits.entity.RibbitEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Точка посадки пассажира в 1.21.1 объявлена у {@link Entity}.
 */
@Mixin(Entity.class)
public abstract class EntityHeadRideMixin {
    @Inject(
        method = "getPassengerAttachmentPoint(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/EntityDimensions;F)Lnet/minecraft/world/phys/Vec3;",
        at = @At("HEAD"),
        cancellable = true
    )
    private void reallyusefulribbits$ribbitOnHead(
        Entity passenger,
        EntityDimensions dimensions,
        float scale,
        CallbackInfoReturnable<Vec3> cir
    ) {
        Entity self = (Entity) (Object) this;
        if (self instanceof Player && passenger instanceof RibbitEntity) {
            cir.setReturnValue(new Vec3(0.0, dimensions.height() + 0.15f * scale, 0.0));
        }
    }

    @Inject(
        method = "positionRider(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/Entity$MoveFunction;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void reallyusefulribbits$holdRibbitOnHead(Entity passenger, Entity.MoveFunction callback, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (self instanceof Player player && passenger instanceof RibbitEntity) {
            double x = player.getX();
            double y = player.getY() + player.getBbHeight() + 0.15;
            double z = player.getZ();
            callback.accept(passenger, x, y, z);
            ci.cancel();
        }
    }
}
