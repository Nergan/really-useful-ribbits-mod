package com.reallyusefulribbits.mod.compat

import com.reallyusefulribbits.mod.inventory.RibbitBags
import com.reallyusefulribbits.mod.logic.InventoryRules
import com.reallyusefulribbits.mod.logic.ProfessionKind
import com.reallyusefulribbits.mod.util.professionKind
import com.reallyusefulribbits.mod.util.work
import com.yungnickyoung.minecraft.ribbits.entity.RibbitEntity
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.Tag
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import snownee.jade.api.EntityAccessor
import snownee.jade.api.IComponentProvider
import snownee.jade.api.IServerDataProvider
import snownee.jade.api.ITooltip
import snownee.jade.api.IWailaClientRegistration
import snownee.jade.api.IWailaCommonRegistration
import snownee.jade.api.IWailaPlugin
import snownee.jade.api.WailaPlugin
import snownee.jade.api.config.IPluginConfig

/** Jade: сумка фермера и рыбака, и сколько рыбы ещё до похода к бочке. */
@WailaPlugin
class RibbitJadePlugin : IWailaPlugin {
    override fun register(registration: IWailaCommonRegistration) {
        registration.registerEntityDataProvider(Server, RibbitEntity::class.java)
    }

    override fun registerClient(registration: IWailaClientRegistration) {
        registration.registerEntityComponent(Client, RibbitEntity::class.java)
    }

    private object Server : IServerDataProvider<EntityAccessor> {
        private val id: ResourceLocation = ResourceLocation.fromNamespaceAndPath("reallyusefulribbits", "bag")

        override fun getUid(): ResourceLocation = id

        override fun appendServerData(data: CompoundTag, accessor: EntityAccessor) {
            val ribbit = accessor.entity as? RibbitEntity ?: return
            val kind = ribbit.professionKind()
            if (kind != ProfessionKind.FARMER && kind != ProfessionKind.FISHERMAN) return
            val work = ribbit.work()
            val used = work.usedSlots(kind)
            val list = ListTag()
            for (slot in 0 until used) {
                val stack = work.items[slot]
                if (stack.isEmpty) continue
                val row = CompoundTag()
                row.putString("Name", stack.hoverName.string)
                row.putInt("Count", stack.count)
                list.add(row)
            }
            data.put("Bag", list)
            if (kind == ProfessionKind.FISHERMAN) {
                val carried = RibbitBags.carriedCount(work, used)
                data.putInt("FishLeft", InventoryRules.fishUntilHaul(carried, work.slotLimit(kind)))
            }
        }
    }

    private object Client : IComponentProvider<EntityAccessor> {
        private val id: ResourceLocation = ResourceLocation.fromNamespaceAndPath("reallyusefulribbits", "bag")

        override fun getUid(): ResourceLocation = id

        override fun appendTooltip(tooltip: ITooltip, accessor: EntityAccessor, config: IPluginConfig) {
            val data = accessor.serverData
            if (data.contains("FishLeft")) {
                tooltip.append(Component.translatable("reallyusefulribbits.jade.fish_left", data.getInt("FishLeft")))
            }
            if (!data.contains("Bag", Tag.TAG_LIST.toInt())) return
            val list = data.getList("Bag", Tag.TAG_COMPOUND.toInt())
            if (list.isEmpty()) {
                tooltip.append(Component.translatable("reallyusefulribbits.jade.bag_empty"))
                return
            }
            for (row in list) {
                val item = row as? CompoundTag ?: continue
                tooltip.append(Component.literal("${item.getString("Name")} ×${item.getInt("Count")}"))
            }
        }
    }
}
