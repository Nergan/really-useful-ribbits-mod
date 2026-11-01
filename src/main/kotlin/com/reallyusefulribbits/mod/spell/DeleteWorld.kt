package com.reallyusefulribbits.mod.spell

import com.reallyusefulribbits.mod.event.ModAdvancements
import com.reallyusefulribbits.mod.loader.RuntimeHooks
import com.reallyusefulribbits.mod.network.DoomSkyPayload
import it.unimi.dsi.fastutil.longs.LongOpenHashSet
import net.minecraft.ChatFormatting
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.network.chat.Component
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerBossEvent
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.level.TicketType
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.util.Mth
import net.minecraft.world.BossEvent
import net.minecraft.world.Difficulty
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.Pose
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.monster.warden.Warden
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.GameType
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.SculkShriekerBlock
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.levelgen.Heightmap
import net.minecraft.world.phys.Vec3
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * «Удаляй мир»: варден в сто раз больше с 10 000 здоровья. Он идёт за ближайшим игроком
 * в любом измерении, пока его не убьют. Пока он жив, ночь не кончается, небо красное,
 * а скалк под местом призыва растёт без опыта.
 */
object DeleteWorld {
    const val TAG = "reallyusefulribbits_delete_world"
    private const val SCALE = 100.0
    private const val HEALTH = 10_000.0
    private const val ATTACK = 40.0
    private const val WALK_SPEED = 0.5
    private const val CHASE_LIMIT = 160.0
    private const val ARRIVE_DISTANCE = 64.0
    private const val EMERGE_TICKS = 134
    private const val SONIC_RANGE = 140.0
    private const val BLOOM_RADIUS = 192
    private const val BLOOM_PER_TICK = 24
    private const val BLOOM_VISITS = 600
    private const val BLOOM_MEMORY = 1_500_000

    private val TICKET: TicketType<ChunkPos> = TicketType.create("reallyusefulribbits_delete_world", Comparator.comparingLong(ChunkPos::toLong), 60)

    private val bossBar = ServerBossEvent(
        Component.translatable("entity.reallyusefulribbits.delete_world").withStyle(ChatFormatting.DARK_RED),
        BossEvent.BossBarColor.RED,
        BossEvent.BossBarOverlay.PROGRESS,
    ).also {
        it.setDarkenScreen(true)
        it.setCreateWorldFog(true)
    }

    private var bloom: Bloom? = null
    private var missingTicks = 0
    private var attackCooldown = 0
    private var boomCooldown = 0
    private var emergeUntil = 0
    private var skyShown: Boolean? = null

    fun active(server: MinecraftServer): Boolean = SorcererWorldData.get(server).doomActive

    fun summon(level: ServerLevel, player: ServerPlayer): Boolean {
        val server = level.server
        val data = SorcererWorldData.get(server)
        if (data.doomActive || level.difficulty == Difficulty.PEACEFUL) return false
        val warden = EntityType.WARDEN.create(level) ?: return false
        val spot = arrival(level, player, ARRIVE_DISTANCE) ?: return false
        warden.moveTo(spot.x, spot.y, spot.z, yawTowards(spot, player.position()), 0f)
        warden.addTag(TAG)
        warden.setPersistenceRequired()
        warden.isNoAi = true
        warden.noPhysics = true
        warden.isNoGravity = true
        warden.getAttribute(Attributes.SCALE)?.baseValue = SCALE
        warden.getAttribute(Attributes.MAX_HEALTH)?.baseValue = HEALTH
        warden.getAttribute(Attributes.ATTACK_DAMAGE)?.baseValue = ATTACK
        warden.health = HEALTH.toFloat()
        warden.pose = Pose.EMERGING
        if (!level.addFreshEntity(warden)) return false
        warden.refreshDimensions()
        emergeUntil = server.tickCount + EMERGE_TICKS
        data.doomActive = true
        data.doomWarden = warden.uuid
        data.doomDimension = level.dimension()
        data.doomLastPos = warden.blockPosition()
        val origin = surfaceBelow(level, warden.blockPosition())
        data.doomBloomOrigin = origin
        data.changed()
        level.setBlock(origin, Blocks.SCULK.defaultBlockState(), 3)
        bloom = Bloom(origin)
        for (other in server.playerList.players) {
            other.playNotifySound(SoundEvents.WARDEN_EMERGE, SoundSource.HOSTILE, 1.5f, 0.5f)
        }
        ModAdvancements.grantDeleteWorld(player)
        showSky(server, true)
        return true
    }

