package com.reallyusefulribbits.mod.profession

import com.reallyusefulribbits.mod.config.ModConfig
import com.reallyusefulribbits.mod.inventory.GroundPickup
import com.reallyusefulribbits.mod.inventory.RibbitBags
import com.reallyusefulribbits.mod.logic.FarmerFocus
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
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.CaveVines
import net.minecraft.world.phys.Vec3
import java.util.EnumSet

object FarmerAi {
    private val drove = HashSet<Int>()
    private val busy = HashSet<Int>()
    private val hikes = HashMap<Int, Hike>()
    private val plantLocks = HashMap<Int, BlockPos>()
    private val farCursor = HashMap<Int, Int>()
    private val ignoredItems = HashMap<Int, Long>()

    fun ensureWorkGoal(ribbit: RibbitEntity) {
        if (ribbit.professionKind() != ProfessionKind.FARMER) return
        val existing = ribbit.goalSelector.availableGoals.filter { it.goal is FarmerWorkGoal }
        if (existing.any { it.priority == -1 }) return
        existing.forEach { ribbit.goalSelector.removeGoal(it.goal) }
        ribbit.goalSelector.addGoal(-1, FarmerWorkGoal(ribbit))
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
        val radius = com.reallyusefulribbits.mod.loader.RuntimeHooks.scanRadius()
        GroundPickup.tick(level, ribbit, ProfessionKind.FARMER)
        if (seekThrown(level, ribbit)) return
        if (WorldScan.scanDue(level.gameTime, data.lastScanAt, ModConfig.BIND_SCAN_INTERVAL, ribbit.id)) {
            data.lastScanAt = level.gameTime
            if (!WorldScan.bindingOk(level, data.containerPos) { ContainerSupport.isStorage(level, it) }) {
                data.containerPos = WorldScan.nearestContainer(level, ribbit.blockPosition(), radius, ribbit.id)
            }
            if (!WorldScan.bindingOk(level, data.farmOrigin) { CropSupport.isFarmBlock(level, it) }) {
                val next = WorldScan.nearestFarmOrigin(level, ribbit.blockPosition(), radius, ribbit.id)
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
        val farmInterval = if (data.farmMemory.isEmpty()) 20 else ModConfig.FARM_RESCAN_INTERVAL
        if (WorldScan.scanDue(level.gameTime, data.lastFarmScanAt, farmInterval, ribbit.id)) {
            data.lastFarmScanAt = level.gameTime
            val fresh = WorldScan.allWorkBlocks(level, origin, maxOf(radius, ModConfig.FARM_CLAIM_RADIUS), ribbit.id)
            val merged = LinkedHashSet<BlockPos>()
            merged.addAll(fresh)
            for (pos in data.farmMemory) {
                if (merged.size >= ModConfig.FARM_MEMORY_CAP) break
                if (level.isLoaded(pos) && CropSupport.keepsFarmSpot(level, pos)) merged.add(pos)
            }
            data.farmMemory.clear()
            data.farmMemory.addAll(merged)
        }
        val farm = data.farmMemory.toList()
        val chestSeeds = chestPlantables(level, ribbit)
        val jobs = scanJobs(level, ribbit, farm, chestSeeds)
        val view = FarmerWorldView(
            inventoryFull = RibbitBags.isFull(data, ProfessionKind.FARMER),
            inventoryHasItems = RibbitBags.hasItems(data, ProfessionKind.FARMER),
            hasProduce = hasProduce(data),
            hasMatureCrop = jobs.harvest != null,
            hasTillable = jobs.till != null,
            hasEmptyFarmland = jobs.plant != null,
            hasPlantable = hasPlantable(ribbit, chestSeeds),
            hasImmatureCrop = jobs.water != null && data.cooldown <= 0,
            hasBonemeal = hasBoneMeal(level, ribbit),
            hasBonemealTarget = jobs.meal != null,
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
            FarmerTask.BONEMEAL -> bonemeal(level, ribbit, jobs.meal)
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
        val meal: BlockPos?,
    )

    private fun scanJobs(
        level: ServerLevel,
        ribbit: RibbitEntity,
        farm: List<BlockPos>,
        chestSeeds: List<ItemStack>,
    ): Jobs {
        val now = level.gameTime
        var harvest: BlockPos? = null
        var till: BlockPos? = null
        var tillDist = Double.MAX_VALUE
        var plant: BlockPos? = null
        var water: BlockPos? = null
        var meal: BlockPos? = null
        var harvestDist = Double.MAX_VALUE
        var plantDist = Double.MAX_VALUE
        var waterDist = Double.MAX_VALUE
        var mealDist = Double.MAX_VALUE
        var previews = 0
        val seen = HashSet<Long>()
        val origin = ribbit.blockPosition()
        val distances = LongArray(farm.size)
        for (i in farm.indices) {
            val plot = farm[i]
            val dx = (plot.x - origin.x).toLong()
            val dz = (plot.z - origin.z).toLong()
            distances[i] = dx * dx + dz * dz
        }
        val cursor = farCursor[ribbit.id] ?: 0
        val focus = FarmerFocus.lookIndexes(farm.size, cursor, distances)
        if (farm.isNotEmpty()) farCursor[ribbit.id] = (cursor + FarmerFocus.FAR_STEP) % farm.size
        val farmSet = HashSet<BlockPos>(focus.size)
        for (index in focus) farmSet.add(farm[index])
        fun consider(pos: BlockPos) {
            if (!seen.add(pos.asLong()) || !level.isLoaded(pos)) return
            if (!available(level, pos, ribbit.id, now)) return
            val state = level.getBlockState(pos)
            val dist = ribbit.distanceToSqr(pos.x + 0.5, pos.y.toDouble(), pos.z + 0.5)
            if (dist < harvestDist && CropSupport.isMatureCrop(level, pos)) {
                val berriesTooHigh = CaveVines.hasGlowBerries(state) && !CropSupport.canReachBerries(ribbit.eyeY, pos)
                if (!berriesTooHigh) {
                    val decision = acceptsHarvest(level, ribbit, pos, previews)
                    if (decision.second) previews++
                    if (decision.first) {
                        harvest = pos
                        harvestDist = dist
                    }
                }
            }
            if (dist < plantDist && canPlantSoil(level, ribbit, pos, chestSeeds)) {
                plant = pos
                plantDist = dist
            }
            if (dist < waterDist && CropSupport.needsWater(level, pos)) {
                water = pos
                waterDist = dist
            }
            if (dist < mealDist && CropSupport.canBonemeal(level, pos)) {
                meal = pos
                mealDist = dist
            }
        }
        for (index in focus) {
            val plot = farm[index]
            val vine = level.isLoaded(plot) && CropSupport.isCaveVine(level.getBlockState(plot))
            val height = if (vine) 8 else 3
            consider(plot)
            for (dy in 1..height) consider(plot.above(dy))
            for (dir in Direction.Plane.HORIZONTAL) {
                val side = plot.relative(dir)
                consider(side)
                consider(side.above())
                if (isInteriorHole(level, side, farmSet)) {
                    if (!level.isLoaded(side) || !CropSupport.isTillable(level.getBlockState(side))) continue
                    val dist = ribbit.distanceToSqr(side.x + 0.5, side.y.toDouble(), side.z + 0.5)
                    if (dist < tillDist) {
                        till = side
                        tillDist = dist
                    }
                }
            }
        }
        for (index in focus) {
            val plot = farm[index]
            if (!level.isLoaded(plot) || !CropSupport.isTillable(level.getBlockState(plot))) continue
            val dist = ribbit.distanceToSqr(plot.x + 0.5, plot.y.toDouble(), plot.z + 0.5)
            if (dist < tillDist) {
                till = plot
                tillDist = dist
            }
        }
        val plantTarget = stick(ribbit, plant) { canPlantSoil(level, ribbit, it, chestSeeds) }
        BlockReservation.cleanup(now)
        return Jobs(harvest, till, plantTarget, water, meal)
    }

    /** Пустой слот принимает любой урожай. Иначе смотрим дроп, но не больше четырёх лут-таблиц за тик. */
    private fun acceptsHarvest(
        level: ServerLevel,
        ribbit: RibbitEntity,
        pos: BlockPos,
        previews: Int,
    ): Pair<Boolean, Boolean> {
        val data = ribbit.work()
        val decision = FarmerFocus.harvestNow(
            RibbitBags.isFull(data, ProfessionKind.FARMER),
            hasEmptySlot(data),
        )
        if (decision != null) return decision to false
        if (previews >= 4) return false to false
        val preview = CropSupport.previewDrops(level, pos, ribbit)
        val fits = preview.isEmpty() || preview.any { stack ->
            stack.isEmpty || RibbitBags.canInsertAll(data, ProfessionKind.FARMER, listOf(stack))
        }
        return fits to true
    }

    private fun hasEmptySlot(data: com.reallyusefulribbits.mod.attach.RibbitWorkData): Boolean {
        val used = data.usedSlots(ProfessionKind.FARMER)
        for (i in 0 until used) {
            if (data.items[i].isEmpty) return true
        }
        return false
    }

    private fun available(level: ServerLevel, pos: BlockPos, entityId: Int, now: Long): Boolean {
        val holder = BlockReservation.holder(level, pos, now)
        return holder == null || holder == entityId
    }

    /** Семена из сундука читаются один раз за тик, а не на каждую пустую грядку. */
    private fun chestPlantables(level: ServerLevel, ribbit: RibbitEntity): List<ItemStack> {
        val container = ribbit.work().containerPos ?: return emptyList()
        val handler = ContainerSupport.handler(level, container) ?: return emptyList()
        val out = ArrayList<ItemStack>()
        for (slot in 0 until handler.slots) {
            val stack = handler.getStackInSlot(slot)
            if (!stack.isEmpty && CropSupport.plantableBlock(stack) != null) out += stack.copy()
        }
        return out
    }

    /** Держит одну дальнюю грядку, но если рядом уже есть пустая — сажает её сразу. */
    private fun stick(ribbit: RibbitEntity, nearest: BlockPos?, still: (BlockPos) -> Boolean): BlockPos? {
        val id = ribbit.id
        val locked = plantLocks[id]
        if (locked != null && still(locked)) {
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
        if (!level.isLoaded(pos)) return false
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
        if (!BlockReservation.tryClaim(level, pos, ribbit.id, level.gameTime)) return
        if (!walkTo(level, ribbit, pos)) return
        action(pos)
        BlockReservation.release(level, pos, ribbit.id)
        ribbit.work().taskTicks = FarmerTaskPlanner.MIN_TASK_TICKS + FarmerTaskPlanner.SWITCH_COOLDOWN_TICKS
    }

    /** Предмет, брошенный рядом, важнее текущей грядки, кроме уже начатого полива. */
    private fun seekThrown(level: ServerLevel, ribbit: RibbitEntity): Boolean {
        val data = ribbit.work()
        if (RibbitBags.isFull(data, ProfessionKind.FARMER)) return false
        if (data.task == FarmerTask.WATER.name && data.taskTicks in 1 until ModConfig.WATERING_ANIM_TICKS) return false
        val now = level.gameTime
        if (ignoredItems.size > 64) ignoredItems.entries.removeIf { it.value <= now }
        val box = ribbit.boundingBox.inflate(8.0)
        val item = level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity::class.java, box) { entity ->
            entity.isAlive && entity.tickCount >= 8 &&
                (ignoredItems[entity.id] ?: 0L) <= now &&
                GroundPickup.isRelevant(ProfessionKind.FARMER, entity.item)
        }.minByOrNull { ribbit.distanceToSqr(it) } ?: return false
        if (ribbit.distanceToSqr(item) <= ModConfig.GROUND_PICKUP_RANGE * ModConfig.GROUND_PICKUP_RANGE) return false
        walkTo(level, ribbit, item.blockPosition(), 1.8, 1.35, false, item.id.toLong())
        LookAt.block(ribbit, item.blockPosition(), 0.2)
        val still = hikes[ribbit.id]?.takeIf { it.work == item.id.toLong() }?.still ?: 0
        if (still > 50) {
            ignoredItems[item.id] = now + 200
            hikes.remove(ribbit.id)
            return false
        }
        return true
    }

    private fun harvest(level: ServerLevel, ribbit: RibbitEntity, pos: BlockPos) {
        val preview = CropSupport.previewDrops(level, pos, ribbit)
        val anyFits = preview.isEmpty() || preview.any { stack ->
            stack.isEmpty || RibbitBags.canInsertAll(ribbit.work(), ProfessionKind.FARMER, listOf(stack))
        }
        if (!anyFits) return
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
        val pocket = RibbitBags.find(data, ProfessionKind.FARMER) { CropSupport.canPlant(level, soil, it) }
        if (pocket.isEmpty) {
            val chest = data.containerPos ?: return
            if (!walkTo(level, ribbit, chest, Math.sqrt(ModConfig.CONTAINER_REACH_SQ), 1.35, false)) return
            LookAt.block(ribbit, chest, 0.5)
            ContainerSupport.openBriefly(level, chest)
            var grabbed = 0
            while (grabbed < 4 && !RibbitBags.isFull(data, ProfessionKind.FARMER)) {
                val taken = ContainerSupport.extractMatching(level, chest, { CropSupport.canPlant(level, soil, it) }, 64)
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
        if (!BlockReservation.tryClaim(level, soil, ribbit.id, level.gameTime)) return
        if (!walkTo(level, ribbit, soil)) return
        val seed = RibbitBags.takeOne(data, ProfessionKind.FARMER) { CropSupport.canPlant(level, soil, it) }
        if (seed.isEmpty) return
        if (!CropSupport.plant(level, soil, seed) && !seed.isEmpty) {
            RibbitBags.insert(data, ProfessionKind.FARMER, seed)
        }
        BlockReservation.release(level, soil, ribbit.id)
        data.taskTicks = FarmerTaskPlanner.MIN_TASK_TICKS + FarmerTaskPlanner.SWITCH_COOLDOWN_TICKS
    }

    private fun bonemeal(level: ServerLevel, ribbit: RibbitEntity, crop: BlockPos?) {
        if (crop == null) return
        val data = ribbit.work()
        val pocket = RibbitBags.find(data, ProfessionKind.FARMER) { it.`is`(Items.BONE_MEAL) }
        if (pocket.isEmpty) {
            val chest = data.containerPos ?: return
            if (!walkTo(level, ribbit, chest, Math.sqrt(ModConfig.CONTAINER_REACH_SQ), 1.35, false)) return
            LookAt.block(ribbit, chest, 0.5)
            ContainerSupport.openBriefly(level, chest)
            val taken = ContainerSupport.extractMatching(level, chest, { it.`is`(Items.BONE_MEAL) }, 64)
            if (!taken.isEmpty) {
                val leftover = RibbitBags.insert(data, ProfessionKind.FARMER, taken)
                if (!leftover.isEmpty) ContainerSupport.insertAll(level, chest, listOf(leftover))
            }
            return
        }
        if (!BlockReservation.tryClaim(level, crop, ribbit.id, level.gameTime)) return
        if (!walkTo(level, ribbit, crop)) return
        LookAt.block(ribbit, crop, 0.5)
        val meal = RibbitBags.takeOne(data, ProfessionKind.FARMER) { it.`is`(Items.BONE_MEAL) }
        if (meal.isEmpty) return
        if (!CropSupport.applyBonemeal(level, crop, meal) && !meal.isEmpty) {
            RibbitBags.insert(data, ProfessionKind.FARMER, meal)
        }
        BlockReservation.release(level, crop, ribbit.id)
        data.taskTicks = FarmerTaskPlanner.MIN_TASK_TICKS + FarmerTaskPlanner.SWITCH_COOLDOWN_TICKS
    }

    private fun water(level: ServerLevel, ribbit: RibbitEntity, pos: BlockPos?) {
        if (pos == null) return
        if (!BlockReservation.tryClaim(level, pos, ribbit.id, level.gameTime)) {
            ribbit.setWatering(false)
            return
        }
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
            BlockReservation.release(level, pos, ribbit.id)
            ribbit.setWatering(false)
            ribbit.work().cooldown = ModConfig.WATER_COOLDOWN_TICKS
            ribbit.work().taskTicks = FarmerTaskPlanner.MIN_TASK_TICKS + FarmerTaskPlanner.SWITCH_COOLDOWN_TICKS
        }
    }

    private fun deposit(level: ServerLevel, ribbit: RibbitEntity) {
        val data = ribbit.work()
        val pos = data.containerPos ?: return
        if (!level.isLoaded(pos)) return
        if (!ContainerSupport.isStorage(level, pos)) {
            data.containerPos = null
            return
        }
        if (!walkTo(level, ribbit, pos, Math.sqrt(ModConfig.CONTAINER_REACH_SQ), 1.35, false)) return
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
            if (!isKeptSupply(stack)) return true
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
            if (!isKeptSupply(stack)) {
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

    private fun isKeptSupply(stack: ItemStack): Boolean =
        stack.`is`(Items.BONE_MEAL) || CropSupport.plantableBlock(stack) != null

    private fun hasBoneMeal(level: ServerLevel, ribbit: RibbitEntity): Boolean {
        val data = ribbit.work()
        if (!RibbitBags.find(data, ProfessionKind.FARMER) { it.`is`(Items.BONE_MEAL) }.isEmpty) return true
        val container = data.containerPos ?: return false
        val handler = ContainerSupport.handler(level, container) ?: return false
        for (slot in 0 until handler.slots) {
            if (handler.getStackInSlot(slot).`is`(Items.BONE_MEAL)) return true
        }
        return false
    }

    private fun canPlantSoil(
        level: ServerLevel,
        ribbit: RibbitEntity,
        pos: BlockPos,
        chestSeeds: List<ItemStack>,
    ): Boolean {
        if (!CropSupport.isEmptyFarmland(level, pos)) return false
        val data = ribbit.work()
        if (!RibbitBags.find(data, ProfessionKind.FARMER) { CropSupport.canPlant(level, pos, it) }.isEmpty) return true
        return chestSeeds.any { CropSupport.canPlant(level, pos, it) }
    }

    private fun hasPlantable(ribbit: RibbitEntity, chestSeeds: List<ItemStack>): Boolean {
        if (chestSeeds.isNotEmpty()) return true
        val data = ribbit.work()
        return !RibbitBags.find(data, ProfessionKind.FARMER) { CropSupport.plantableBlock(it) != null }.isEmpty
    }

    private fun walkTo(
        level: ServerLevel,
        ribbit: RibbitEntity,
        pos: BlockPos,
        reach: Double = Math.sqrt(ModConfig.WORK_REACH_SQ),
        speed: Double = 1.35,
        allowStuckArrive: Boolean = false,
        track: Long = pos.asLong(),
    ): Boolean {
        val target = Vec3(pos.x + 0.5, pos.y.toDouble(), pos.z + 0.5)
        if (ribbit.distanceToSqr(target) <= reach * reach) {
            ribbit.navigation.stop()
            ribbit.work().navStuck = 0
            hikes.remove(ribbit.id)
            return true
        }
        val spot = standSpot(level, ribbit, pos, reach)
        val nav = ribbit.navigation
        val previous = hikes[ribbit.id]
        val hike = if (previous != null && previous.work == track) {
            previous
        } else {
            Hike(track, ribbit.x, ribbit.z, 0, false)
        }
        val moved = ribbit.distanceToSqr(hike.lastX, ribbit.y, hike.lastZ) > 0.01
        if (moved) hike.still = 0 else if (previous != null && previous.work == track) hike.still++
        hike.lastX = ribbit.x
        hike.lastZ = ribbit.z
        if (!hike.direct && hike.still >= 8) hike.direct = true
        hikes[ribbit.id] = hike
        ribbit.work().navStuck = hike.still
        if (hike.direct) {
            nav.stop()
            val aim = sidestep(ribbit, spot, hike.still)
            ribbit.moveControl.setWantedPosition(aim.x, aim.y, aim.z, speed)
        } else if (!nav.isInProgress) {
            if (!nav.moveTo(spot.x, spot.y, spot.z, speed)) {
                hike.direct = true
                ribbit.moveControl.setWantedPosition(spot.x, spot.y, spot.z, speed)
            }
        }
        if (allowStuckArrive && hike.still > 80 && ribbit.distanceToSqr(target) < 4.0) {
            ribbit.work().navStuck = 0
            return true
        }
        return false
    }

    /** Если прямой шаг упёрся в бочку или воду, сместиться вбок и обойти. */
    private fun sidestep(ribbit: RibbitEntity, spot: Vec3, still: Int): Vec3 {
        if (still < 15) return spot
        val dx = spot.x - ribbit.x
        val dz = spot.z - ribbit.z
        val len = kotlin.math.sqrt(dx * dx + dz * dz).coerceAtLeast(0.01)
        val side = if ((still / 15) % 2 == 0) 1.25 else -1.25
        return Vec3(
            ribbit.x + dx / len * 2.0 + (-dz / len) * side,
            spot.y,
            ribbit.z + dz / len * 2.0 + (dx / len) * side,
        )
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
        if (!level.isLoaded(feet) || !level.isLoaded(feet.below())) return false
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
        var direct: Boolean,
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

    private fun working(): Boolean {
        if (ribbit.professionKind() != ProfessionKind.FARMER) return false
        if (ribbit.work().fleeTicks > 0) return false
        return ribbit.work().farmOrigin != null
    }
}
