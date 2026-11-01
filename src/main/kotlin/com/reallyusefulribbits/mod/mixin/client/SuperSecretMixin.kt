package com.reallyusefulribbits.mod.mixin.client

import com.reallyusefulribbits.mod.ReallyUsefulRibbitsMod
import com.reallyusefulribbits.mod.client.ClientSpells
import net.minecraft.client.DeltaTracker
import net.minecraft.client.renderer.GameRenderer
import net.minecraft.client.renderer.PostChain
import net.minecraft.resources.ResourceLocation
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.Shadow
import org.spongepowered.asm.mixin.Unique
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

/**
 * Шейдер Super Secret Settings. `checkEntityPostEffect` снимает эффект при смене камеры и F5,
 * поэтому он возвращается в каждом кадре, если пропал.
 */
@Mixin(GameRenderer::class)
abstract class SuperSecretMixin {
    @Shadow
    abstract fun loadEffect(location: ResourceLocation)

    @Shadow
    abstract fun shutdownEffect()

    @Shadow
    abstract fun currentEffect(): PostChain?

    @Unique
    private var `reallyusefulribbits$shader`: String = ""

    @Unique
    private var `reallyusefulribbits$failed`: String = ""

    @Inject(method = ["render"], at = [At("HEAD")])
    private fun `reallyusefulribbits$superSecret`(deltaTracker: DeltaTracker, renderLevel: Boolean, ci: CallbackInfo) {
        val wanted = ClientSpells.wantedShader()
        if (wanted.isNotEmpty()) {
            if (wanted == `reallyusefulribbits$failed`) return
            if (currentEffect()?.name == wanted) return
            val location = ResourceLocation.tryParse(wanted) ?: return
            loadEffect(location)
            if (currentEffect()?.name == wanted) {
                `reallyusefulribbits$shader` = wanted
            } else {
                `reallyusefulribbits$failed` = wanted
                ReallyUsefulRibbitsMod.LOGGER.warn("Super Secret Settings shader {} did not load", wanted)
            }
        } else if (`reallyusefulribbits$shader`.isNotEmpty()) {
            if (currentEffect()?.name == `reallyusefulribbits$shader`) shutdownEffect()
            `reallyusefulribbits$shader` = ""
        }
    }
}
