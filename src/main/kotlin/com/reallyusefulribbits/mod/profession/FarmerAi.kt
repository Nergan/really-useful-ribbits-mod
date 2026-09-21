package com.reallyusefulribbits.mod.profession

import com.reallyusefulribbits.mod.config.ModConfig
import com.reallyusefulribbits.mod.config.ServerConfig
import com.reallyusefulribbits.mod.inventory.GroundPickup
import com.reallyusefulribbits.mod.inventory.RibbitBags
import com.reallyusefulribbits.mod.logic.FarmerTask
import com.reallyusefulribbits.mod.logic.FarmerTaskPlanner
import com.reallyusefulribbits.mod.logic.FarmerWorldView
import com.reallyusefulribbits.mod.logic.ProfessionKind
import com.reallyusefulribbits.mod.util.LookAt
import com.reallyusefulribbits.mod.util.professionKind
import com.reallyusefulribbits.mod.util.work
import com.reallyusefulribbits.mod.world.BlockReservation
import com.reallyusefulribbits.mod.world.ContainerSupport
import com.reallyusefulribbits.mod.world.CropSupport
import com.reallyusefulribbits.mod.world.WorldScan
import com.yungnickyoung.minecraft.ribbits.entity.RibbitEntity
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.ai.goal.Goal
import net.minecraft.world.level.block.CaveVines
import net.minecraft.world.phys.Vec3
import java.util.EnumSet

object FarmerAi {
    private val walks = HashMap<Int, WalkOrder>()
    private val carried = HashMap<Int, WalkOrder>()
    private val applied = HashMap<Int, WalkOrder>()
    private val plantLocks = HashMap<Int, BlockPos>()

    fun ensureWorkGoal(ribbit: RibbitEntity) {
        if (ribbit.professionKind() != ProfessionKind.FARMER) return
        val present = ribbit.goalSelector.availableGoals.any { it.goal is FarmerWorkGoal }
        if (present) return
        ribbit.goalSelector.addGoal(0, FarmerWorkGoal(ribbit))
    }

    fun hasWalk(ribbit: RibbitEntity): Boolean = walks.containsKey(ribbit.id)

    /** Путь ставится из цели, до navigation.tick. Иначе прогулка риббита его стирает. */
    fun follow(ribbit: RibbitEntity) {
        val order = walks[ribbit.id] ?: return
        val nav = ribbit.navigation
        val previous = applied[ribbit.id]
        val changed = previous == null || previous.x != order.x || previous.y != order.y || previous.z != order.z
        if (changed || !nav.isInProgress || ribbit.tickCount % 10 == 0) {
            nav.moveTo(order.x, order.y, order.z, order.speed)
            applied[ribbit.id] = order
        }
    }

    fun tick(level: ServerLevel, ribbit: RibbitEntity) {
        val kept = walks.remove(ribbit.id)
        if (kept != null) carried[ribbit.id] = kept
        try {
            tickWork(level, ribbit)
        } finally {
            carried.remove(ribbit.id)
        }
    }

