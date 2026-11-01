package com.reallyusefulribbits.mod.spell

import com.reallyusefulribbits.mod.event.ModAdvancements
import com.reallyusefulribbits.mod.loader.RuntimeHooks
import com.reallyusefulribbits.mod.util.DelayedTasks
import com.yungnickyoung.minecraft.ribbits.entity.RibbitEntity
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.entity.projectile.LargeFireball
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.Vec3
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** Огонь с потолка мира: дождь огненных шаров и озеро лавы на высоте постройки. */
object SkyFire {
    private const val RAIN_WAVES = 8
    private const val RAIN_PER_WAVE = 6
    private const val RAIN_POWER = 4
    private const val LAKE_HALF = 64
    private const val LAKE_BATCH = 1024

    /** «Апокалипсис»: шары летят вниз с высоты постройки и взрываются как динамит. */
    fun fireballRain(level: ServerLevel, ribbit: RibbitEntity, player: ServerPlayer): Boolean {
        val radius = RuntimeHooks.scanRadius().coerceIn(16, 48).toDouble()
        val top = level.maxBuildHeight - 4.0
        val centerX = ribbit.x
        val centerZ = ribbit.z
        repeat(RAIN_WAVES) { wave ->
            DelayedTasks.later(level.server, 1 + wave * 15) {
                if (!ribbit.isAlive) return@later
                repeat(RAIN_PER_WAVE) {
                    val angle = level.random.nextDouble() * Math.PI * 2.0
                    val distance = sqrt(level.random.nextDouble()) * radius
                    val x = centerX + cos(angle) * distance
                    val z = centerZ + sin(angle) * distance
                    if (!level.isLoaded(BlockPos.containing(x, top, z))) return@repeat
                    val fireball = LargeFireball(level, ribbit, Vec3(0.0, -1.0, 0.0), RAIN_POWER)
                    fireball.setPos(x, top, z)
                    fireball.accelerationPower = 0.25
                    level.addFreshEntity(fireball)
                }
            }
        }
        for (other in level.players()) {
            other.playNotifySound(SoundEvents.GHAST_WARN, SoundSource.HOSTILE, 1.2f, 0.6f)
        }
        ModAdvancements.grantApocalypse(player)
        return true
    }

    /** «И так было сказано…»: 128×128 блоков лавы над чародеем на высоте постройки. */
    fun lavaLake(level: ServerLevel, ribbit: RibbitEntity, player: ServerPlayer): Boolean {
        val y = level.maxBuildHeight - 1
        val cx = ribbit.blockX
        val cz = ribbit.blockZ
        val columns = ArrayList<BlockPos>((LAKE_HALF * 2) * (LAKE_HALF * 2))
        for (x in cx - LAKE_HALF until cx + LAKE_HALF) {
            for (z in cz - LAKE_HALF until cz + LAKE_HALF) columns += BlockPos(x, y, z)
        }
        val lava = Blocks.LAVA.defaultBlockState()
        columns.chunked(LAKE_BATCH).forEachIndexed { index, batch ->
            DelayedTasks.later(level.server, 1 + index) {
                for (pos in batch) {
                    if (!level.isLoaded(pos) || !level.getBlockState(pos).isAir) continue
                    level.setBlock(pos, lava, Block.UPDATE_CLIENTS)
                }
            }
        }
        for (other in level.players()) {
            other.playNotifySound(SoundEvents.LAVA_AMBIENT, SoundSource.BLOCKS, 2f, 0.5f)
        }
        ModAdvancements.grantScripture(player)
        return true
    }
}
