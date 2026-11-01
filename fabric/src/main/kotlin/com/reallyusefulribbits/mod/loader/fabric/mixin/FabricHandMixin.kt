package com.reallyusefulribbits.mod.loader.fabric.mixin

import com.mojang.blaze3d.vertex.PoseStack
import com.reallyusefulribbits.mod.client.ClientVisuals
import com.reallyusefulribbits.mod.morph.PlayerMorph
import net.minecraft.client.Minecraft
import net.minecraft.client.player.AbstractClientPlayer
import net.minecraft.client.renderer.ItemInHandRenderer
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.world.InteractionHand
import net.minecraft.world.item.ItemStack
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@Mixin(ItemInHandRenderer::class)
abstract class FabricHandMixin {
    @Inject(method = ["renderArmWithItem"], at = [At("HEAD")], cancellable = true)
    private fun `reallyusefulribbits$hideNonHumanoidHand`(
        player: AbstractClientPlayer,
        partialTick: Float,
        pitch: Float,
        hand: InteractionHand,
        swingProgress: Float,
        stack: ItemStack,
        equippedProgress: Float,
        pose: PoseStack,
        buffer: MultiBufferSource,
        light: Int,
        ci: CallbackInfo,
    ) {
        val local = Minecraft.getInstance().player ?: return
        val type = ClientVisuals.morphType(local.uuid) ?: return
        if (!PlayerMorph.isHumanoidType(type)) ci.cancel()
    }
}
