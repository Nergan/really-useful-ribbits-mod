package com.reallyusefulribbits.mod.mixin

import com.reallyusefulribbits.mod.morph.SorcererCurse
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.scores.PlayerTeam
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable

@Mixin(ServerPlayer::class)
abstract class ServerPlayerNicknameMixin {
    @Inject(method = ["getTabListDisplayName"], at = [At("RETURN")], cancellable = true)
    private fun `reallyusefulribbits$tabNickname`(cir: CallbackInfoReturnable<Component?>) {
        val player = (this as Any) as ServerPlayer
        val nickname = SorcererCurse.nickname(player) ?: return
        cir.returnValue = PlayerTeam.formatNameForTeam(player.team, nickname)
    }
}
