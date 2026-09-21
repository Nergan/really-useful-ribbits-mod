package com.reallyusefulribbits.mod.highlight

import com.reallyusefulribbits.mod.ReallyUsefulRibbitsMod
import com.reallyusefulribbits.mod.config.ModConfig
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.monster.Shulker
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.neoforge.event.tick.ServerTickEvent
import java.util.UUID

object HighlightMarkers {
    private const val TAG = ReallyUsefulRibbitsMod.MOD_ID + "_highlight"
    private val pending = ArrayList<Pair<UUID, Int>>()

    fun glowBlocks(level: ServerLevel, positions: Collection<BlockPos>, duration: Int = ModConfig.HIGHLIGHT_TICKS) {
        val expire = level.server.tickCount + duration
        for (pos in positions.take(ModConfig.MAX_HIGHLIGHT_BLOCKS)) {
            val shulker = EntityType.SHULKER.create(level) ?: continue
            shulker.moveTo(pos.x + 0.5, pos.y.toDouble(), pos.z + 0.5, 0f, 0f)
            shulker.isInvisible = true
            shulker.isSilent = true
            shulker.isInvulnerable = true
            shulker.setNoAi(true)
            shulker.setNoGravity(true)
            shulker.addTag(TAG)
            shulker.addEffect(MobEffectInstance(MobEffects.GLOWING, duration, 0, false, false))
            shulker.setPersistenceRequired()
            level.addFreshEntity(shulker)
            pending += shulker.uuid to expire
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
            for (level in event.server.allLevels) {
                val entity = level.getEntity(id) ?: continue
                if (entity.tags.contains(TAG)) entity.discard()
            }
        }
        pending.clear()
        pending += leftover
    }
}
