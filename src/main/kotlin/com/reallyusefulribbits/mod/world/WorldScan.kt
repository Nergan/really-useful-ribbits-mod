package com.reallyusefulribbits.mod.world

import com.reallyusefulribbits.mod.config.ModConfig
import com.reallyusefulribbits.mod.logic.FarmFloodFill
import com.reallyusefulribbits.mod.logic.GridPos
import com.reallyusefulribbits.mod.logic.ScanRadius
import net.minecraft.core.BlockPos
import net.minecraft.tags.FluidTags
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.FarmBlock
import net.minecraft.world.level.block.SugarCaneBlock
import kotlin.math.abs

object WorldScan {
    private const val VERTICAL = 8

    /** Сколько колонок чанков смотреть за один вызов. Спираль по всему радиусу грузила мир. */
    private const val CHUNK_BUDGET = 4
    private const val KIND_WATER = 0
    private const val KIND_CONTAINER = 1
    private const val KIND_FARM = 2
    private const val KIND_WORK = 3

    private data class ScanCursor(val place: Long, val index: Int)

    private val cursors = HashMap<Long, ScanCursor>()

    fun scanDue(gameTime: Long, last: Long, interval: Int, id: Int): Boolean {
        if (interval <= 0) return true
        if (gameTime - last < interval) return false
        val span = interval.toLong()
        return Math.floorMod(gameTime, span) == Math.floorMod(id.toLong(), span)
    }

    /** Незагруженный чанк не значит, что привязка пропала: проверка блока его сгенерирует. */
    fun bindingOk(level: Level, pos: BlockPos?, valid: (BlockPos) -> Boolean): Boolean {
        if (pos == null) return false
        if (!level.isLoaded(pos)) return true
        return valid(pos)
    }

    fun nearestWater(level: Level, origin: BlockPos, radius: Int, cursorId: Int): BlockPos? =
        findInLoaded(level, origin, radius, cursorId, KIND_WATER, rewind = true) { pos ->
            level.getFluidState(pos).`is`(FluidTags.WATER) &&
                level.getBlockState(pos.above()).`is`(Blocks.AIR)
        }

    fun nearestContainer(level: Level, origin: BlockPos, radius: Int, cursorId: Int): BlockPos? =
        findInLoaded(level, origin, radius, cursorId, KIND_CONTAINER, rewind = true) { pos ->
            ContainerSupport.isStorage(level, pos)
        }

    fun nearestFarmOrigin(level: Level, origin: BlockPos, radius: Int, cursorId: Int): BlockPos? =
        findInLoaded(level, origin, radius, cursorId, KIND_FARM, rewind = true) { pos ->
            CropSupport.isFarmBlock(level, pos)
        }

    fun farmBlocks(level: Level, origin: BlockPos, radius: Int): List<BlockPos> {
        val filled = FarmFloodFill.fill(
            origin = GridPos(origin.x, origin.y, origin.z),
            radius = radius,
            maxBlocks = ModConfig.FARM_MEMORY_CAP,
        ) { pos -> CropSupport.isFarmBlock(level, BlockPos(pos.x, pos.y, pos.z)) }
        return filled.map { BlockPos(it.x, it.y, it.z) }
    }

    fun allWorkBlocks(level: Level, origin: BlockPos, radius: Int, cursorId: Int): List<BlockPos> {
        val found = LinkedHashSet<BlockPos>()
        found.addAll(farmBlocks(level, origin, radius))
        for (seed in discoverSeeds(level, origin, radius, cursorId)) {
            if (seed == origin) continue
            found.addAll(farmBlocks(level, seed, radius))
        }
        return found.toList()
    }

    fun hasItemHandler(level: Level, pos: BlockPos): Boolean {
        if (!level.isLoaded(pos)) return false
        return ContainerSupport.handler(level, pos) != null
    }

    private fun findInLoaded(
        level: Level,
        origin: BlockPos,
        radius: Int,
        cursorId: Int,
        kind: Int,
        rewind: Boolean,
        test: (BlockPos) -> Boolean,
    ): BlockPos? {
        val window = takeWindow(level, origin, radius, cursorId, kind) ?: return null
        var best: BlockPos? = null
        var bestDist = Long.MAX_VALUE
        val cursor = BlockPos.MutableBlockPos()
        for (chunk in window) {
            scanChunk(level, origin, radius, chunk, cursor) { pos ->
                if (!test(pos)) return@scanChunk
                val dist = dist2(origin, pos)
                if (dist < bestDist) {
                    bestDist = dist
                    best = pos.immutable()
                }
            }
        }
        if (best != null && rewind) {
            cursors[cursorKey(cursorId, kind)] = ScanCursor(placeOf(origin), 0)
        }
        return best
    }

