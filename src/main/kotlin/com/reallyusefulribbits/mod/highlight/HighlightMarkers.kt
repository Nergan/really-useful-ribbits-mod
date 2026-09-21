package com.reallyusefulribbits.mod.highlight

import com.reallyusefulribbits.mod.ReallyUsefulRibbitsMod
import com.reallyusefulribbits.mod.config.ModConfig
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.Display
import net.minecraft.world.entity.EntityType
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.neoforge.event.tick.ServerTickEvent
import java.util.UUID

object HighlightMarkers {
    private const val TAG = ReallyUsefulRibbitsMod.MOD_ID + "_highlight"
    private val pending = ArrayList<Pair<UUID, Int>>()

    fun glowBlocks(level: ServerLevel, positions: Collection<BlockPos>, duration: Int = ModConfig.HIGHLIGHT_TICKS) {
        val expire = level.server.tickCount + duration
        for (pos in positions.take(ModConfig.MAX_HIGHLIGHT_BLOCKS)) {
            val display = EntityType.BLOCK_DISPLAY.create(level) as? Display.BlockDisplay ?: continue
            val tag = CompoundTag()
            display.saveWithoutId(tag)
            val state = CompoundTag()
            state.putString("Name", "minecraft:barrier")
            tag.put("block_state", state)
            display.load(tag)
            display.moveTo(pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble(), 0f, 0f)
            display.setGlowingTag(true)
            display.isSilent = true
            display.isInvulnerable = true
            display.setNoGravity(true)
            display.addTag(TAG)
            level.addFreshEntity(display)
            pending += display.uuid to expire
        }
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