    fun tick(server: MinecraftServer) {
        val data = SorcererWorldData.get(server)
        if (!data.doomActive) {
            if (bossBar.players.isNotEmpty()) bossBar.removeAllPlayers()
            bloom = null
            showSky(server, false)
            return
        }
        showSky(server, true)
        val overworld = server.overworld()
        val day = Math.floorDiv(overworld.dayTime, 24_000L)
        overworld.dayTime = day * 24_000L + 18_000L
        val level = server.getLevel(data.doomDimension) ?: server.overworld()
        val warden = data.doomWarden?.let { level.getEntity(it) } as? Warden
        if (warden == null || !warden.isAlive) {
            val chunk = ChunkPos(data.doomLastPos)
            if (server.tickCount % 20 == 0) level.chunkSource.addRegionTicket(TICKET, chunk, 2, chunk)
            missingTicks++
            if (missingTicks > 600 && level.areEntitiesLoaded(chunk.toLong())) stop(server, data)
            return
        }
        missingTicks = 0
        drive(level, warden, data)
        bloomTick(server, data)
    }

    private fun drive(level: ServerLevel, warden: Warden, data: SorcererWorldData) {
        val server = level.server
        warden.noPhysics = true
        warden.isNoGravity = true
        if (!warden.isNoAi) warden.isNoAi = true
        warden.setDeltaMovement(Vec3.ZERO)
        if (server.tickCount % 20 == 0) {
            val chunk = ChunkPos(warden.blockPosition())
            level.chunkSource.addRegionTicket(TICKET, chunk, 2, chunk)
            data.doomLastPos = warden.blockPosition()
            data.changed()
            syncBossBar(server)
        }
        bossBar.progress = (warden.health / warden.maxHealth).coerceIn(0f, 1f)
        if (emergeUntil > server.tickCount) return
        if (warden.pose == Pose.EMERGING) warden.pose = Pose.STANDING
        val target = target(server, level) ?: return
        if (target.level() != level) {
            val spot = arrival(target.serverLevel(), target, ARRIVE_DISTANCE) ?: return
            warden.teleportTo(target.serverLevel(), spot.x, spot.y, spot.z, emptySet(), yawTowards(spot, target.position()), 0f)
            data.doomDimension = target.serverLevel().dimension()
            data.changed()
            emerge(server, target)
            return
        }
        val dx = target.x - warden.x
        val dz = target.z - warden.z
        val distance = sqrt(dx * dx + dz * dz)
        if (distance > CHASE_LIMIT) {
            val spot = arrival(level, target, ARRIVE_DISTANCE) ?: return
            warden.setPos(spot.x, spot.y, spot.z)
            face(warden, target.position())
            emerge(server, target)
            return
        }
        val reach = warden.bbWidth / 2.0 + 6.0
        if (distance > reach * 0.8) {
            val step = minOf(WALK_SPEED, distance - reach * 0.8)
            val nx = warden.x + dx / distance * step
            val nz = warden.z + dz / distance * step
            val ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Mth.floor(nx), Mth.floor(nz)).toDouble()
            val ny = warden.y + Mth.clamp(ground - warden.y, -1.0, 1.0)
            warden.setPos(nx, ny, nz)
        }
        face(warden, target.position())
        if (attackCooldown > 0) attackCooldown--
        if (boomCooldown > 0) boomCooldown--
        if (distance <= reach && attackCooldown <= 0) {
            attackCooldown = 40
            level.broadcastEntityEvent(warden, 4.toByte())
            target.hurt(warden.damageSources().mobAttack(warden), ATTACK.toFloat())
            target.knockback(2.5, warden.x - target.x, warden.z - target.z)
            target.hurtMarked = true
            level.playSound(null, target.blockPosition(), SoundEvents.WARDEN_ATTACK_IMPACT, SoundSource.HOSTILE, 3f, 0.5f)
        } else if (distance in reach..SONIC_RANGE && boomCooldown <= 0) {
            boomCooldown = 100
            sonicBoom(level, warden, target)
        }
    }

    private fun sonicBoom(level: ServerLevel, warden: Warden, target: ServerPlayer) {
        level.broadcastEntityEvent(warden, 62.toByte())
        val from = Vec3(warden.x, warden.y + warden.bbHeight * 0.7, warden.z)
        val to = target.eyePosition
        val span = to.subtract(from)
        val steps = (span.length() / 2.0).toInt().coerceIn(1, 200)
        for (i in 1..steps) {
            val point = from.add(span.scale(i / steps.toDouble()))
            level.sendParticles(ParticleTypes.SONIC_BOOM, point.x, point.y, point.z, 1, 0.0, 0.0, 0.0, 0.0)
        }
        level.playSound(null, warden.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 6f, 0.6f)
        level.playSound(null, target.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 3f, 0.6f)
        target.hurt(level.damageSources().sonicBoom(warden), 14f)
        val push = span.normalize()
        target.push(push.x * 2.5, 0.6, push.z * 2.5)
        target.hurtMarked = true
    }

    private fun emerge(server: MinecraftServer, target: ServerPlayer) {
        emergeUntil = server.tickCount + EMERGE_TICKS
        val level = target.serverLevel()
        val warden = SorcererWorldData.get(server).doomWarden?.let { level.getEntity(it) } as? Warden ?: return
        warden.pose = Pose.EMERGING
        target.playNotifySound(SoundEvents.WARDEN_EMERGE, SoundSource.HOSTILE, 2f, 0.5f)
    }

    private fun face(warden: Warden, at: Vec3) {
        val yaw = yawTowards(warden.position(), at)
        warden.yRot = yaw
        warden.yBodyRot = yaw
        warden.yHeadRot = yaw
    }

    private fun yawTowards(from: Vec3, to: Vec3): Float =
        (Mth.atan2(to.z - from.z, to.x - from.x) * Mth.RAD_TO_DEG).toFloat() - 90f

    /** Ближайший живой игрок не в творческом режиме. Сначала в том же измерении, потом в любом. */
    private fun target(server: MinecraftServer, level: ServerLevel): ServerPlayer? {
        val candidates = server.playerList.players.filter { it.isAlive && !it.isSpectator && !it.isCreative }
        val warden = SorcererWorldData.get(server).doomWarden?.let { level.getEntity(it) } ?: return candidates.firstOrNull()
        return candidates.filter { it.level() == level }.minByOrNull { it.distanceToSqr(warden) } ?: candidates.firstOrNull()
    }

    private fun arrival(level: ServerLevel, player: ServerPlayer, distance: Double): Vec3? {
        repeat(8) {
            val angle = level.random.nextDouble() * Math.PI * 2.0
            val x = player.x + cos(angle) * distance
            val z = player.z + sin(angle) * distance
            val pos = BlockPos.containing(x, player.y, z)
            if (level.isLoaded(pos)) {
                val y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pos.x, pos.z).toDouble()
                return Vec3(x, y, z)
            }
        }
        val fallback = player.blockPosition()
        return if (level.isLoaded(fallback)) player.position() else null
    }

    private fun surfaceBelow(level: ServerLevel, pos: BlockPos): BlockPos {
        val top = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pos.x, pos.z)
        return BlockPos(pos.x, (top - 1).coerceAtLeast(level.minBuildHeight), pos.z)
    }

    private fun syncBossBar(server: MinecraftServer) {
        val online = server.playerList.players.toSet()
        for (player in bossBar.players.toList()) {
            if (player !in online) bossBar.removePlayer(player)
        }
        for (player in online) bossBar.addPlayer(player)
    }

    fun onLogin(player: ServerPlayer) {
        val active = active(player.server)
        RuntimeHooks.sendPayload(player, DoomSkyPayload(active))
        if (active) bossBar.addPlayer(player)
    }

    fun onDeath(entity: LivingEntity, source: DamageSource) {
        if (!entity.tags.contains(TAG)) return
        val server = entity.server ?: return
        val data = SorcererWorldData.get(server)
        if (data.doomWarden != entity.uuid) return
        val killer = (source.entity as? ServerPlayer) ?: (entity.killCredit as? ServerPlayer)
        stop(server, data)
        if (killer != null) {
            killer.setGameMode(GameType.CREATIVE)
            ModAdvancements.grantAtWhatCost(killer)
            server.playerList.broadcastSystemMessage(
                Component.translatable("reallyusefulribbits.delete_world.defeated", killer.displayName).withStyle(ChatFormatting.DARK_RED),
                false,
            )
        }
    }

    private fun stop(server: MinecraftServer, data: SorcererWorldData) {
        data.doomActive = false
        data.doomWarden = null
        data.doomBloomOrigin = null
        data.changed()
        bloom = null
        missingTicks = 0
        bossBar.removeAllPlayers()
        showSky(server, false)
    }

    private fun showSky(server: MinecraftServer, active: Boolean) {
        if (skyShown == active) return
        skyShown = active
        for (player in server.playerList.players) RuntimeHooks.sendPayload(player, DoomSkyPayload(active))
    }

    private fun bloomTick(server: MinecraftServer, data: SorcererWorldData) {
        val origin = data.doomBloomOrigin ?: return
        val level = server.getLevel(data.doomDimension) ?: return
        val current = bloom?.takeIf { it.origin == origin } ?: Bloom(origin).also { bloom = it }
        current.tick(level)
    }

    /** Ковёр скалка по поверхности от точки призыва. Ходит только по загруженным чанкам. */
    private class Bloom(val origin: BlockPos) {
        private val queue = ArrayDeque<BlockPos>()
        private val seen = LongOpenHashSet()

        init {
            queue.addLast(origin)
            seen.add(origin.asLong())
        }

        fun tick(level: ServerLevel) {
            var grown = 0
            var visits = 0
            while (queue.isNotEmpty() && grown < BLOOM_PER_TICK && visits < BLOOM_VISITS) {
                val pos = queue.removeFirst()
                visits++
                if (!level.isLoaded(pos)) continue
                val state = level.getBlockState(pos)
                if (!state.`is`(Blocks.SCULK)) {
                    if (!canGrow(level, pos, state)) continue
                    level.setBlock(pos, Blocks.SCULK.defaultBlockState(), 3)
                    grown++
                    decorate(level, pos)
                }
                if (seen.size >= BLOOM_MEMORY) continue
                for (direction in Direction.entries) {
                    val next = pos.relative(direction)
                    val dx = next.x - origin.x
                    val dz = next.z - origin.z
                    if (dx * dx + dz * dz > BLOOM_RADIUS * BLOOM_RADIUS) continue
                    if (seen.add(next.asLong())) queue.addLast(next)
                }
            }
        }

        private fun canGrow(level: ServerLevel, pos: BlockPos, state: BlockState): Boolean {
            if (state.isAir || !state.fluidState.isEmpty || state.hasBlockEntity()) return false
            if (state.getDestroySpeed(level, pos) < 0f || state.`is`(Blocks.OBSIDIAN)) return false
            if (!state.isCollisionShapeFullBlock(level, pos)) return false
            return Direction.entries.any { direction ->
                val side = level.getBlockState(pos.relative(direction))
                side.isAir || !side.isCollisionShapeFullBlock(level, pos.relative(direction))
            }
        }

        private fun decorate(level: ServerLevel, pos: BlockPos) {
            val above = pos.above()
            if (!level.getBlockState(above).isAir) return
            val roll = level.random.nextInt(160)
            when {
                roll == 0 -> level.setBlock(above, Blocks.SCULK_SHRIEKER.defaultBlockState().setValue(SculkShriekerBlock.CAN_SUMMON, false), 3)
                roll < 4 -> level.setBlock(above, Blocks.SCULK_SENSOR.defaultBlockState(), 3)
            }
        }
    }
}
