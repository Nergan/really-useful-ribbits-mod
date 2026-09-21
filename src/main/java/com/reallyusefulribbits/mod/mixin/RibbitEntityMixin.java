package com.reallyusefulribbits.mod.mixin;

import com.yungnickyoung.minecraft.ribbits.entity.RibbitEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Оригинальные profession-цели (рыбалка без улова, полив, музыка nitwit,
 * автобаф чародея) мешают новым работам. Базовые цели из registerGoals
 * (двери, дом, паника, прогулка) остаются.
 */
@Mixin(RibbitEntity.class)
public abstract class RibbitEntityMixin {
    @Inject(method = "reassessGoals", at = @At("HEAD"), cancellable = true)
    private void reallyusefulribbits$skipVanillaJobs(CallbackInfo ci) {
        ci.cancel();
    }
}
