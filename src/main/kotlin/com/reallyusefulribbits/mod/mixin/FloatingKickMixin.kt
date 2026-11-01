package com.reallyusefulribbits.mod.mixin

import com.reallyusefulribbits.mod.loader.RuntimeHooks
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.network.ServerGamePacketListenerImpl
import net.minecraft.world.entity.Entity
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable

/** При `allow-flight=false` сервер выкидывает того, кто долго падает вверх. VVVVVV — не полёт. */
@Mixin(ServerGamePacketListenerImpl::class)
abstract class FloatingKickMixin {
    @Inject(method = ["noBlocksAround"], at = [At("HEAD")], cancellable = true)
    private fun `reallyusefulribbits$gravityIsNotFlight`(entity: Entity, cir: CallbackInfoReturnable<Boolean>) {
        val player = entity as? ServerPlayer ?: return
        if (RuntimeHooks.visual(player).vvvvvv) cir.returnValue = false
    }
}