    private fun tickWork(level: ServerLevel, ribbit: RibbitEntity) {
        val data = ribbit.work()
        val radius = ServerConfig.scanRadius()
        GroundPickup.tick(level, ribbit, ProfessionKind.FARMER)
        if (level.gameTime - data.lastScanAt >= ModConfig.BIND_SCAN_INTERVAL) {
            data.lastScanAt = level.gameTime
            if (data.containerPos == null || !ContainerSupport.isStorage(level, data.containerPos!!)) {
                data.containerPos = WorldScan.nearestContainer(level, ribbit.blockPosition(), radius)
            }
            if (data.farmOrigin == null || !CropSupport.isFarmBlock(level, data.farmOrigin!!)) {
                val next = WorldScan.nearestFarmOrigin(level, ribbit.blockPosition(), radius)
                if (next != data.farmOrigin) {
                    data.farmMemory.clear()
                    data.lastFarmScanAt = 0
                    data.farmOrigin = next
                }
            }
        }
        if (data.cooldown > 0) data.cooldown--
        val origin = data.farmOrigin ?: return
        if (data.farmMemory.isEmpty() || level.gameTime - data.lastFarmScanAt >= ModConfig.FARM_RESCAN_INTERVAL) {
            data.lastFarmScanAt = level.gameTime
            data.farmMemory.clear()
            data.farmMemory += WorldScan.allWorkBlocks(level, origin, maxOf(radius, ModConfig.FARM_CLAIM_RADIUS))
        }
        val farm = data.farmMemory.toList()
        val jobs = scanJobs(level, ribbit, farm)
        val view = FarmerWorldView(
            inventoryFull = RibbitBags.isFull(data, ProfessionKind.FARMER),
            inventoryHasItems = RibbitBags.hasItems(data, ProfessionKind.FARMER),
            hasMatureCrop = jobs.harvest != null,
            hasTillable = jobs.till != null,
            hasEmptyFarmland = jobs.plant != null,
            hasPlantable = hasPlantable(level, ribbit),
            hasImmatureCrop = jobs.water != null && data.cooldown <= 0,
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
        val farmSet = farm.toHashSet()
        val data = ribbit.work()
        var harvest: BlockPos? = null
        var till: BlockPos? = null
        var tillDist = Double.MAX_VALUE
        var plant: BlockPos? = null
        var water: BlockPos? = null
        var harvestDist = Double.MAX_VALUE
        var plantDist = Double.MAX_VALUE
        var waterDist = Double.MAX_VALUE
        val cropScan = LinkedHashSet<BlockPos>()
        for (pos in farm) {
            cropScan += pos
            for (dx in -2..2) {
                for (dz in -2..2) {
                    cropScan += pos.offset(dx, 0, dz)
                }
            }
            for (dy in 0..8) cropScan += pos.above(dy)
        }
        for (pos in cropScan) {
            val state = level.getBlockState(pos)
            val dist = ribbit.distanceToSqr(pos.x + 0.5, pos.y.toDouble(), pos.z + 0.5)
            if (dist < harvestDist && CropSupport.isMatureCrop(level, pos)) {
                if (CaveVines.hasGlowBerries(state) && !CropSupport.canReachBerries(ribbit.eyeY, pos)) continue
                val preview = CropSupport.previewDrops(level, pos, ribbit)
                if (!RibbitBags.canInsertAll(data, ProfessionKind.FARMER, preview)) continue
                harvest = pos
                harvestDist = dist
            }
            if (dist < plantDist && CropSupport.isEmptyFarmland(level, pos)) {
                plant = pos
                plantDist = dist
            }
            if (dist < waterDist && CropSupport.needsWater(level, pos)) {
                water = pos
                waterDist = dist
            }
        }
        val tillCandidates = LinkedHashSet<BlockPos>()
        tillCandidates.addAll(data.farmMemory)
        for (pos in farm) {
            for (dir in Direction.Plane.HORIZONTAL) {
                val neighbor = pos.relative(dir)
                if (isInteriorHole(level, neighbor, farmSet)) tillCandidates += neighbor
            }
        }
        for (pos in tillCandidates) {
            if (!CropSupport.isTillable(level.getBlockState(pos))) continue
            val dist = ribbit.distanceToSqr(pos.x + 0.5, pos.y.toDouble(), pos.z + 0.5)
            if (dist < tillDist) {
                till = pos
                tillDist = dist
            }
        }
        val plantTarget = stick(ribbit.id, plant) { CropSupport.isEmptyFarmland(level, it) }
        listOfNotNull(harvest, till, plantTarget, water).forEach {
            BlockReservation.tryClaim(level, it, ribbit.id, now)
        }
        BlockReservation.cleanup(now)
        return Jobs(harvest, till, plantTarget, water)
    }

    /** Одна грядка, пока не посадит: иначе цель прыгает каждый тик и путь собирается заново. */
    private fun stick(id: Int, nearest: BlockPos?, still: (BlockPos) -> Boolean): BlockPos? {
        val locked = plantLocks[id]
        if (locked != null && still(locked)) return locked
        if (nearest != null) plantLocks[id] = nearest else plantLocks.remove(id)
        return nearest
    }

    private fun isInteriorHole(level: ServerLevel, pos: BlockPos, farm: Set<BlockPos>): Boolean {
        if (!CropSupport.isTillable(level.getBlockState(pos))) return false
        var neighbors = 0
        for (dir in Direction.Plane.HORIZONTAL) {
            val next = pos.relative(dir)
            if (farm.contains(next) || CropSupport.isFarmBlock(level, next)) neighbors++
        }
        return neighbors >= 4
    }

    private fun actOn(level: ServerLevel, ribbit: RibbitEntity, pos: BlockPos?, action: (BlockPos) -> Unit) {
        if (pos == null) return
        if (!walkTo(level, ribbit, pos)) return
        action(pos)
        BlockReservation.release(level, pos, ribbit.id)
        ribbit.work().taskTicks = FarmerTaskPlanner.MIN_TASK_TICKS + FarmerTaskPlanner.SWITCH_COOLDOWN_TICKS
    }

    private fun harvest(level: ServerLevel, ribbit: RibbitEntity, pos: BlockPos) {
        val preview = CropSupport.previewDrops(level, pos, ribbit)
        if (!RibbitBags.canInsertAll(ribbit.work(), ProfessionKind.FARMER, preview)) return
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
        val data = ribbit.work()
        val pocket = RibbitBags.find(data, ProfessionKind.FARMER) { CropSupport.plantableBlock(it) != null }
        if (pocket.isEmpty) {
            val chest = data.containerPos ?: return
            if (!walkTo(level, ribbit, chest, Math.sqrt(ModConfig.CONTAINER_REACH_SQ), 1.15, false)) return
            LookAt.block(ribbit, chest, 0.5)
            ContainerSupport.openBriefly(level, chest)
            var grabbed = 0
            while (grabbed < 4 && !RibbitBags.isFull(data, ProfessionKind.FARMER)) {
                val taken = ContainerSupport.extractMatching(level, chest, { CropSupport.plantableBlock(it) != null }, 64)
                if (taken.isEmpty) break
                val leftover = RibbitBags.insert(data, ProfessionKind.FARMER, taken)
                if (!leftover.isEmpty) {
                    ContainerSupport.insertAll(level, chest, listOf(leftover))
                    break
                }
                grabbed++
            }
            return
        }
        if (!walkTo(level, ribbit, soil)) return
        val seed = RibbitBags.takeOne(data, ProfessionKind.FARMER) { CropSupport.plantableBlock(it) != null }
        if (seed.isEmpty) return
        if (!CropSupport.plant(level, soil, seed) && !seed.isEmpty) {
            RibbitBags.insert(data, ProfessionKind.FARMER, seed)
        }
        data.taskTicks = FarmerTaskPlanner.MIN_TASK_TICKS + FarmerTaskPlanner.SWITCH_COOLDOWN_TICKS
    }

    private fun water(level: ServerLevel, ribbit: RibbitEntity, pos: BlockPos?) {
        if (pos == null) return
        LookAt.block(ribbit, pos, 0.4)
        if (!walkTo(level, ribbit, pos)) {
            ribbit.setWatering(false)
            return
        }
        LookAt.block(ribbit, pos, 0.4)
        ribbit.setWatering(true)
        if (ribbit.work().taskTicks >= ModConfig.WATERING_ANIM_TICKS) {
            LookAt.block(ribbit, pos, 0.4)
            CropSupport.water(level, pos)
            ribbit.setWatering(false)
            ribbit.work().cooldown = ModConfig.WATER_COOLDOWN_TICKS
            ribbit.work().taskTicks = FarmerTaskPlanner.MIN_TASK_TICKS + FarmerTaskPlanner.SWITCH_COOLDOWN_TICKS
        }
    }

    private fun deposit(level: ServerLevel, ribbit: RibbitEntity) {
        val data = ribbit.work()
        val pos = data.containerPos ?: return
        if (!walkTo(level, ribbit, pos, Math.sqrt(ModConfig.CONTAINER_REACH_SQ), 1.15, false)) return
        LookAt.block(ribbit, pos, 0.5)
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

    private fun walkTo(
        level: ServerLevel,
        ribbit: RibbitEntity,
        pos: BlockPos,
        reach: Double = Math.sqrt(ModConfig.WORK_REACH_SQ),
        speed: Double = 1.15,
        allowStuckArrive: Boolean = false,
    ): Boolean {
        val target = Vec3(pos.x + 0.5, pos.y.toDouble(), pos.z + 0.5)
        if (ribbit.distanceToSqr(target) <= reach * reach) {
            ribbit.navigation.stop()
            ribbit.work().navStuck = 0
            applied.remove(ribbit.id)
            return true
        }
        val previous = carried[ribbit.id]
        val order = if (previous != null && previous.work == pos.asLong()) {
            previous.copy(speed = speed)
        } else {
            val spot = standSpot(level, ribbit, pos, reach)
            WalkOrder(pos.asLong(), spot.x, spot.y, spot.z, speed)
        }
        walks[ribbit.id] = order
        ribbit.work().navStuck++
        if (allowStuckArrive && ribbit.work().navStuck > 80 && ribbit.distanceToSqr(target) < 4.0) {
            ribbit.work().navStuck = 0
            return true
        }
        return false
    }

    /** Ноги в воздухе над блоком или на соседней клетке, а не внутри пашни. */
    private fun standSpot(level: ServerLevel, ribbit: RibbitEntity, pos: BlockPos, reach: Double): Vec3 {
        val reachSq = reach * reach
        var best: Vec3? = null
        var bestDist = Double.MAX_VALUE
        val feet = ArrayList<BlockPos>(16)
        feet += pos.above()
        for (dir in Direction.Plane.HORIZONTAL) {
            feet += pos.relative(dir)
            feet += pos.relative(dir).above()
        }
        for (dx in -1..1) {
            for (dz in -1..1) {
                if (dx == 0 || dz == 0) continue
                feet += pos.offset(dx, 1, dz)
            }
        }
        for (spotFeet in feet) {
            if (!canStand(level, spotFeet)) continue
            val spot = Vec3(spotFeet.x + 0.5, spotFeet.y.toDouble(), spotFeet.z + 0.5)
            val dx = spot.x - (pos.x + 0.5)
            val dy = spot.y - pos.y
            val dz = spot.z - (pos.z + 0.5)
            if (dx * dx + dy * dy + dz * dz > reachSq) continue
            val dist = ribbit.distanceToSqr(spot)
            if (dist < bestDist) {
                bestDist = dist
                best = spot
            }
        }
        return best ?: Vec3(pos.x + 0.5, pos.y + 1.0, pos.z + 0.5)
    }

    private fun canStand(level: ServerLevel, feet: BlockPos): Boolean {
        if (!level.getFluidState(feet).isEmpty) return false
        if (!level.getBlockState(feet).getCollisionShape(level, feet).isEmpty) return false
        val ground = feet.below()
        if (!level.getFluidState(ground).isEmpty) return false
        val groundState = level.getBlockState(ground)
        return !groundState.getCollisionShape(level, ground).isEmpty || CropSupport.isFarmBlock(level, ground)
    }

    private data class WalkOrder(
        val work: Long,
        val x: Double,
        val y: Double,
        val z: Double,
        val speed: Double,
    )
}

private class FarmerWorkGoal(private val ribbit: RibbitEntity) : Goal() {
    init {
        setFlags(EnumSet.of(Flag.MOVE))
    }

    override fun canUse(): Boolean = walking()

    override fun canContinueToUse(): Boolean = walking()

    override fun tick() {
        FarmerAi.follow(ribbit)
    }

    private fun walking(): Boolean {
        if (ribbit.professionKind() != ProfessionKind.FARMER) return false
        if (ribbit.work().fleeTicks > 0) return false
        return FarmerAi.hasWalk(ribbit)
    }
}
