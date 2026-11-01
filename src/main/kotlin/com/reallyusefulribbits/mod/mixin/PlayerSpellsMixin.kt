package com.reallyusefulribbits.mod.mixin

import com.reallyusefulribbits.mod.loader.RuntimeHooks
import com.reallyusefulribbits.mod.morph.SorcererCurse
import net.minecraft.network.chat.Component
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.player.Player
import net.minecraft.world.scores.PlayerTeam
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable

@Mixin(Player::class)
abstract class PlayerSpellsMixin {
    /** Ник чародея — в чате, над головой и у сервера для списка игроков. */
    @Inject(method = ["getDisplayName"], at = [At("RETURN")], cancellable = true)
    private fun `reallyusefulribbits$nickname`(cir: CallbackInfoReturnable<Component>) {
        val player = (this as Any) as Player
        val nickname = SorcererCurse.nickname(player) ?: return
        val original = cir.returnValue ?: return
        cir.returnValue = PlayerTeam.formatNameForTeam(player.team, nickname).withStyle(original.style)
    }

    /** VVVVVV: падение на потолок и обратно не ранит. */
    @Inject(method = ["causeFallDamage"], at = [At("HEAD")], cancellable = true)
    private fun `reallyusefulribbits$gravityFall`(distance: Float, multiplier: Float, source: DamageSource, cir: CallbackInfoReturnable<Boolean>) {
        val player = (this as Any) as Player
        if (!player.level().isClientSide && RuntimeHooks.visual(player).vvvvvv) cir.returnValue = false
    }
}
