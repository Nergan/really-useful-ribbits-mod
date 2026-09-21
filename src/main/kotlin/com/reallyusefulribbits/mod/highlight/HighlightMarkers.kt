package com.reallyusefulribbits.mod.highlight

import com.reallyusefulribbits.mod.ReallyUsefulRibbitsMod
import com.reallyusefulribbits.mod.config.ModConfig
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.Display
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.monster.Shulker
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.neoforge.event.tick.ServerTickEvent
import java.util.UUID

object HighlightMarkers {
    private const val TAG = ReallyUsefulRibbitsMod.MOD_ID + "_highlight"
    private val pending = ArrayList<Pair<UUID, Int>>()

    fun glowBlocks(level: ServerLevel, positions: Collection<BlockPos>, duration: Int = ModConfig.HIGHLIGHT_TICKS) {
        val expire = level.server.tickCount + duration
        for (pos in positions.take(ModConfig.MAX_HIGHLIGHT_BLOCKS)) {
            val marker = spawnOutline(level, pos) ?: spawnItemMarker(level, pos) ?: continue
            marker.isSilent = true
            marker.isInvulnerable = true
            marker.setNoGravity(true)
            marker.setGlowingTag(true)
            marker.addTag(TAG)
            pending += marker.uuid to expire
        }
    }

    private fun spawnOutline(level: ServerLevel, pos: BlockPos): Shulker? {
        val shulker = EntityType.SHULKER.create(level) ?: return null
        shulker.moveTo(pos.x + 0.5, pos.y.toDouble(), pos.z + 0.5, 0f, 0f)
        shulker.isInvisible = true
        shulker.setNoAi(true)
        shulker.noPhysics = true
        shulker.setPersistenceRequired()
        level.addFreshEntity(shulker)
        return shulker
    }

    private fun spawnItemMarker(level: ServerLevel, pos: BlockPos): Display.ItemDisplay? {
        val display = EntityType.ITEM_DISPLAY.create(level) as? Display.ItemDisplay ?: return null
        val tag = CompoundTag()
        display.saveWithoutId(tag)
        tag.put("item", ItemStack(Items.BARRIER).save(level.registryAccess()))
        tag.putString("billboard", "center")
        tag.putString("item_display", "gui")
        display.load(tag)
        display.moveTo(pos.x + 0.5, pos.y + 0.45, pos.z + 0.5, 0f, 0f)
        level.addFreshEntity(display)
        return display
    }

    @SubscribeEvent
    fun onServerTick(event: ServerTickEvent.Post) {
        val now = event.server.tickCount
        if (pending.isEmpty()) return
        val leftover = ArrayList<Pair<UUID, Int>>()
        for ((id, expire) in pending) {
            if (now < expire) {
                leftover += id to expire
                continue
            }
            for (world in event.server.allLevels) {
                val entity = world.getEntity(id) ?: continue
                if (entity.tags.contains(TAG)) entity.discard()
            }
        }
        pending.clear()
        pending += leftover
    }
}
