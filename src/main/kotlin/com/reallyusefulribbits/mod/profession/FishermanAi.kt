package com.reallyusefulribbits.mod.profession

import com.reallyusefulribbits.mod.config.ModConfig
import com.reallyusefulribbits.mod.config.ServerConfig
import com.reallyusefulribbits.mod.inventory.GroundPickup
import com.reallyusefulribbits.mod.inventory.RibbitBags
import com.reallyusefulribbits.mod.logic.FishingTiming
import com.reallyusefulribbits.mod.logic.ProfessionKind
import com.reallyusefulribbits.mod.util.LookAt
import com.reallyusefulribbits.mod.util.professionKind
import com.reallyusefulribbits.mod.util.work
import com.reallyusefulribbits.mod.world.ContainerSupport
import com.reallyusefulribbits.mod.world.WorldScan
import com.yungnickyoung.minecraft.ribbits.entity.RibbitEntity
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.entity.ai.goal.Goal
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.storage.loot.BuiltInLootTables
import java.util.EnumSet
import net.minecraft.world.level.storage.loot.LootParams
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets
import net.minecraft.world.level.storage.loot.parameters.LootContextParams
import net.minecraft.world.phys.Vec3

object FishermanAi {
    private val chestProgress = HashMap<Int, Double>()

    fun ensureHaulGoal(ribbit: RibbitEntity) {
        if (ribbit.professionKind() != ProfessionKind.FISHERMAN) return
        val present = ribbit.goalSelector.availableGoals.any { it.goal is FisherHaulGoal }
        if (present) return
        ribbit.goalSelector.addGoal(0, FisherHaulGoal(ribbit))
    }

    fun tick(level: ServerLevel, ribbit: RibbitEntity) {
        val data = ribbit.work()
        val radius = ServerConfig.scanRadius()
        GroundPickup.tick(level, ribbit, ProfessionKind.FISHERMAN)
        if (level.gameTime - data.lastScanAt >= ModConfig.BIND_SCAN_INTERVAL) {
            data.lastScanAt = level.gameTime
            if (data.waterPos == null || !isWater(level, data.waterPos!!)) {
                data.waterPos = WorldScan.nearestWater(level, ribbit.blockPosition(), radius)
            }
            if (data.containerPos == null || !ContainerSupport.isStorage(level, data.containerPos!!)) {
                data.containerPos = WorldScan.nearestContainer(level, ribbit.blockPosition(), radius)
            }
        }

        if (RibbitBags.isFull(data, ProfessionKind.FISHERMAN)) {
            ribbit.setFishing(false)
            data.fishingActive = false
            if (data.containerPos == null || !ContainerSupport.isStorage(level, data.containerPos!!)) {
                data.containerPos = WorldScan.nearestContainer(level, ribbit.blockPosition(), radius)
            }
            if (data.containerPos != null) goDeposit(level, ribbit)
            return
        }

        val water = data.waterPos
        if (water == null) {
            ribbit.setFishing(false)
            return
        }
        val shore = shorePos(level, water)
        if (shore == null) {
            ribbit.setFishing(false)
            data.fishingActive = false
            return
        }
        val stand = Vec3(shore.x + 0.5, shore.y + 1.0, shore.z + 0.5)
        val waterCenter = Vec3(water.x + 0.5, stand.y, water.z + 0.5)
        val towardWater = waterCenter.subtract(stand)
        val sit = if (towardWater.lengthSqr() > 1.0e-6) {
            val nudged = stand.add(towardWater.normalize().scale(0.35))
            Vec3(
                nudged.x.coerceIn(shore.x + 0.2, shore.x + 0.8),
                stand.y,
                nudged.z.coerceIn(shore.z + 0.2, shore.z + 0.8),
            )
        } else {
            stand
        }
        LookAt.block(ribbit, water, 0.85)
        if (ribbit.distanceToSqr(sit) > ModConfig.FISHER_SIT_REACH_SQ) {
            ribbit.setFishing(false)
            data.fishingActive = false
            ribbit.navigation.moveTo(sit.x, sit.y, sit.z, 1.0)
            data.navStuck++
            if (data.navStuck >= ModConfig.FISHER_STUCK_TICKS) {
                data.waterPos = null
                data.navStuck = 0
                data.fishingActive = false
            }
            return
        }
        data.navStuck = 0
        ribbit.navigation.stop()
        ribbit.moveTo(sit.x, sit.y, sit.z, ribbit.yRot, ribbit.xRot)
        ribbit.setFishing(true)
        LookAt.block(ribbit, water, 0.85)
        if (!data.fishingActive) {
            startSession(ribbit, water)
        }
        tickFishing(level, ribbit)
    }

