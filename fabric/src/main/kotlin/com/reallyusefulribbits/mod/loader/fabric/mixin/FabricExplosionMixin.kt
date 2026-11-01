package com.reallyusefulribbits.mod.loader.fabric.mixin

import com.reallyusefulribbits.mod.profession.SorcererEvents
import net.minecraft.world.entity.Entity
import net.minecraft.world.level.Explosion
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable

@Mixin(Entity::class)
abstract class FabricExplosionMixin {
    @Inject(method = ["ignoreExplosion"], at = [At("HEAD")], cancellable = true)
    private fun `reallyusefulribbits$sorcererIgnores`(explosion: Explosion, cir: CallbackInfoReturnable<Boolean>) {
        val affected = mutableListOf((this as Any) as Entity)
        SorcererEvents.protectFromExplosion(affected)
        if (affected.isEmpty()) cir.returnValue = true
    }
}