    private fun discoverSeeds(level: Level, origin: BlockPos, radius: Int, cursorId: Int): List<BlockPos> {
        val window = takeWindow(level, origin, radius, cursorId, KIND_WORK) ?: return emptyList()
        var farmland: BlockPos? = null
        var soul: BlockPos? = null
        var cane: BlockPos? = null
        var vine: BlockPos? = null
        var farmDist = Long.MAX_VALUE
        var soulDist = Long.MAX_VALUE
        var caneDist = Long.MAX_VALUE
        var vineDist = Long.MAX_VALUE
        val cursor = BlockPos.MutableBlockPos()
        for (chunk in window) {
            scanChunk(level, origin, radius, chunk, cursor) { pos ->
                val state = level.getBlockState(pos)
                val dist = dist2(origin, pos)
                if (dist < farmDist && state.block is FarmBlock) {
                    farmDist = dist
                    farmland = pos.immutable()
                }
                if (dist < soulDist && state.`is`(Blocks.SOUL_SAND)) {
                    soulDist = dist
                    soul = pos.immutable()
                }
                if (dist < caneDist && state.block is SugarCaneBlock && level.getBlockState(pos.below()).block !is SugarCaneBlock) {
                    caneDist = dist
                    cane = pos.immutable()
                }
                if (dist < vineDist && CropSupport.isCaveVine(state)) {
                    vineDist = dist
                    vine = pos.immutable()
                }
            }
        }
        return listOfNotNull(farmland, soul, cane, vine)
    }

    private fun takeWindow(level: Level, origin: BlockPos, radius: Int, cursorId: Int, kind: Int): List<ChunkPos>? {
        val chunks = loadedChunks(level, origin, ScanRadius.clamp(radius))
        if (chunks.isEmpty()) return null
        val place = placeOf(origin)
        val key = cursorKey(cursorId, kind)
        val stored = cursors[key]
        var index = if (stored != null && stored.place == place) stored.index else 0
        if (index >= chunks.size) index = 0
        val end = minOf(index + CHUNK_BUDGET, chunks.size)
        val next = if (end >= chunks.size) 0 else end
        cursors[key] = ScanCursor(place, next)
        return chunks.subList(index, end).toList()
    }

    private fun loadedChunks(level: Level, origin: BlockPos, radius: Int): List<ChunkPos> {
        val center = ChunkPos(origin)
        val chunkRadius = (radius + 15) shr 4
        val found = ArrayList<ChunkPos>()
        for (dx in -chunkRadius..chunkRadius) {
            for (dz in -chunkRadius..chunkRadius) {
                val chunk = ChunkPos(center.x + dx, center.z + dz)
                if (!chunkTouches(chunk, origin, radius)) continue
                val probe = BlockPos(chunk.minBlockX + 8, origin.y, chunk.minBlockZ + 8)
                if (!level.isLoaded(probe)) continue
                found += chunk
            }
        }
        found.sortBy { chunkDist2(it, origin) }
        return found
    }

    private fun chunkTouches(chunk: ChunkPos, origin: BlockPos, radius: Int): Boolean {
        val nearX = origin.x.coerceIn(chunk.minBlockX, chunk.maxBlockX)
        val nearZ = origin.z.coerceIn(chunk.minBlockZ, chunk.maxBlockZ)
        return abs(nearX - origin.x) <= radius && abs(nearZ - origin.z) <= radius
    }

    private fun chunkDist2(chunk: ChunkPos, origin: BlockPos): Long {
        val dx = (chunk.minBlockX + 8 - origin.x).toLong()
        val dz = (chunk.minBlockZ + 8 - origin.z).toLong()
        return dx * dx + dz * dz
    }

    private inline fun scanChunk(
        level: Level,
        origin: BlockPos,
        radius: Int,
        chunk: ChunkPos,
        cursor: BlockPos.MutableBlockPos,
        visit: (BlockPos) -> Unit,
    ) {
        val clamped = ScanRadius.clamp(radius)
        val minY = maxOf(level.minBuildHeight, origin.y - VERTICAL)
        val maxY = minOf(level.maxBuildHeight - 1, origin.y + VERTICAL)
        val minX = maxOf(chunk.minBlockX, origin.x - clamped)
        val maxX = minOf(chunk.maxBlockX, origin.x + clamped)
        val minZ = maxOf(chunk.minBlockZ, origin.z - clamped)
        val maxZ = minOf(chunk.maxBlockZ, origin.z + clamped)
        for (x in minX..maxX) {
            for (z in minZ..maxZ) {
                for (y in minY..maxY) {
                    cursor.set(x, y, z)
                    visit(cursor)
                }
            }
        }
    }

    private fun cursorKey(id: Int, kind: Int): Long = (id.toLong() shl 4) or kind.toLong()

    private fun placeOf(origin: BlockPos): Long = ChunkPos(origin).toLong()

    private fun dist2(origin: BlockPos, pos: BlockPos): Long {
        val dx = (pos.x - origin.x).toLong()
        val dy = (pos.y - origin.y).toLong()
        val dz = (pos.z - origin.z).toLong()
        return dx * dx + dy * dy + dz * dz
    }
}
