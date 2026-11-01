package com.reallyusefulribbits.mod.mixin

import com.reallyusefulribbits.mod.spell.GiantScale
import net.minecraft.world.entity.Entity
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable

/** Хитбокс 90×290×90 блоков: перебор жидкостей и блоков внутри стоил бы миллионы проверок за тик. */
@Mixin(Entity::class)
abstract class GiantEntityMixin {
    @Inject(method = ["updateInWaterStateAndDoFluidPushing"], at = [At("HEAD")], cancellable = true)
    private fun `reallyusefulribbits$giantSkipsFluids`(cir: CallbackInfoReturnable<Boolean>) {
        if (GiantScale.isGiant((this as Any) as Entity)) cir.returnValue = false
    }

    @Inject(method = ["checkInsideBlocks"], at = [At("HEAD")], cancellable = true)
    private fun `reallyusefulribbits$giantSkipsInsideBlocks`(ci: CallbackInfo) {
        if (GiantScale.isGiant((this as Any) as Entity)) ci.cancel()
    }
}
