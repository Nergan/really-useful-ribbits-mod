package com.reallyusefulribbits.mod.loader.fabric.mixin

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import com.reallyusefulribbits.mod.client.ClientVisuals
import com.reallyusefulribbits.mod.client.MorphRenderer
import net.minecraft.client.player.AbstractClientPlayer
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.entity.player.PlayerRenderer
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@Mixin(PlayerRenderer::class)
abstract class FabricClientRenderMixin {
    @Inject(
        method = ["render(Lnet/minecraft/client/player/AbstractClientPlayer;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"],
        at = [At("HEAD")],
        cancellable = true,
    )
    private fun `reallyusefulribbits$morph`(
        player: AbstractClientPlayer,
        yaw: Float,
        partialTick: Float,
        pose: PoseStack,
        buffer: MultiBufferSource,
        light: Int,
        ci: CallbackInfo,
    ) {
        if (ClientVisuals.morphType(player.uuid) == null) return
        ci.cancel()
        MorphRenderer.render(player, pose, partialTick, buffer, light)
    }

    @Inject(
        method = ["render(Lnet/minecraft/client/player/AbstractClientPlayer;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"],
        at = [At("HEAD")],
    )
    private fun `reallyusefulribbits$pushUpsideDown`(
        player: AbstractClientPlayer,
        yaw: Float,
        partialTick: Float,
        pose: PoseStack,
        buffer: MultiBufferSource,
        light: Int,
        ci: CallbackInfo,
    ) {
        if (!ClientVisuals.rendersUpsideDown(player)) return
        pose.pushPose()
        pose.translate(0.0, player.bbHeight.toDouble(), 0.0)
        pose.mulPose(Axis.ZP.rotationDegrees(180.0f))
    }

    @Inject(
        method = ["render(Lnet/minecraft/client/player/AbstractClientPlayer;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"],
        at = [At("RETURN")],
    )
    private fun `reallyusefulribbits$popUpsideDown`(
        player: AbstractClientPlayer,
        yaw: Float,
        partialTick: Float,
        pose: PoseStack,
        buffer: MultiBufferSource,
        light: Int,
        ci: CallbackInfo,
    ) {
        if (ClientVisuals.rendersUpsideDown(player)) pose.popPose()
    }
}
