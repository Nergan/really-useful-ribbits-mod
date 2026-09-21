package com.reallyusefulribbits.mod.client

import com.mojang.math.Axis
import net.minecraft.client.Minecraft
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.neoforged.neoforge.client.event.RenderPlayerEvent

object MorphRenderer {
    fun render(event: RenderPlayerEvent.Pre, player: Player) {
        val type = ClientVisuals.morphType(player.uuid) ?: return
        val dummy = type.create(player.level()) as? LivingEntity ?: return
        dummy.tickCount = player.tickCount
        dummy.yRot = player.yRot
        dummy.xRot = player.xRot
        dummy.yHeadRot = player.yHeadRot
        dummy.yBodyRot = player.yBodyRot
        dummy.yHeadRotO = player.yHeadRotO
        dummy.yBodyRotO = player.yBodyRotO
        dummy.hurtTime = player.hurtTime
        dummy.attackAnim = player.attackAnim
        dummy.oAttackAnim = player.oAttackAnim
        dummy.setSprinting(player.isSprinting)
        dummy.setSwimming(player.isSwimming)
        dummy.pose = player.pose
        dummy.setOnGround(player.onGround())
        dummy.isShiftKeyDown = player.isShiftKeyDown
        dummy.setPos(player.x, player.y, player.z)
        dummy.xo = player.x
        dummy.yo = player.y
        dummy.zo = player.z
        dummy.xOld = player.xOld
        dummy.yOld = player.yOld
        dummy.zOld = player.zOld
        val pose = event.poseStack
        pose.pushPose()
        if (ClientVisuals.isUpsideDown(player.uuid)) {
            pose.translate(0.0, player.bbHeight.toDouble(), 0.0)
            pose.mulPose(Axis.ZP.rotationDegrees(180f))
        }
        val dispatcher = Minecraft.getInstance().entityRenderDispatcher
        val renderer = dispatcher.getRenderer(dummy)
        renderer.render(dummy, player.yRot, event.partialTick, pose, event.multiBufferSource, event.packedLight)
        pose.popPose()
        dummy.discard()
    }
}
