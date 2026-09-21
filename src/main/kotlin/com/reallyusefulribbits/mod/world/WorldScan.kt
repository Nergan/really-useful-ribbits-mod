package com.reallyusefulribbits.mod.world

import com.reallyusefulribbits.mod.logic.FarmFloodFill
import com.reallyusefulribbits.mod.logic.GridPos
import net.minecraft.core.BlockPos
import net.minecraft.tags.FluidTags
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.FarmBlock
import net.minecraft.world.level.block.SugarCaneBlock
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

    fun allWorkBlocks(level: Level, origin: BlockPos, radius: Int): List<BlockPos> {
        val found = LinkedHashSet<BlockPos>()
        found.addAll(farmBlocks(level, origin, radius))
        val seeds = listOfNotNull(
            findClosest(level, origin, radius) { CropSupport.isFarmBlock(level, it) && level.getBlockState(it).block is FarmBlock },
            findClosest(level, origin, radius) { level.getBlockState(it).`is`(Blocks.SOUL_SAND) },
            findClosest(level, origin, radius) {
                val state = level.getBlockState(it)
                state.block is SugarCaneBlock && level.getBlockState(it.below()).block !is SugarCaneBlock
            },
            findClosest(level, origin, radius) { CropSupport.isCaveVine(level.getBlockState(it)) },
        )
        for (seed in seeds) {
            found.addAll(farmBlocks(level, seed, radius))
        }
        return found.toList()
    }

    private fun findClosest(level: Level, origin: BlockPos, radius: Int, test: (BlockPos) -> Boolean): BlockPos? {
        return BlockPos.findClosestMatch(origin, radius, 8, test).getOrNull()
    }

    fun hasItemHandler(level: Level, pos: BlockPos): Boolean = ContainerSupport.handler(level, pos) != null
}
