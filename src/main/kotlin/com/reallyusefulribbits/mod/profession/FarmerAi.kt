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
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.CaveVines
import net.minecraft.world.phys.Vec3
import java.util.EnumSet

object FarmerAi {
    private val drove = HashSet<Int>()
    private val busy = HashSet<Int>()
    private val hikes = HashMap<Int, Hike>()
    private val plantLocks = HashMap<Int, BlockPos>()
    private val bans = HashMap<Int, HashMap<Long, Long>>()

    fun ensureWorkGoal(ribbit: RibbitEntity) {
        if (ribbit.professionKind() != ProfessionKind.FARMER) return
        val present = ribbit.goalSelector.availableGoals.any { it.goal is FarmerWorkGoal }
        if (present) return
        ribbit.goalSelector.addGoal(0, FarmerWorkGoal(ribbit))
    }

    fun isBusy(ribbit: RibbitEntity): Boolean = busy.contains(ribbit.id)

    /** Вызывается из цели до navigation.tick, чтобы прогулка риббита не стирала путь. */
    fun drive(ribbit: RibbitEntity) {
        val level = ribbit.level() as? ServerLevel ?: return
        drove.add(ribbit.id)
        perform(level, ribbit)
    }

    fun tick(level: ServerLevel, ribbit: RibbitEntity) {
        if (drove.remove(ribbit.id)) return
        perform(level, ribbit)
    }

    private fun perform(level: ServerLevel, ribbit: RibbitEntity) {
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
        val origin = data.farmOrigin
        if (origin == null) {
            busy.remove(ribbit.id)
            return
        }
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
            hasProduce = hasProduce(data),
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
            FarmerTask.IDLE -> {
                ribbit.setWatering(false)
                hikes.remove(ribbit.id)
            }
        }
        if (task == FarmerTask.IDLE) busy.remove(ribbit.id) else busy.add(ribbit.id)
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
            if (dist < plantDist && CropSupport.isEmptyFarmland(level, pos) && !isBanned(ribbit.id, pos, now)) {
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
        val plantTarget = stick(level, ribbit, plant) { CropSupport.isEmptyFarmland(level, it) }
        listOfNotNull(harvest, till, plantTarget, water).forEach {
            BlockReservation.tryClaim(level, it, ribbit.id, now)
        }
        BlockReservation.cleanup(now)
        return Jobs(harvest, till, plantTarget, water)
    }

    /** Держит одну дальнюю грядку, но если рядом уже есть пустая — сажает её сразу. */
    private fun stick(level: ServerLevel, ribbit: RibbitEntity, nearest: BlockPos?, still: (BlockPos) -> Boolean): BlockPos? {
        val id = ribbit.id
        val now = level.gameTime
        val locked = plantLocks[id]
        if (locked != null && still(locked) && !isBanned(id, locked, now)) {
            if (nearest != null && ribbit.distanceToSqr(nearest.x + 0.5, nearest.y.toDouble(), nearest.z + 0.5) <= ModConfig.WORK_REACH_SQ) {
                plantLocks[id] = nearest
                return nearest
            }
            return locked
        }
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
            if (!walkTo(level, ribbit, chest, Math.sqrt(ModConfig.CONTAINER_REACH_SQ), 1.2, false)) return
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
        if (data.containerPos == null || !ContainerSupport.isStorage(level, data.containerPos!!)) {
            data.containerPos = WorldScan.nearestContainer(level, ribbit.blockPosition(), ServerConfig.scanRadius())
        }
        val pos = data.containerPos ?: return
        if (!walkTo(level, ribbit, pos, Math.sqrt(ModConfig.CONTAINER_REACH_SQ), 1.2, false)) return
        LookAt.block(ribbit, pos, 0.5)
        ContainerSupport.openBriefly(level, pos)
        val cargo = takeProduce(data)
        val dumping = if (cargo.isNotEmpty()) cargo else RibbitBags.extractAll(data, ProfessionKind.FARMER)
        val leftover = ContainerSupport.insertAll(level, pos, dumping)
        leftover.forEach { RibbitBags.insert(data, ProfessionKind.FARMER, it) }
        data.taskTicks = FarmerTaskPlanner.MIN_TASK_TICKS + FarmerTaskPlanner.SWITCH_COOLDOWN_TICKS
    }

