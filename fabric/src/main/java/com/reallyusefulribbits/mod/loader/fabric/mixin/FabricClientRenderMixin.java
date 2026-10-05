package com.reallyusefulribbits.mod.loader.fabric.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.reallyusefulribbits.mod.client.ClientVisuals;
import com.reallyusefulribbits.mod.client.MorphRenderer;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerRenderer.class)
public abstract class FabricClientRenderMixin {
    @Inject(method = "render(Lnet/minecraft/client/player/AbstractClientPlayer;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @At("HEAD"), cancellable = true)
    private void reallyusefulribbits$morph(
        AbstractClientPlayer player,
        float yaw,
        float partialTick,
        PoseStack pose,
        MultiBufferSource buffer,
        int light,
        CallbackInfo ci
    ) {
        if (ClientVisuals.INSTANCE.morphType(player.getUUID()) == null) {
            return;
        }
        ci.cancel();
        MorphRenderer.INSTANCE.render(player, pose, partialTick, buffer, light);
    }

    @Inject(method = "render(Lnet/minecraft/client/player/AbstractClientPlayer;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @At("HEAD"))
    private void reallyusefulribbits$pushUpsideDown(
        AbstractClientPlayer player,
        float yaw,
        float partialTick,
        PoseStack pose,
        MultiBufferSource buffer,
        int light,
        CallbackInfo ci
    ) {
        if (!upsideDown(player)) {
            return;
        }
        pose.pushPose();
        pose.translate(0.0, player.getBbHeight(), 0.0);
        pose.mulPose(Axis.ZP.rotationDegrees(180.0F));
    }

    @Inject(method = "render(Lnet/minecraft/client/player/AbstractClientPlayer;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @At("RETURN"))
    private void reallyusefulribbits$popUpsideDown(
        AbstractClientPlayer player,
        float yaw,
        float partialTick,
        PoseStack pose,
        MultiBufferSource buffer,
        int light,
        CallbackInfo ci
    ) {
        if (upsideDown(player)) {
            pose.popPose();
        }
    }

    private static boolean upsideDown(AbstractClientPlayer player) {
        return ClientVisuals.INSTANCE.morphType(player.getUUID()) == null
            && ClientVisuals.INSTANCE.isUpsideDown(player.getUUID());
    }
}
