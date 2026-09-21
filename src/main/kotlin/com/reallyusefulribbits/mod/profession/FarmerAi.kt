package com.reallyusefulribbits.mod.profession

import com.reallyusefulribbits.mod.config.ModConfig
import com.reallyusefulribbits.mod.config.ServerConfig
import com.reallyusefulribbits.mod.inventory.RibbitBags
import com.reallyusefulribbits.mod.logic.FarmerTask
import com.reallyusefulribbits.mod.logic.FarmerTaskPlanner
import com.reallyusefulribbits.mod.logic.FarmerWorldView
import com.reallyusefulribbits.mod.logic.ProfessionKind
import com.reallyusefulribbits.mod.util.work
import com.reallyusefulribbits.mod.world.BlockReservation
import com.reallyusefulribbits.mod.world.ContainerSupport
import com.reallyusefulribbits.mod.world.CropSupport
import com.reallyusefulribbits.mod.world.WorldScan
import com.yungnickyoung.minecraft.ribbits.entity.RibbitEntity
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.CaveVines
import net.minecraft.world.phys.Vec3

object FarmerAi {
    fun tick(level: ServerLevel, ribbit: RibbitEntity) {
        val data = ribbit.work()
        val radius = ServerConfig.scanRadius()
        if (level.gameTime - data.lastScanAt >= ModConfig.BIND_SCAN_INTERVAL) {
            data.lastScanAt = level.gameTime
            if (data.containerPos == null || !ContainerSupport.isStorage(level, data.containerPos!!)) {
                data.containerPos = WorldScan.nearestContainer(level, ribbit.blockPosition(), radius)
            }
            if (data.farmOrigin == null || !CropSupport.isFarmBlock(level, data.farmOrigin!!)) {
                data.farmOrigin = WorldScan.nearestFarmOrigin(level, ribbit.blockPosition(), radius)
            }
        }
        val origin = data.farmOrigin ?: return
        val farm = if (level.gameTime - data.lastFarmScanAt >= ModConfig.FARM_RESCAN_INTERVAL) {
            data.lastFarmScanAt = level.gameTime
            WorldScan.farmBlocks(level, origin, radius)
        } else {
            WorldScan.farmBlocks(level, origin, radius)
        }
        val jobs = scanJobs(level, ribbit, farm)
        val view = FarmerWorldView(
            inventoryFull = RibbitBags.isFull(data, ProfessionKind.FARMER),
            inventoryHasItems = RibbitBags.hasItems(data, ProfessionKind.FARMER),
            hasMatureCrop = jobs.harvest != null,
            hasTillable = jobs.till != null,
            hasEmptyFarmland = jobs.plant != null,
            hasPlantable = hasPlantable(level, ribbit),
            hasImmatureCrop = jobs.water != null,
        )
        var task = data.farmerTask()
        if (!FarmerTaskPlanner.shouldKeep(task, data.taskTicks, view)) {
            task = FarmerTaskPlanner.next(view)
            data.task = task.name
            data.taskTicks = 0
            ribbit.setWatering(false)
        }
        data.taskTicks++
        when (task) {
            FarmerTask.DEPOSIT -> deposit(level, ribbit)
            FarmerTask.HARVEST -> actOn(level, ribbit, jobs.harvest) { harvest(level, ribbit, it) }
            FarmerTask.TILL -> actOn(level, ribbit, jobs.till) { CropSupport.till(level, it) }
            FarmerTask.PLANT -> plant(level, ribbit, jobs.plant)
            FarmerTask.WATER -> water(level, ribbit, jobs.water)
            FarmerTask.IDLE -> ribbit.setWatering(false)
        }
    }

    private data class Jobs(
        val harvest: BlockPos?,
        val till: BlockPos?,
        val plant: BlockPos?,
        val water: BlockPos?,
    )

    private fun scanJobs(level: ServerLevel, ribbit: RibbitEntity, farm: List<BlockPos>): Jobs {
        val now = level.gameTime
        var harvest: BlockPos? = null
        var till: BlockPos? = null
        var plant: BlockPos? = null
        var water: BlockPos? = null
        val extras = farm.flatMap { pos ->
            listOf(pos, pos.above(), pos.north(), pos.south(), pos.east(), pos.west())
        }.distinct()
        for (pos in extras) {
            val state = level.getBlockState(pos)
            if (harvest == null && CropSupport.isMatureCrop(level, pos)) {
                if (CaveVines.hasGlowBerries(state) && !CropSupport.canReachBerries(ribbit.eyeY, pos)) continue
                harvest = pos
            }
            if (till == null && CropSupport.isTillable(state)) till = pos
            if (plant == null && CropSupport.isEmptyFarmland(level, pos)) plant = pos
            if (water == null && CropSupport.isImmatureCrop(level, pos)) water = pos
            if (harvest != null && till != null && plant != null && water != null) break
        }
        listOfNotNull(harvest, till, plant, water).forEach {
            BlockReservation.tryClaim(level, it, ribbit.id, now)
        }
        BlockReservation.cleanup(now)
        return Jobs(harvest, till, plant, water)
    }

