package com.reallyusefulribbits.mod.item

import com.reallyusefulribbits.mod.ReallyUsefulRibbitsMod
import com.reallyusefulribbits.mod.event.RibbitGuideHandler
import net.minecraft.core.registries.Registries
import net.minecraft.network.chat.Component
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.registries.DeferredRegister
import java.util.function.Supplier

object ModItems {

    val ITEMS: DeferredRegister<Item> =
        DeferredRegister.create(Registries.ITEM, ReallyUsefulRibbitsMod.MOD_ID)

    val CREATIVE_TABS: DeferredRegister<CreativeModeTab> =
        DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ReallyUsefulRibbitsMod.MOD_ID)

    /** Иконка вкладки (та же текстура, что у гайд-книги). В список предметов не кладём. */
    val TAB_ICON = ITEMS.register(
        "tab_icon",
        Supplier<Item> { Item(Item.Properties()) },
    )

    val CREATIVE_TAB = CREATIVE_TABS.register(
        "reallyusefulribbits",
        Supplier<CreativeModeTab> {
            CreativeModeTab.builder()
                .title(Component.translatable("itemGroup.reallyusefulribbits"))
                .icon {
                    RibbitGuideHandler.createBookStack() ?: ItemStack(TAB_ICON.get())
                }
                .displayItems { _, output ->
                    RibbitGuideHandler.createBookStack()?.let { output.accept(it) }
                }
                .build()
        },
    )

    fun register(modBus: IEventBus) {
        ITEMS.register(modBus)
        CREATIVE_TABS.register(modBus)
    }
}
