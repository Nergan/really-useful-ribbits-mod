package com.reallyusefulribbits.mod.spell

import net.minecraft.core.BlockPos
import net.minecraft.server.MinecraftServer
import net.minecraft.world.level.WorldGenLevel
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.chunk.ChunkAccess
import net.minecraft.world.level.levelgen.Heightmap
import java.util.Random

/**
 * «Хуже проигрыша»: каждый новый чанк после заклинания выходит испорченным.
 * Уже сгенерированные чанки не трогаются. Зачарованное золотое яблоко выключает порчу.
 */
object WorldCorruption {
    @Volatile
    var active: Boolean = false
        private set

    private val WRONG: List<BlockState> = listOf(
        Blocks.SCULK, Blocks.BARRIER, Blocks.END_STONE, Blocks.NETHERRACK, Blocks.OBSIDIAN,
        Blocks.CRYING_OBSIDIAN, Blocks.PURPUR_BLOCK, Blocks.MAGENTA_GLAZED_TERRACOTTA, Blocks.SPONGE,
        Blocks.MUSHROOM_STEM, Blocks.GLOWSTONE, Blocks.SLIME_BLOCK, Blocks.SOUL_SAND, Blocks.DRIED_KELP_BLOCK,
        Blocks.NOTE_BLOCK, Blocks.BONE_BLOCK, Blocks.SHROOMLIGHT, Blocks.MAGMA_BLOCK,
    ).map { it.defaultBlockState() }

    fun load(server: MinecraftServer) {
        active = SorcererWorldData.get(server).corruptionActive
    }

    fun unload() {
        active = false
    }

    fun set(server: MinecraftServer, on: Boolean): Boolean {
        val data = SorcererWorldData.get(server)
        if (data.corruptionActive == on) return false
        data.corruptionActive = on
        data.changed()
        active = on
        return true
    }

    /** Вызывается после фич в потоке генерации. Пишет только в свой чанк. */
    fun corrupt(level: WorldGenLevel, chunk: ChunkAccess) {
        val origin = chunk.pos
        val random = Random(level.seed xor (origin.toLong() * 31L + 0x5DEECE66DL))
        val minX = origin.minBlockX
        val minZ = origin.minBlockZ
        val bottom = chunk.minBuildHeight
        val top = chunk.maxBuildHeight - 1
        val cursor = BlockPos.MutableBlockPos()
        when (random.nextInt(4)) {
            0 -> carveVoids(chunk, random, minX, minZ, bottom, top, cursor)
            1 -> raisePillars(chunk, random, minX, minZ, top, cursor)
            2 -> slice(chunk, random, minX, minZ, bottom, cursor)
            else -> shear(chunk, random, minX, minZ, bottom, cursor)
        }
        speckle(chunk, random, minX, minZ, bottom, cursor)
    }

    private fun surface(chunk: ChunkAccess, x: Int, z: Int): Int =
        chunk.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x and 15, z and 15)

    private fun carveVoids(chunk: ChunkAccess, random: Random, minX: Int, minZ: Int, bottom: Int, top: Int, cursor: BlockPos.MutableBlockPos) {
        repeat(2 + random.nextInt(3)) {
            val cx = minX + random.nextInt(16)
            val cz = minZ + random.nextInt(16)
            val cy = (surface(chunk, cx, cz) - random.nextInt(12)).coerceIn(bottom + 6, top - 6)
            val radius = 3 + random.nextInt(5)
            for (dx in -radius..radius) for (dy in -radius..radius) for (dz in -radius..radius) {
                if (dx * dx + dy * dy + dz * dz > radius * radius) continue
                val x = cx + dx
                val z = cz + dz
                if (x - minX !in 0..15 || z - minZ !in 0..15) continue
                put(chunk, cursor.set(x, cy + dy, z), Blocks.AIR.defaultBlockState())
            }
        }
    }

    private fun raisePillars(chunk: ChunkAccess, random: Random, minX: Int, minZ: Int, top: Int, cursor: BlockPos.MutableBlockPos) {
        repeat(3 + random.nextInt(6)) {
            val x = minX + random.nextInt(16)
            val z = minZ + random.nextInt(16)
            val base = surface(chunk, x, z)
            val block = WRONG[random.nextInt(WRONG.size)]
            for (y in base until (base + 10 + random.nextInt(30)).coerceAtMost(top)) {
                put(chunk, cursor.set(x, y, z), block)
            }
        }
    }

    /** Под поверхностью вырезан слой: земля висит над пустотой. */
    private fun slice(chunk: ChunkAccess, random: Random, minX: Int, minZ: Int, bottom: Int, cursor: BlockPos.MutableBlockPos) {
        val depth = 3 + random.nextInt(4)
        val gap = 2 + random.nextInt(4)
        for (x in minX until minX + 16) for (z in minZ until minZ + 16) {
            val surfaceY = surface(chunk, x, z)
            for (y in (surfaceY - depth - gap) until (surfaceY - depth)) {
                if (y <= bottom + 1) continue
                put(chunk, cursor.set(x, y, z), Blocks.AIR.defaultBlockState())
            }
        }
    }

    /** Колонны сдвинуты вверх и вниз ступенями, как битая текстура. */
    private fun shear(chunk: ChunkAccess, random: Random, minX: Int, minZ: Int, bottom: Int, cursor: BlockPos.MutableBlockPos) {
        val stripe = 2 + random.nextInt(3)
        for (x in minX until minX + 16) for (z in minZ until minZ + 16) {
            val band = ((x - minX) / stripe + (z - minZ) / stripe) % 3
            if (band == 0) continue
            val lift = if (band == 1) 4 + random.nextInt(3) else -(3 + random.nextInt(3))
            val surfaceY = surface(chunk, x, z)
            if (lift > 0) {
                val filler = chunk.getBlockState(cursor.set(x, surfaceY - 1, z))
                for (y in surfaceY until surfaceY + lift) put(chunk, cursor.set(x, y, z), filler)
            } else {
                for (y in (surfaceY + lift).coerceAtLeast(bottom + 2) until surfaceY) {
                    put(chunk, cursor.set(x, y, z), Blocks.AIR.defaultBlockState())
                }
            }
        }
    }

    private fun speckle(chunk: ChunkAccess, random: Random, minX: Int, minZ: Int, bottom: Int, cursor: BlockPos.MutableBlockPos) {
        for (x in minX until minX + 16) for (z in minZ until minZ + 16) {
            val surfaceY = surface(chunk, x, z)
            for (y in (surfaceY - 24).coerceAtLeast(bottom + 1)..surfaceY) {
                if (random.nextInt(14) != 0) continue
                cursor.set(x, y, z)
                val state = chunk.getBlockState(cursor)
                if (state.isAir || !state.fluidState.isEmpty || state.hasBlockEntity() || state.`is`(Blocks.BEDROCK)) continue
                put(chunk, cursor, WRONG[random.nextInt(WRONG.size)])
            }
        }
    }

    private fun put(chunk: ChunkAccess, pos: BlockPos, state: BlockState) {
        val current = chunk.getBlockState(pos)
        if (current.`is`(Blocks.BEDROCK) || current.hasBlockEntity()) return
        chunk.setBlockState(pos, state, false)
    }
}