    private fun startSession(ribbit: RibbitEntity, water: BlockPos) {
        val data = ribbit.work()
        val times = FishingTiming.roll({ min, max -> ribbit.random.nextInt(min, max) })
        data.fishingActive = true
        data.fishingElapsed = 0
        data.fishingWait = times.waitTicks
        data.fishingApproach = times.approachTicks
        data.fishingBite = times.biteWindowTicks
        data.bobberX = water.x + 0.5
        data.bobberY = water.y + 1.0
        data.bobberZ = water.z + 0.5
    }

    private fun tickFishing(level: ServerLevel, ribbit: RibbitEntity) {
        val data = ribbit.work()
        data.fishingElapsed++
        val times = data.fishingTimes()
        val phase = FishingTiming.phase(data.fishingElapsed, times)
        val bobber = Vec3(data.bobberX, data.bobberY, data.bobberZ)
        when (phase) {
            FishingTiming.Phase.WAIT -> Unit
            FishingTiming.Phase.APPROACH -> {
                val progress = FishingTiming.approachProgress(data.fishingElapsed, times)
                val start = bobber.add(2.4, 0.0, 0.6)
                val pos = start.lerp(bobber, progress)
                level.sendParticles(ParticleTypes.FISHING, pos.x, pos.y, pos.z, 2, 0.08, 0.0, 0.08, 0.01)
                level.sendParticles(ParticleTypes.SPLASH, pos.x, pos.y, pos.z, 1, 0.05, 0.0, 0.05, 0.0)
            }
            FishingTiming.Phase.BITE -> {
                level.sendParticles(ParticleTypes.BUBBLE, bobber.x, bobber.y, bobber.z, 3, 0.1, 0.0, 0.1, 0.02)
            }
            FishingTiming.Phase.CAUGHT -> catchFish(level, ribbit)
        }
    }

