package com.reallyusefulribbits.mod.loader.fabric.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reallyusefulribbits.mod.client.ClientVisuals;
import com.reallyusefulribbits.mod.morph.PlayerMorph;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public abstract class FabricHandMixin {
    @Inject(method = "renderArmWithItem", at = @At("HEAD"), cancellable = true)
    private void reallyusefulribbits$hideNonHumanoidHand(
        AbstractClientPlayer player,
        float partialTick,
        float pitch,
        InteractionHand hand,
        float swingProgress,
        ItemStack stack,
        float equippedProgress,
        PoseStack pose,
        MultiBufferSource buffer,
        int light,
        CallbackInfo ci
    ) {
        Player local = Minecraft.getInstance().player;
        if (local == null) {
            return;
        }
        EntityType<?> type = ClientVisuals.INSTANCE.morphType(local.getUUID());
        if (type != null && !PlayerMorph.INSTANCE.isHumanoidType(type)) {
            ci.cancel();
        }
    }
}