    private fun actOn(level: ServerLevel, ribbit: RibbitEntity, pos: BlockPos?, action: (BlockPos) -> Unit) {
        if (pos == null) return
        if (!walkTo(ribbit, pos)) return
        action(pos)
        BlockReservation.release(level, pos, ribbit.id)
        ribbit.work().taskTicks = FarmerTaskPlanner.MIN_TASK_TICKS + FarmerTaskPlanner.SWITCH_COOLDOWN_TICKS
    }

    private fun harvest(level: ServerLevel, ribbit: RibbitEntity, pos: BlockPos) {
        val drops = CropSupport.harvest(level, pos, ribbit)
        for (stack in drops) {
            val leftover = RibbitBags.insert(ribbit.work(), ProfessionKind.FARMER, stack)
            if (!leftover.isEmpty) {
                net.minecraft.world.entity.item.ItemEntity(level, pos.x + 0.5, pos.y + 0.4, pos.z + 0.5, leftover)
                    .also { level.addFreshEntity(it) }
            }
        }
    }

    private fun plant(level: ServerLevel, ribbit: RibbitEntity, soil: BlockPos?) {
        if (soil == null) return
        if (!walkTo(ribbit, soil)) return
        val data = ribbit.work()
        var seed = RibbitBags.takeOne(data, ProfessionKind.FARMER) { CropSupport.plantableBlock(it) != null }
        if (seed.isEmpty && data.containerPos != null) {
            val taken = ContainerSupport.extractMatching(level, data.containerPos!!, { CropSupport.plantableBlock(it) != null }, 1)
            if (!taken.isEmpty) {
                ContainerSupport.openBriefly(level, data.containerPos!!)
                seed = taken
            }
        }
        if (!seed.isEmpty && CropSupport.plant(level, soil, seed)) {
            if (seed.count > 0) RibbitBags.insert(data, ProfessionKind.FARMER, seed)
        } else if (!seed.isEmpty) {
            RibbitBags.insert(data, ProfessionKind.FARMER, seed)
        }
        data.taskTicks = FarmerTaskPlanner.MIN_TASK_TICKS + FarmerTaskPlanner.SWITCH_COOLDOWN_TICKS
    }

    private fun water(level: ServerLevel, ribbit: RibbitEntity, pos: BlockPos?) {
        if (pos == null) return
        if (!walkTo(ribbit, pos)) {
            ribbit.setWatering(false)
            return
        }
        ribbit.setWatering(true)
        if (ribbit.work().taskTicks >= ModConfig.WATERING_ANIM_TICKS) {
            CropSupport.water(level, pos)
            ribbit.setWatering(false)
            ribbit.work().taskTicks = FarmerTaskPlanner.MIN_TASK_TICKS + FarmerTaskPlanner.SWITCH_COOLDOWN_TICKS
        }
    }

    private fun deposit(level: ServerLevel, ribbit: RibbitEntity) {
        val data = ribbit.work()
        val pos = data.containerPos ?: return
        if (!walkTo(ribbit, pos, 2.2)) return
        ContainerSupport.openBriefly(level, pos)
        val leftover = ContainerSupport.insertAll(level, pos, RibbitBags.extractAll(data, ProfessionKind.FARMER))
        leftover.forEach { RibbitBags.insert(data, ProfessionKind.FARMER, it) }
        data.taskTicks = FarmerTaskPlanner.MIN_TASK_TICKS + FarmerTaskPlanner.SWITCH_COOLDOWN_TICKS
    }

    private fun hasPlantable(level: ServerLevel, ribbit: RibbitEntity): Boolean {
        val data = ribbit.work()
        if (!RibbitBags.find(data, ProfessionKind.FARMER) { CropSupport.plantableBlock(it) != null }.isEmpty) return true
        val container = data.containerPos ?: return false
        val handler = ContainerSupport.handler(level, container) ?: return false
        for (slot in 0 until handler.slots) {
            if (CropSupport.plantableBlock(handler.getStackInSlot(slot)) != null) return true
        }
        return false
    }

    private fun walkTo(ribbit: RibbitEntity, pos: BlockPos, reach: Double = Math.sqrt(ModConfig.WORK_REACH_SQ)): Boolean {
        val target = Vec3(pos.x + 0.5, pos.y.toDouble(), pos.z + 0.5)
        if (ribbit.distanceToSqr(target) <= reach * reach) {
            ribbit.navigation.stop()
            return true
        }
        ribbit.navigation.moveTo(target.x, target.y, target.z, 1.0)
        return false
    }
}
