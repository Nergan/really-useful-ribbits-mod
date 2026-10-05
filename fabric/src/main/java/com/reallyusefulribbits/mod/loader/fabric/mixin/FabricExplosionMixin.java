package com.reallyusefulribbits.mod.loader.fabric.mixin;

import com.reallyusefulribbits.mod.profession.SorcererEvents;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Explosion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class FabricExplosionMixin {
    @Inject(method = "ignoreExplosion", at = @At("HEAD"), cancellable = true)
    private void reallyusefulribbits$sorcererIgnores(Explosion explosion, CallbackInfoReturnable<Boolean> cir) {
        List<Entity> affected = new ArrayList<>();
        affected.add((Entity) (Object) this);
        SorcererEvents.INSTANCE.protectFromExplosion(affected);
        if (affected.isEmpty()) {
            cir.setReturnValue(true);
        }
    }
}
