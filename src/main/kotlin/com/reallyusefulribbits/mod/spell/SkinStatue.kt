package com.reallyusefulribbits.mod.spell

import com.reallyusefulribbits.mod.loader.RuntimeHooks
import com.reallyusefulribbits.mod.util.DelayedTasks
import com.yungnickyoung.minecraft.ribbits.entity.RibbitEntity
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.levelgen.Heightmap
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.Base64
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.imageio.ImageIO
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/** Статуя игрока из бетона по пикселям скина. Если скина ещё нет, стоит Стив. */
object SkinStatue {
    private val http: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(3))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build()
    private val skins = ConcurrentHashMap<UUID, IntArray>()
    private val pending = ConcurrentHashMap.newKeySet<UUID>()

    fun request(player: ServerPlayer) {
        if (skins.containsKey(player.uuid) || !pending.add(player.uuid)) return
        val url = skinUrl(player)
        if (url == null) {
            pending.remove(player.uuid)
            return
        }
        val request = HttpRequest.newBuilder(URI.create(url))
            .timeout(Duration.ofSeconds(4))
            .GET()
            .build()
        http.sendAsync(request, HttpResponse.BodyHandlers.ofByteArray()).whenComplete { response, error ->
            pending.remove(player.uuid)
            if (error != null || response == null || response.statusCode() != 200) return@whenComplete
            val bytes = response.body()
            if (bytes == null || bytes.size > 1_000_000) return@whenComplete
            val image = runCatching { ImageIO.read(ByteArrayInputStream(bytes)) }.getOrNull() ?: return@whenComplete
            if (image.width < 64 || image.height < 32) return@whenComplete
            skins[player.uuid] = pixels(image)
        }
    }

    fun place(level: ServerLevel, ribbit: RibbitEntity, player: ServerPlayer): Boolean {
        request(player)
        val ready = skins[player.uuid]
        if (ready != null) return build(level, ribbit, ready)
        DelayedTasks.later(level.server, 30) {
            if (!level.isLoaded(ribbit.blockPosition())) return@later
            build(level, ribbit, skins[player.uuid] ?: STEVE)
        }
        return true
    }

    private fun build(level: ServerLevel, ribbit: RibbitEntity, skin: IntArray): Boolean {
        val radius = RuntimeHooks.scanRadius()
        val origin = ribbit.blockPosition()
        val yaw = Math.toRadians(ribbit.yRot.toDouble())
        val forwardX = -sin(yaw)
        val forwardZ = cos(yaw)
        val rightX = -cos(yaw)
        val rightZ = -sin(yaw)
        for (distance in intArrayOf(4, 6, 8, 3)) {
            val x = origin.x + (forwardX * distance).roundToInt()
            val z = origin.z + (forwardZ * distance).roundToInt()
            if (origin.distSqr(BlockPos(x, origin.y, z)) > radius * radius) continue
            if (!level.hasChunk(x shr 4, z shr 4)) continue
            val ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z)
            if (ground <= level.minBuildHeight) continue
            val placed = placeAt(level, origin, radius, x, ground, z, rightX, rightZ, forwardX, forwardZ, skin)
            if (placed >= 40) return true
        }
        return false
    }

    private fun placeAt(
        level: ServerLevel,
        origin: BlockPos,
        radius: Int,
        baseX: Int,
        baseY: Int,
        baseZ: Int,
        rightX: Double,
        rightZ: Double,
        forwardX: Double,
        forwardZ: Double,
        skin: IntArray,
    ): Int {
        var placed = 0
        for (box in BOXES) {
            for (lx in 0 until box.sx) {
                for (ly in 0 until box.sy) {
                    for (lz in 0 until box.sz) {
                        val localX = box.x + lx - 3.5
                        val localZ = box.z + lz - 1.0
                        val x = baseX + (rightX * localX + forwardX * localZ).roundToInt()
                        val y = baseY + box.y + ly
                        val z = baseZ + (rightZ * localX + forwardZ * localZ).roundToInt()
                        val pos = BlockPos(x, y, z)
                        if (y >= level.maxBuildHeight || y < level.minBuildHeight) continue
                        if (origin.distSqr(pos) > radius * radius) continue
                        val state = level.getBlockState(pos)
                        if (!state.isAir && !(state.canBeReplaced() && state.fluidState.isEmpty)) continue
                        val u = box.u + lx * box.uw / box.sx
                        val v = box.v + (box.sy - 1 - ly) * box.uh / box.sy
                        val color = sample(skin, u, v)
                        level.setBlock(pos, concrete(color).defaultBlockState(), Block.UPDATE_CLIENTS)
                        placed++
                    }
                }
            }
        }
        return placed
    }

    private fun sample(skin: IntArray, u: Int, v: Int): Int {
        val x = u.coerceIn(0, 63)
        val y = v.coerceIn(0, 63)
        val pixel = skin[y * 64 + x]
        val alpha = pixel ushr 24
        return if (alpha < 16) 0xFFC6A07A.toInt() else pixel
    }

    private fun concrete(argb: Int): Block {
        val r = (argb shr 16) and 255
        val g = (argb shr 8) and 255
        val b = argb and 255
        var best = Blocks.WHITE_CONCRETE
        var bestDist = Int.MAX_VALUE
        for ((block, color) in CONCRETE) {
            val dr = r - ((color shr 16) and 255)
            val dg = g - ((color shr 8) and 255)
            val db = b - (color and 255)
            val dist = dr * dr + dg * dg + db * db
            if (dist < bestDist) {
                bestDist = dist
                best = block
            }
        }
        return best
    }

    private fun skinUrl(player: ServerPlayer): String? {
        val property = player.gameProfile.properties.get("textures").firstOrNull() ?: return null
        val json = String(Base64.getDecoder().decode(property.value), Charsets.UTF_8)
        return SKIN_URL.find(json)?.groupValues?.get(1)
    }

    private fun pixels(image: BufferedImage): IntArray {
        val out = IntArray(64 * 64) { 0xFFC6A07A.toInt() }
        val width = image.width.coerceAtMost(64)
        val height = image.height.coerceAtMost(64)
        for (y in 0 until height) {
            for (x in 0 until width) out[y * 64 + x] = image.getRGB(x, y)
        }
        return out
    }

    private data class Box(
        val x: Int,
        val y: Int,
        val z: Int,
        val sx: Int,
        val sy: Int,
        val sz: Int,
        val u: Int,
        val v: Int,
        val uw: Int,
        val uh: Int,
    )

    private val BOXES = listOf(
        Box(2, 0, 1, 2, 6, 2, 4, 20, 4, 12),
        Box(4, 0, 1, 2, 6, 2, 20, 52, 4, 12),
        Box(2, 6, 1, 4, 6, 2, 20, 20, 8, 12),
        Box(0, 6, 1, 2, 6, 2, 44, 20, 4, 12),
        Box(6, 6, 1, 2, 6, 2, 36, 52, 4, 12),
        Box(2, 12, 0, 4, 4, 4, 8, 8, 8, 8),
    )

    private val CONCRETE = listOf(
        Blocks.WHITE_CONCRETE to 0xD1D1D1,
        Blocks.ORANGE_CONCRETE to 0xE06101,
        Blocks.MAGENTA_CONCRETE to 0xA9309F,
        Blocks.LIGHT_BLUE_CONCRETE to 0x2389C6,
        Blocks.YELLOW_CONCRETE to 0xF0AF15,
        Blocks.LIME_CONCRETE to 0x5EA918,
        Blocks.PINK_CONCRETE to 0xD5658F,
        Blocks.GRAY_CONCRETE to 0x36393D,
        Blocks.LIGHT_GRAY_CONCRETE to 0x7D7D73,
        Blocks.CYAN_CONCRETE to 0x157788,
        Blocks.PURPLE_CONCRETE to 0x64209C,
        Blocks.BLUE_CONCRETE to 0x2C2E8F,
        Blocks.BROWN_CONCRETE to 0x603C20,
        Blocks.GREEN_CONCRETE to 0x495B24,
        Blocks.RED_CONCRETE to 0x8E2121,
        Blocks.BLACK_CONCRETE to 0x080A0F,
    )

    private val SKIN_URL = Regex(""""url"\s*:\s*"(https://textures\.minecraft\.net/texture/[^"]+)"""")

    private val STEVE: IntArray = IntArray(64 * 64) { index ->
        val x = index % 64
        val y = index / 64
        when {
            x in 8..15 && y in 8..15 -> 0xFFC6A07A.toInt()
            x in 20..27 && y in 20..31 -> 0xFF2389C6.toInt()
            x in 44..47 && y in 20..31 -> 0xFFC6A07A.toInt()
            x in 4..7 && y in 20..31 -> 0xFF2C2E8F.toInt()
            x in 20..23 && y in 52..63 -> 0xFF2C2E8F.toInt()
            x in 36..39 && y in 52..63 -> 0xFFC6A07A.toInt()
            else -> 0xFFC6A07A.toInt()
        }
    }

}
