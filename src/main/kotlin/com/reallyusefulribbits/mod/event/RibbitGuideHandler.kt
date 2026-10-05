package com.reallyusefulribbits.mod.event

import com.reallyusefulribbits.mod.ReallyUsefulRibbitsMod
import com.reallyusefulribbits.mod.loader.RuntimeHooks
import net.minecraft.core.component.DataComponentType
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items

/**
 * Книжка Patchouli выдаётся только если мод установлен.
 * Без Patchouli этот обработчик ничего не делает.
 */
object RibbitGuideHandler {

    const val BOOK_ID = "reallyusefulribbits:ribbit_guide"
    private const val PATCHOULI_ID = "patchouli"
    private const val TAG_RECEIVED = "reallyusefulribbits.received_guide"

    fun isPatchouliLoaded(): Boolean = RuntimeHooks.patchouliLoaded()

    fun acceptsCreativeTab(namespace: String): Boolean = namespace == "ribbits"

    fun creativeBook(): ItemStack? = createBookStack()

    fun onLogin(player: Player) {
        if (player.level().isClientSide) return
        if (!isPatchouliLoaded()) return
        if (RuntimeHooks.guideReceived(player)) return

        val book = createBookStack() ?: return
        if (!player.addItem(book)) {
            player.drop(book, false)
        }
        RuntimeHooks.markGuideReceived(player)
    }

    fun createBookStack(): ItemStack? {
        if (!isPatchouliLoaded()) return null
        val item = BuiltInRegistries.ITEM.get(ResourceLocation.parse("patchouli:guide_book"))
        if (item === Items.AIR) return null
        val stack = ItemStack(item)
        val componentType = BuiltInRegistries.DATA_COMPONENT_TYPE.get(ResourceLocation.parse("patchouli:book"))
            ?: return null
        val bookId = ResourceLocation.parse(BOOK_ID)
        return try {
            @Suppress("UNCHECKED_CAST")
            stack.set(componentType as DataComponentType<Any>, bookId)
            stack
        } catch (_: Throwable) {
            try {
                @Suppress("UNCHECKED_CAST")
                stack.set(componentType as DataComponentType<Any>, BOOK_ID)
                stack
            } catch (error: Throwable) {
                ReallyUsefulRibbitsMod.LOGGER.warn("Не удалось создать книгу Patchouli.", error)
                null
            }
        }
    }
}