    private fun catchFish(level: ServerLevel, ribbit: RibbitEntity) {
        val data = ribbit.work()
        val loot = rollLoot(level, ribbit, Vec3(data.bobberX, data.bobberY, data.bobberZ))
        for (stack in loot) {
            val leftover = RibbitBags.insert(data, ProfessionKind.FISHERMAN, stack)
            if (!leftover.isEmpty) {
                val drop = ItemEntity(level, ribbit.x, ribbit.y + 0.2, ribbit.z, leftover)
                drop.setPickUpDelay(20)
                level.addFreshEntity(drop)
            }
        }
        if (RibbitBags.isFull(data, ProfessionKind.FISHERMAN)) {
            ribbit.setFishing(false)
        }
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, ribbit.x, ribbit.y + 0.8, ribbit.z, 8, 0.3, 0.3, 0.3, 0.02)
        level.sendParticles(ParticleTypes.BUBBLE, data.bobberX, data.bobberY, data.bobberZ, 10, 0.15, 0.1, 0.15, 0.03)
        level.playSound(null, BlockPos.containing(data.bobberX, data.bobberY, data.bobberZ), SoundEvents.FISHING_BOBBER_SPLASH, SoundSource.NEUTRAL, 0.9f, 1.0f)
        level.playSound(null, ribbit.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.NEUTRAL, 0.45f, 1.2f)
        data.fishingActive = false
        data.fishingElapsed = 0
    }

    private fun rollLoot(level: ServerLevel, ribbit: RibbitEntity, origin: Vec3): List<ItemStack> {
        return try {
            val table = level.server.reloadableRegistries().getLootTable(BuiltInLootTables.FISHING)
            val params = LootParams.Builder(level)
                .withParameter(LootContextParams.ORIGIN, origin)
                .withParameter(LootContextParams.TOOL, ItemStack(Items.FISHING_ROD))
                .withParameter(LootContextParams.THIS_ENTITY, ribbit)
                .create(LootContextParamSets.FISHING)
            table.getRandomItems(params)
        } catch (_: Exception) {
            listOf(ItemStack(Items.COD))
        }
    }

    private fun goDeposit(level: ServerLevel, ribbit: RibbitEntity) {
        val data = ribbit.work()
        val pos = data.containerPos ?: return
        LookAt.block(ribbit, pos, 0.5)
        val center = Vec3(pos.x + 0.5, pos.y.toDouble(), pos.z + 0.5)
        val chestDist = ribbit.distanceToSqr(center)
        val stand = standNear(level, pos, ribbit)
        val standDist = if (stand != null) ribbit.distanceToSqr(stand) else Double.MAX_VALUE
        val arrived = chestDist <= ModConfig.CONTAINER_REACH_SQ || standDist <= 2.56
        val previous = chestProgress[ribbit.id]
        if (previous != null && chestDist >= previous - 0.02) data.navStuck++ else data.navStuck = 0
        chestProgress[ribbit.id] = chestDist
        val closeEnoughToGiveUp = data.navStuck >= 20 && chestDist <= 9.0
        if (!arrived && !closeEnoughToGiveUp) {
            val goal = stand ?: center
            val walking = ribbit.navigation.moveTo(goal.x, goal.y, goal.z, 1.05)
            if (!walking) ribbit.moveControl.setWantedPosition(goal.x, goal.y, goal.z, 1.05)
            return
        }
        data.navStuck = 0
        chestProgress.remove(ribbit.id)
        ribbit.navigation.stop()
        LookAt.block(ribbit, pos, 0.5)
        ContainerSupport.openBriefly(level, pos)
        val leftover = ContainerSupport.insertAll(level, pos, RibbitBags.extractAll(data, ProfessionKind.FISHERMAN))
        leftover.forEach { RibbitBags.insert(data, ProfessionKind.FISHERMAN, it) }
    }

    private fun standNear(level: ServerLevel, container: BlockPos, ribbit: RibbitEntity): Vec3? {
        var best: Vec3? = null
        var bestDist = Double.MAX_VALUE
        for (dir in Direction.Plane.HORIZONTAL) {
            val side = container.relative(dir)
            for (feet in listOf(side, side.above())) {
                if (!level.getBlockState(feet).isAir) continue
                if (!level.getFluidState(feet).isEmpty) continue
                val ground = feet.below()
                if (!canStandOn(level, ground)) continue
                val spot = Vec3(feet.x + 0.5, feet.y.toDouble(), feet.z + 0.5)
                val dist = ribbit.distanceToSqr(spot)
                if (dist < bestDist) {
                    bestDist = dist
                    best = spot
                }
            }
        }
        return best
    }

    private fun isWater(level: ServerLevel, pos: BlockPos): Boolean =
        !level.getFluidState(pos).isEmpty

    private fun shorePos(level: ServerLevel, water: BlockPos): BlockPos? {
        var best: BlockPos? = null
        var bestDist = Double.MAX_VALUE
        for (dx in -1..1) {
            for (dz in -1..1) {
                if (dx == 0 && dz == 0) continue
                for (dy in 0..1) {
                    val ground = water.offset(dx, dy, dz)
                    if (!canStandOn(level, ground)) continue
                    val dist = ground.distSqr(water)
                    if (dist < bestDist) {
                        bestDist = dist
                        best = ground
                    }
                }
            }
        }
        return best
    }

    private fun canStandOn(level: ServerLevel, ground: BlockPos): Boolean {
        if (!level.getFluidState(ground).isEmpty) return false
        val below = level.getBlockState(ground)
        if (!below.isFaceSturdy(level, ground, net.minecraft.core.Direction.UP)) return false
        val feet = ground.above()
        if (!level.getFluidState(feet).isEmpty) return false
        return level.getBlockState(feet).isAir
    }

    fun haul(level: ServerLevel, ribbit: RibbitEntity) {
        ribbit.setFishing(false)
        ribbit.work().fishingActive = false
        goDeposit(level, ribbit)
    }
}

private class FisherHaulGoal(private val ribbit: RibbitEntity) : Goal() {
    init {
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK))
    }

    override fun canUse(): Boolean = hauling()

    override fun canContinueToUse(): Boolean = hauling()

    override fun start() {
        ribbit.setFishing(false)
        ribbit.work().fishingActive = false
    }

    override fun tick() {
        val level = ribbit.level() as? ServerLevel ?: return
        FishermanAi.haul(level, ribbit)
    }

    private fun hauling(): Boolean {
        if (ribbit.professionKind() != ProfessionKind.FISHERMAN) return false
        val data = ribbit.work()
        return RibbitBags.isFull(data, ProfessionKind.FISHERMAN) && data.containerPos != null
    }
}
