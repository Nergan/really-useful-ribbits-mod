package com.reallyusefulribbits.mod.spell

import com.reallyusefulribbits.mod.ReallyUsefulRibbitsMod
import com.reallyusefulribbits.mod.loader.RuntimeHooks
import com.yungnickyoung.minecraft.ribbits.entity.RibbitEntity
import net.minecraft.core.BlockPos
import net.minecraft.core.SectionPos
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.core.registries.Registries
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.levelgen.structure.BoundingBox
import kotlin.math.cos
import kotlin.math.sin

/** Случайная структура из реестра сервера, модовые тоже, в радиусе чародея. */
object RandomStructure {
    private const val ATTEMPTS = 10

    fun place(level: ServerLevel, ribbit: RibbitEntity): Boolean {
        val registry = level.registryAccess().registryOrThrow(Registries.STRUCTURE)
        val structures = registry.holders().toList()
        if (structures.isEmpty()) return false
        val generator = level.chunkSource.generator
        val radius = RuntimeHooks.scanRadius().coerceIn(24, 96).toDouble()
        repeat(ATTEMPTS) {
            val holder = structures[ribbit.random.nextInt(structures.size)]
            val angle = ribbit.random.nextDouble() * Math.PI * 2.0
            val distance = 16.0 + ribbit.random.nextDouble() * (radius - 16.0)
            val at = BlockPos.containing(ribbit.x + cos(angle) * distance, ribbit.y, ribbit.z + sin(angle) * distance)
            if (!level.isLoaded(at)) return@repeat
            val start = try {
                holder.value().generate(
                    level.registryAccess(),
                    generator,
                    generator.biomeSource,
                    level.chunkSource.randomState(),
                    level.server.structureManager,
                    level.seed,
                    ChunkPos(at),
                    0,
                    level,
                ) { true }
            } catch (error: Exception) {
                ReallyUsefulRibbitsMod.LOGGER.debug("Structure {} did not generate", holder.key().location(), error)
                null
            } ?: return@repeat
            if (!start.isValid) return@repeat
            val box = start.boundingBox
            val from = ChunkPos(SectionPos.blockToSectionCoord(box.minX()), SectionPos.blockToSectionCoord(box.minZ()))
            val to = ChunkPos(SectionPos.blockToSectionCoord(box.maxX()), SectionPos.blockToSectionCoord(box.maxZ()))
            val chunks = ChunkPos.rangeClosed(from, to).toList()
            if (chunks.any { !level.hasChunk(it.x, it.z) }) return@repeat
            try {
                for (chunk in chunks) {
                    start.placeInChunk(
                        level,
                        level.structureManager(),
                        generator,
                        level.random,
                        BoundingBox(chunk.minBlockX, level.minBuildHeight, chunk.minBlockZ, chunk.maxBlockX, level.maxBuildHeight, chunk.maxBlockZ),
                        chunk,
                    )
                }
            } catch (error: Exception) {
                ReallyUsefulRibbitsMod.LOGGER.warn("Structure {} failed while placing", holder.key().location(), error)
                return@repeat
            }
            val center = box.center
            level.sendParticles(ParticleTypes.PORTAL, center.x + 0.5, center.y + 0.5, center.z + 0.5, 80, 3.0, 3.0, 3.0, 0.2)
            return true
        }
        return false
    }
}
