package com.reallyusefulribbits.mod.client

import net.minecraft.world.entity.player.Player
import net.neoforged.bus.api.IEventBus
import net.neoforged.fml.ModContainer
import net.neoforged.neoforge.client.event.RenderLivingEvent
import net.neoforged.neoforge.client.gui.ConfigurationScreen
import net.neoforged.neoforge.client.gui.IConfigScreenFactory
import net.neoforged.neoforge.common.NeoForge
import net.neoforged.bus.api.SubscribeEvent
import com.mojang.math.Axis

object ClientModEvents {
    fun init(modBus: IEventBus, modContainer: ModContainer) {
        NeoForge.EVENT_BUS.register(this)
        modContainer.registerExtensionPoint(
            IConfigScreenFactory::class.java,
            IConfigScreenFactory { container, currentScreen -> ConfigurationScreen(container, currentScreen) },
        )
    }

    @SubscribeEvent
    fun onRenderPre(event: RenderLivingEvent.Pre<*, *>) {
        val entity = event.entity
        if (entity is Player && ClientVisuals.isUpsideDown(entity.uuid)) {
            event.poseStack.pushPose()
            event.poseStack.translate(0.0, entity.bbHeight.toDouble(), 0.0)
            event.poseStack.mulPose(Axis.ZP.rotationDegrees(180f))
        }
    }

    @SubscribeEvent
    fun onRenderPost(event: RenderLivingEvent.Post<*, *>) {
        val entity = event.entity
        if (entity is Player && ClientVisuals.isUpsideDown(entity.uuid)) {
            event.poseStack.popPose()
        }
    }
}
