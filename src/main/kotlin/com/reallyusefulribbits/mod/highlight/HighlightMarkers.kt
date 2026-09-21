package com.reallyusefulribbits.mod.highlight

import com.reallyusefulribbits.mod.config.ModConfig
import net.minecraft.core.BlockPos
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.resources.ResourceKey
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.Level
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.neoforge.event.tick.ServerTickEvent

object HighlightMarkers {
    private data class Job(
        val dimension: ResourceKey<Level>,
        val blocks: List<BlockPos>,
        val expire: Int,
    )

    private val jobs = ArrayList<Job>()

    fun glowBlocks(level: ServerLevel, positions: Collection<BlockPos>, duration: Int = ModConfig.HIGHLIGHT_TICKS) {
        val blocks = positions.take(ModConfig.MAX_HIGHLIGHT_BLOCKS).toList()
        if (blocks.isEmpty()) return
        jobs += Job(level.dimension(), blocks, level.server.tickCount + duration)
        spark(level, blocks)
    }

    @SubscribeEvent
    fun onServerTick(event: ServerTickEvent.Post) {
        val now = event.server.tickCount
        if (jobs.isEmpty()) return
        val leftover = ArrayList<Job>()
        for (job in jobs) {
            if (now >= job.expire) continue
            leftover += job
            if (now % 8 != 0) continue
            val world = event.server.getLevel(job.dimension) ?: continue
            spark(world, job.blocks)
        }
        jobs.clear()
        jobs += leftover
    }

    private fun spark(level: ServerLevel, blocks: List<BlockPos>) {
        for (pos in blocks) {
            level.sendParticles(ParticleTypes.GLOW, pos.x + 0.5, pos.y + 0.55, pos.z + 0.5, 6, 0.32, 0.32, 0.32, 0.0)
            level.sendParticles(ParticleTypes.END_ROD, pos.x + 0.5, pos.y + 1.05, pos.z + 0.5, 1, 0.0, 0.0, 0.0, 0.0)
        }
    }
}
