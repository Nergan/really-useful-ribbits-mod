package com.reallyusefulribbits.mod.client

import com.mojang.math.Axis
import com.reallyusefulribbits.mod.loader.RuntimeHooks
import com.reallyusefulribbits.mod.morph.PlayerMorph
import net.minecraft.client.Minecraft
import net.minecraft.world.entity.player.Player
import net.neoforged.bus.api.IEventBus
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.ModContainer
import net.neoforged.neoforge.client.event.ClientTickEvent
import net.neoforged.neoforge.client.event.RenderHandEvent
import net.neoforged.neoforge.client.event.RenderLevelStageEvent
import net.neoforged.neoforge.client.event.RenderLivingEvent
import net.neoforged.neoforge.client.event.RenderPlayerEvent
import net.neoforged.neoforge.client.gui.ConfigurationScreen
import net.neoforged.neoforge.client.gui.IConfigScreenFactory
import net.neoforged.neoforge.common.NeoForge
import net.neoforged.neoforge.network.PacketDistributor

object ClientModEvents {
    fun init(@Suppress("UNUSED_PARAMETER") modBus: IEventBus, modContainer: ModContainer) {
        NeoForge.EVENT_BUS.register(this)
        RuntimeHooks.sendToServer = { payload -> PacketDistributor.sendToServer(payload) }
        modContainer.registerExtensionPoint(
            IConfigScreenFactory::class.java,
            IConfigScreenFactory { container, currentScreen -> ConfigurationScreen(container, currentScreen) },
        )
    }

    @SubscribeEvent
    fun onTickStart(event: ClientTickEvent.Pre) {
        ClientSpells.startTick(Minecraft.getInstance())
    }

    @SubscribeEvent
    fun onTickEnd(event: ClientTickEvent.Post) {
        ClientSpells.endTick(Minecraft.getInstance())
    }

    @SubscribeEvent
    fun onRenderStage(event: RenderLevelStageEvent) {
        if (event.stage != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return
        BeamRenderer.render(event.partialTick.getGameTimeDeltaPartialTick(false))
    }

    @SubscribeEvent
    fun onRenderPlayer(event: RenderPlayerEvent.Pre) {
        val player = event.entity
        if (ClientVisuals.morphType(player.uuid) == null) return
        event.isCanceled = true
        MorphRenderer.render(player, event.poseStack, event.partialTick, event.multiBufferSource, event.packedLight)
    }

    @SubscribeEvent
    fun onRenderHand(event: RenderHandEvent) {
        val player = Minecraft.getInstance().player ?: return
        val type = ClientVisuals.morphType(player.uuid) ?: return
        if (!PlayerMorph.isHumanoidType(type)) {
            event.isCanceled = true
        }
    }

    @SubscribeEvent
    fun onRenderPre(event: RenderLivingEvent.Pre<*, *>) {
        val entity = event.entity
        if (entity is Player && ClientVisuals.rendersUpsideDown(entity)) {
            event.poseStack.pushPose()
            event.poseStack.translate(0.0, entity.bbHeight.toDouble(), 0.0)
            event.poseStack.mulPose(Axis.ZP.rotationDegrees(180f))
        }
    }

    @SubscribeEvent
    fun onRenderPost(event: RenderLivingEvent.Post<*, *>) {
        val entity = event.entity
        if (entity is Player && ClientVisuals.rendersUpsideDown(entity)) {
            event.poseStack.popPose()
        }
    }
}
