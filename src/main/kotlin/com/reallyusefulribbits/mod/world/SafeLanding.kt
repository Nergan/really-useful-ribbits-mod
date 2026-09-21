package com.reallyusefulribbits.mod.world

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.server.level.ServerLevel
import net.minecraft.tags.FluidTags
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.levelgen.Heightmap
import net.minecraft.world.phys.Vec3

object SafeLanding {
    fun find(dest: ServerLevel, hint: BlockPos): Vec3 {
        val start = when (dest.dimension()) {
            Level.NETHER -> BlockPos(hint.x, 64, hint.z)
            Level.END -> BlockPos(100, 50, 0)
            else -> dest.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, BlockPos(hint.x, 0, hint.z))
        }
        findNear(dest, start)?.let { return center(it) }
        val fallback = dest.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, start)
        findNear(dest, fallback)?.let { return center(it) }
        return Vec3(start.x + 0.5, (dest.minBuildHeight + 8).toDouble(), start.z + 0.5)
    }

    private fun findNear(level: ServerLevel, origin: BlockPos): BlockPos? {
        for (radius in 0..16) {
            for (dx in -radius..radius) {
                for (dz in -radius..radius) {
                    if (kotlin.math.abs(dx) != radius && kotlin.math.abs(dz) != radius && radius > 0) continue
                    column(level, origin.x + dx, origin.z + dz)?.let { return it }
                }
            }
        }
        return null
    }

    private fun column(level: ServerLevel, x: Int, z: Int): BlockPos? {
        val yRange = when (level.dimension()) {
            Level.NETHER -> 120 downTo 8
            Level.END -> 80 downTo 40
            else -> {
                val top = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z)
                (top + 2) downTo (top - 8).coerceAtLeast(level.minBuildHeight + 2)
            }
        }
        for (y in yRange) {
            val feet = BlockPos(x, y, z)
            if (isSafe(level, feet)) return feet
        }
        return null
    }

    fun isSafe(level: ServerLevel, feet: BlockPos): Boolean {
        if (feet.y <= level.minBuildHeight + 1 || feet.y >= level.maxBuildHeight - 2) return false
        if (level.dimension() == Level.NETHER && (feet.y < 5 || feet.y > 120)) return false
        val below = level.getBlockState(feet.below())
        val at = level.getBlockState(feet)
        val above = level.getBlockState(feet.above())
        if (below.`is`(Blocks.BEDROCK) || below.`is`(Blocks.LAVA) || below.`is`(Blocks.MAGMA_BLOCK)) return false
        if (below.isAir || !below.isFaceSturdy(level, feet.below(), Direction.UP)) return false
        if (!at.isAir || !above.isAir) return false
        if (!level.getFluidState(feet).isEmpty || !level.getFluidState(feet.above()).isEmpty) return false
        if (level.getFluidState(feet.below()).`is`(FluidTags.LAVA)) return false
        return true
    }

    private fun center(pos: BlockPos): Vec3 = Vec3(pos.x + 0.5, pos.y.toDouble(), pos.z + 0.5)
}
