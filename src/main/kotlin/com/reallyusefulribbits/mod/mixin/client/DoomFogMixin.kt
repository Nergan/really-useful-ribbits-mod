package com.reallyusefulribbits.mod.mixin.client

import com.reallyusefulribbits.mod.client.ClientDoom
import net.minecraft.client.renderer.DimensionSpecialEffects
import net.minecraft.world.phys.Vec3
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable

@Mixin(DimensionSpecialEffects.OverworldEffects::class)
abstract class DoomFogMixin {
    @Inject(method = ["getBrightnessDependentFogColor"], at = [At("RETURN")], cancellable = true)
    private fun `reallyusefulribbits$bloodFog`(color: Vec3, brightness: Float, cir: CallbackInfoReturnable<Vec3>) {
        if (ClientDoom.active) cir.returnValue = ClientDoom.fog(cir.returnValue)
    }
}
