package com.reallyusefulribbits.mod.mixin.client

import com.reallyusefulribbits.mod.client.ClientSpells
import net.minecraft.client.player.Input
import net.minecraft.client.player.KeyboardInput
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

/** «А как ходить...»: вперёд — назад, влево — вправо. */
@Mixin(KeyboardInput::class)
abstract class InvertedControlsMixin {
    @Inject(method = ["tick"], at = [At("TAIL")])
    private fun `reallyusefulribbits$invert`(slow: Boolean, slowFactor: Float, ci: CallbackInfo) {
        if (!ClientSpells.controlsInverted()) return
        val input = (this as Any) as Input
        input.forwardImpulse = -input.forwardImpulse
        input.leftImpulse = -input.leftImpulse
        val up = input.up
        input.up = input.down
        input.down = up
        val left = input.left
        input.left = input.right
        input.right = left
    }
}