    /** Пшеница и прочий урожай, не семена. Семена одного вида оставляет до стака. */
    private fun hasProduce(data: com.reallyusefulribbits.mod.attach.RibbitWorkData): Boolean {
        val kept = HashMap<Item, Int>()
        val used = data.usedSlots(ProfessionKind.FARMER)
        for (i in 0 until used) {
            val stack = data.items[i]
            if (stack.isEmpty) continue
            if (CropSupport.plantableBlock(stack) == null) return true
            val already = kept.getOrDefault(stack.item, 0)
            if (already + stack.count > 64) return true
            kept[stack.item] = already + stack.count
        }
        return false
    }

    private fun takeProduce(data: com.reallyusefulribbits.mod.attach.RibbitWorkData): List<ItemStack> {
        val kept = HashMap<Item, Int>()
        val out = ArrayList<ItemStack>()
        val used = data.usedSlots(ProfessionKind.FARMER)
        for (i in 0 until used) {
            val stack = data.items[i]
            if (stack.isEmpty) continue
            if (CropSupport.plantableBlock(stack) == null) {
                out += stack.copy()
                data.items[i] = ItemStack.EMPTY
                continue
            }
            val already = kept.getOrDefault(stack.item, 0)
            val room = (64 - already).coerceAtLeast(0)
            if (stack.count <= room) {
                kept[stack.item] = already + stack.count
                continue
            }
            val extra = stack.copy()
            extra.count = stack.count - room
            out += extra
            if (room <= 0) data.items[i] = ItemStack.EMPTY else stack.count = room
            kept[stack.item] = already + room
        }
        return out
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
        speed: Double = 1.2,
        allowStuckArrive: Boolean = false,
    ): Boolean {
        val target = Vec3(pos.x + 0.5, pos.y.toDouble(), pos.z + 0.5)
        if (ribbit.distanceToSqr(target) <= reach * reach) {
            ribbit.navigation.stop()
            ribbit.work().navStuck = 0
            hikes.remove(ribbit.id)
            return true
        }
        val nav = ribbit.navigation
        val hike = hikes[ribbit.id]
        if (hike != null && hike.work == pos.asLong()) {
            val moved = ribbit.distanceToSqr(hike.lastX, ribbit.y, hike.lastZ) > 0.002
            hike.lastX = ribbit.x
            hike.lastZ = ribbit.z
            if (moved) hike.still = 0 else hike.still++
            if (hike.still >= 30) {
                ban(ribbit.id, pos, level.gameTime + 100)
                if (plantLocks[ribbit.id] == pos) plantLocks.remove(ribbit.id)
                hikes.remove(ribbit.id)
                nav.stop()
                return false
            }
            if (nav.isInProgress) return false
        }
        val spot = standSpot(level, ribbit, pos, reach)
        nav.moveTo(spot.x, spot.y, spot.z, speed)
        val still = if (hike != null && hike.work == pos.asLong()) hike.still else 0
        hikes[ribbit.id] = Hike(pos.asLong(), ribbit.x, ribbit.z, still)
        ribbit.work().navStuck = still
        if (allowStuckArrive && still > 80 && ribbit.distanceToSqr(target) < 4.0) {
            ribbit.work().navStuck = 0
            return true
        }
        return false
    }

    private fun ban(id: Int, pos: BlockPos, until: Long) {
        bans.getOrPut(id) { HashMap() }[pos.asLong()] = until
    }

    private fun isBanned(id: Int, pos: BlockPos, now: Long): Boolean {
        val table = bans[id] ?: return false
        val until = table[pos.asLong()] ?: return false
        if (now >= until) {
            table.remove(pos.asLong())
            return false
        }
        return true
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

    private data class Hike(
        val work: Long,
        var lastX: Double,
        var lastZ: Double,
        var still: Int,
    )
}

private class FarmerWorkGoal(private val ribbit: RibbitEntity) : Goal() {
    init {
        setFlags(EnumSet.of(Flag.MOVE))
    }

    override fun canUse(): Boolean = working()

    override fun canContinueToUse(): Boolean = working()

    override fun tick() {
        FarmerAi.drive(ribbit)
    }

    override fun stop() {
        ribbit.navigation.stop()
    }

    private fun working(): Boolean {
        if (ribbit.professionKind() != ProfessionKind.FARMER) return false
        if (ribbit.work().fleeTicks > 0) return false
        return FarmerAi.isBusy(ribbit)
    }
}
