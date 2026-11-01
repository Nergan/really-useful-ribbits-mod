package com.reallyusefulribbits.mod.mixin.client

import com.reallyusefulribbits.mod.client.ClientDoom
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.world.phys.Vec3
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable

/** Пока жив гигантский варден, небо и облака кроваво-красные. */
@Mixin(ClientLevel::class)
abstract class DoomSkyMixin {
    @Inject(method = ["getSkyColor"], at = [At("RETURN")], cancellable = true)
    private fun `reallyusefulribbits$bloodSky`(pos: Vec3, partialTick: Float, cir: CallbackInfoReturnable<Vec3>) {
        if (ClientDoom.active) cir.returnValue = ClientDoom.sky(cir.returnValue)
    }

    @Inject(method = ["getCloudColor"], at = [At("RETURN")], cancellable = true)
    private fun `reallyusefulribbits$bloodClouds`(partialTick: Float, cir: CallbackInfoReturnable<Vec3>) {
        if (ClientDoom.active) cir.returnValue = ClientDoom.clouds(cir.returnValue)
    }
}
