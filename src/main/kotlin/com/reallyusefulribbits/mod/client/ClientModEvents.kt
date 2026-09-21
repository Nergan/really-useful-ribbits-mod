package com.reallyusefulribbits.mod.client

import com.mojang.math.Axis
import com.reallyusefulribbits.mod.event.RibbitGuideHandler
import com.reallyusefulribbits.mod.item.ModItems
import com.reallyusefulribbits.mod.morph.PlayerMorph
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.neoforged.bus.api.IEventBus
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.ModContainer
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent
import net.neoforged.neoforge.client.event.RenderHandEvent
import net.neoforged.neoforge.client.event.RenderLivingEvent
import net.neoforged.neoforge.client.event.RenderPlayerEvent
import net.neoforged.neoforge.client.gui.ConfigurationScreen
import net.neoforged.neoforge.client.gui.IConfigScreenFactory
import net.neoforged.neoforge.common.NeoForge

object ClientModEvents {
    private val BOOK_TINT: Int = 0xFF6BB04A.toInt()

    fun init(modBus: IEventBus, modContainer: ModContainer) {
        NeoForge.EVENT_BUS.register(this)
        modBus.addListener(::onItemColors)
        modContainer.registerExtensionPoint(
            IConfigScreenFactory::class.java,
            IConfigScreenFactory { container, currentScreen -> ConfigurationScreen(container, currentScreen) },
        )
    }

    private fun onItemColors(event: RegisterColorHandlersEvent.Item) {
        event.register(
            { stack, tintIndex -> if (tintIndex == 0) BOOK_TINT else -1 },
            ModItems.TAB_ICON.get(),
        )
        val patchouliBook = BuiltInRegistries.ITEM.get(ResourceLocation.parse("patchouli:guide_book"))
        if (patchouliBook !== net.minecraft.world.item.Items.AIR) {
            event.register(
                { stack, tintIndex ->
                    if (tintIndex == 0 && isOurGuide(stack)) BOOK_TINT else -1
                },
                patchouliBook,
            )
        }
    }

    private fun isOurGuide(stack: ItemStack): Boolean {
        if (!RibbitGuideHandler.isPatchouliLoaded()) return false
        val type = BuiltInRegistries.DATA_COMPONENT_TYPE.get(ResourceLocation.parse("patchouli:book")) ?: return false
        val value = stack.get(type) ?: return false
        return value.toString().contains("ribbit_guide")
    }

    @SubscribeEvent
    fun onRenderPlayer(event: RenderPlayerEvent.Pre) {
        val player = event.entity
        if (ClientVisuals.morphType(player.uuid) == null) return
        event.isCanceled = true
        MorphRenderer.render(event, player)
    }

    @SubscribeEvent
    fun onRenderHand(event: RenderHandEvent) {
        val player = net.minecraft.client.Minecraft.getInstance().player ?: return
        val type = ClientVisuals.morphType(player.uuid) ?: return
        if (!PlayerMorph.isHumanoidType(type)) {
            event.isCanceled = true
        }
    }

    @SubscribeEvent
    fun onRenderPre(event: RenderLivingEvent.Pre<*, *>) {
        val entity = event.entity
        if (entity is Player && ClientVisuals.morphType(entity.uuid) == null && ClientVisuals.isUpsideDown(entity.uuid)) {
            event.poseStack.pushPose()
            event.poseStack.translate(0.0, entity.bbHeight.toDouble(), 0.0)
            event.poseStack.mulPose(Axis.ZP.rotationDegrees(180f))
        }
    }

    @SubscribeEvent
    fun onRenderPost(event: RenderLivingEvent.Post<*, *>) {
        val entity = event.entity
        if (entity is Player && ClientVisuals.morphType(entity.uuid) == null && ClientVisuals.isUpsideDown(entity.uuid)) {
            event.poseStack.popPose()
        }
    }
}
