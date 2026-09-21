package com.reallyusefulribbits.mod.profession

import com.reallyusefulribbits.mod.config.ModConfig
import com.reallyusefulribbits.mod.config.ServerConfig
import com.reallyusefulribbits.mod.inventory.GroundPickup
import com.reallyusefulribbits.mod.inventory.RibbitBags
import com.reallyusefulribbits.mod.logic.FishingTiming
import com.reallyusefulribbits.mod.logic.ProfessionKind
import com.reallyusefulribbits.mod.util.LookAt
import com.reallyusefulribbits.mod.util.work
import com.reallyusefulribbits.mod.world.ContainerSupport
import com.reallyusefulribbits.mod.world.WorldScan
import com.yungnickyoung.minecraft.ribbits.entity.RibbitEntity
import net.minecraft.core.BlockPos
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.storage.loot.BuiltInLootTables
import net.minecraft.world.level.storage.loot.LootParams
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets
import net.minecraft.world.level.storage.loot.parameters.LootContextParams
import net.minecraft.world.phys.Vec3

object FishermanAi {
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

        if (RibbitBags.isFull(data, ProfessionKind.FISHERMAN) && data.containerPos != null) {
            ribbit.setFishing(false)
            data.fishingActive = false
            goDeposit(level, ribbit)
            return
        }

        val water = data.waterPos
        if (water == null) {
            ribbit.setFishing(false)
            return
        }
        val shore = shorePos(level, water) ?: water
        val sit = Vec3(shore.x + 0.5, shore.y + 1.0, shore.z + 0.5)
        val waterCenter = Vec3(water.x + 0.5, sit.y, water.z + 0.5)
        val towardWater = waterCenter.subtract(sit)
        val target = if (towardWater.lengthSqr() > 1.0e-6) {
            sit.add(towardWater.normalize().scale(1.35))
        } else {
            sit
        }
        LookAt.block(ribbit, water, 0.35)
        if (ribbit.distanceToSqr(target) > ModConfig.FISHER_SIT_REACH_SQ) {
            ribbit.setFishing(false)
            data.fishingActive = false
            ribbit.navigation.moveTo(target.x, target.y, target.z, 1.0)
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
        ribbit.setFishing(true)
        LookAt.block(ribbit, water, 0.35)
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
            RibbitBags.insert(data, ProfessionKind.FISHERMAN, stack)
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
        val target = Vec3(pos.x + 0.5, pos.y.toDouble(), pos.z + 0.5)
        if (ribbit.distanceToSqr(target) > 4.0) {
            ribbit.navigation.moveTo(target.x, target.y, target.z, 1.05)
            return
        }
        ribbit.navigation.stop()
        ContainerSupport.openBriefly(level, pos)
        val leftover = ContainerSupport.insertAll(level, pos, RibbitBags.extractAll(data, ProfessionKind.FISHERMAN))
        leftover.forEach { RibbitBags.insert(data, ProfessionKind.FISHERMAN, it) }
    }

    private fun isWater(level: ServerLevel, pos: BlockPos): Boolean =
        !level.getFluidState(pos).isEmpty

    private fun shorePos(level: ServerLevel, water: BlockPos): BlockPos? {
        for (dx in -1..1) {
            for (dz in -1..1) {
                if (dx == 0 && dz == 0) continue
                val pos = water.offset(dx, 0, dz)
                if (level.getFluidState(pos).isEmpty && level.getBlockState(pos.above()).isAir) return pos
            }
        }
        return null
    }
}
