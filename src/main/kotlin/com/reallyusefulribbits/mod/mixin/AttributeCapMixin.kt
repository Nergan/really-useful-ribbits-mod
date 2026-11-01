package com.reallyusefulribbits.mod.mixin

import com.reallyusefulribbits.mod.spell.GiantScale
import net.minecraft.world.entity.ai.attributes.RangedAttribute
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable

/** Ванильный потолок — размер 16 и здоровье 1024. Варден в сто раз больше с 10 000 здоровья в них не влезает. */
@Mixin(RangedAttribute::class)
abstract class AttributeCapMixin {
    @Inject(method = ["sanitizeValue"], at = [At("HEAD")], cancellable = true)
    private fun `reallyusefulribbits$raiseCap`(value: Double, cir: CallbackInfoReturnable<Double>) {
        val attribute = (this as Any) as RangedAttribute
        val cap = GiantScale.capFor(attribute) ?: return
        cir.returnValue = if (value.isNaN()) attribute.minValue else value.coerceIn(attribute.minValue, cap)
    }
}
