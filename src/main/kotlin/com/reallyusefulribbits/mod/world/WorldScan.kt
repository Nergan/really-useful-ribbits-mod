package com.reallyusefulribbits.mod.world

import com.reallyusefulribbits.mod.logic.FarmFloodFill
import com.reallyusefulribbits.mod.logic.GridPos
import net.minecraft.core.BlockPos
import net.minecraft.tags.FluidTags
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Blocks
import kotlin.jvm.optionals.getOrNull

object WorldScan {
    fun nearestWater(level: Level, origin: BlockPos, radius: Int): BlockPos? {
        return BlockPos.findClosestMatch(origin, radius, 8) { pos ->
            level.getFluidState(pos).`is`(FluidTags.WATER) &&
                level.getBlockState(pos.above()).`is`(Blocks.AIR)
        }.getOrNull()
    }

    fun nearestContainer(level: Level, origin: BlockPos, radius: Int): BlockPos? {
        return BlockPos.findClosestMatch(origin, radius, 8) { pos ->
            ContainerSupport.isStorage(level, pos)
        }.getOrNull()
    }

    fun nearestFarmOrigin(level: Level, origin: BlockPos, radius: Int): BlockPos? {
        return BlockPos.findClosestMatch(origin, radius, 8) { pos ->
            CropSupport.isFarmBlock(level, pos)
        }.getOrNull()
    }

    fun farmBlocks(level: Level, origin: BlockPos, radius: Int): List<BlockPos> {
        val filled = FarmFloodFill.fill(
            origin = GridPos(origin.x, origin.y, origin.z),
            radius = radius,
        ) { pos -> CropSupport.isFarmBlock(level, BlockPos(pos.x, pos.y, pos.z)) }
        return filled.map { BlockPos(it.x, it.y, it.z) }
    }

    fun hasItemHandler(level: Level, pos: BlockPos): Boolean = ContainerSupport.handler(level, pos) != null
}
