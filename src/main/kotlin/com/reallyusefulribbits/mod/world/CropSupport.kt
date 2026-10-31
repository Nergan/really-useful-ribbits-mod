package com.reallyusefulribbits.mod.world

import com.reallyusefulribbits.mod.config.ModConfig
import com.reallyusefulribbits.mod.logic.CropRules
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.tags.BlockTags
import net.minecraft.tags.FluidTags
import net.minecraft.tags.ItemTags
import net.minecraft.world.entity.Entity
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.AttachedStemBlock
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.BonemealableBlock
import net.minecraft.world.level.block.BushBlock
import net.minecraft.world.level.block.CaveVines
import net.minecraft.world.level.block.CropBlock
import net.minecraft.world.level.block.FarmBlock
import net.minecraft.world.level.block.LiquidBlock
import net.minecraft.world.level.block.NetherWartBlock
import net.minecraft.world.level.block.StemBlock
import net.minecraft.world.level.block.SugarCaneBlock
import net.minecraft.world.level.block.SweetBerryBushBlock
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.level.block.state.properties.IntegerProperty

object CropSupport {
    fun isFarmBlock(level: Level, pos: BlockPos): Boolean {
        if (!level.isLoaded(pos)) return false
        val state = level.getBlockState(pos)
        val block = state.block
        if (isClassicFarmSoil(state)) return true
        if (state.block is SugarCaneBlock && level.getBlockState(pos.below()).block !is SugarCaneBlock) return true
        if (state.`is`(Blocks.SOUL_SAND)) return true
        if (block is CaveVines || state.`is`(Blocks.CAVE_VINES) || state.`is`(Blocks.CAVE_VINES_PLANT)) return true
        if (isSpecialAnchor(level, pos)) return true
        val above = pos.above()
        return level.isLoaded(above) && isSpecialAnchor(level, above)
    }

    /** Грядка, которую уже запомнили: после сбора рис на грязи и куст на земле остаются фермой. */
    fun keepsFarmSpot(level: Level, pos: BlockPos): Boolean {
        if (isFarmBlock(level, pos)) return true
        if (!level.isLoaded(pos) || !level.isLoaded(pos.above())) return false
        val state = level.getBlockState(pos)
        if (!isLooseSoil(state) && !isClassicFarmSoil(state)) return false
        return canReplaceWithCrop(level.getBlockState(pos.above()))
    }

    fun isTillable(state: BlockState): Boolean {
        if (state.block is FarmBlock || state.`is`(ModTags.FARMLAND)) return false
        return state.`is`(ModTags.TILLABLE)
    }

    fun till(level: ServerLevel, pos: BlockPos): Boolean {
        val state = level.getBlockState(pos)
        if (!isTillable(state)) return false
        val farmland = if (BuiltInRegistries.BLOCK.containsKey(
                net.minecraft.resources.ResourceLocation.parse("farmersdelight:rich_soil_farmland"),
            ) && BuiltInRegistries.BLOCK.getKey(state.block).path.contains("rich_soil")
        ) {
            BuiltInRegistries.BLOCK.get(net.minecraft.resources.ResourceLocation.parse("farmersdelight:rich_soil_farmland"))
                .defaultBlockState()
        } else {
            Blocks.FARMLAND.defaultBlockState()
        }
        level.setBlock(pos, farmland, Block.UPDATE_ALL)
        level.playSound(null, pos, SoundEvents.HOE_TILL, SoundSource.BLOCKS, 1f, 1f)
        return true
    }

    fun plantableBlock(stack: ItemStack): Block? {
        val item = stack.item
        if (item is BlockItem) {
            val block = item.block
            val state = block.defaultBlockState()
            if (isPricklyBush(state)) return null
            if (block is CropBlock || block is StemBlock || block is NetherWartBlock || block is SugarCaneBlock) return block
            if (state.`is`(BlockTags.CROPS)) return block
            if (stack.`is`(ItemTags.VILLAGER_PLANTABLE_SEEDS)) return block
            if (block is BushBlock && ageProperty(state) != null) return block
            if (isModAgeCrop(state)) return block
        }
        return null
    }

    fun canPlant(level: Level, soil: BlockPos, stack: ItemStack): Boolean {
        val block = plantableBlock(stack) ?: return false
        return canPlantOn(level, soil, block)
    }

    /** Песок душ принимает только незерский нарост, даже если он в теге фермы. */
    fun canPlantOn(level: Level, soil: BlockPos, plant: Block): Boolean {
        if (isPricklyBush(plant.defaultBlockState())) return false
        if (!level.isLoaded(soil) || !level.isLoaded(soil.above())) return false
        val plantPos = soil.above()
        val above = level.getBlockState(plantPos)
        if (!canReplaceWithCrop(above)) return false
        val soilState = level.getBlockState(soil)
        val soulSand = soilState.`is`(Blocks.SOUL_SAND)
        return when (plant) {
            is NetherWartBlock -> soulSand && above.isAir
            is SugarCaneBlock -> above.isAir && !soulSand && (
                soilState.`is`(Blocks.SAND) || soilState.`is`(Blocks.RED_SAND) ||
                    soilState.`is`(BlockTags.DIRT) || soilState.block is FarmBlock
                )
            else -> {
                if (!soulSand && isClassicFarmSoil(soilState) && above.isAir) return true
                val placed = placedState(level, plantPos, plant)
                if (!above.isAir && !placed.hasProperty(BlockStateProperties.WATERLOGGED)) return false
                placed.canSurvive(level, plantPos)
            }
        }
    }

    fun plant(level: ServerLevel, soil: BlockPos, stack: ItemStack): Boolean {
        val block = plantableBlock(stack) ?: return false
        if (!canPlantOn(level, soil, block)) return false
        level.setBlock(soil.above(), placedState(level, soil.above(), block), Block.UPDATE_ALL)
        stack.shrink(1)
        level.playSound(null, soil, SoundEvents.CROP_PLANTED, SoundSource.BLOCKS, 1f, 1f)
        return true
    }

    fun canBonemeal(level: Level, pos: BlockPos): Boolean {
        val state = level.getBlockState(pos)
        if (isPricklyBush(state)) return false
        val block = state.block as? BonemealableBlock ?: return false
        if (!block.isValidBonemealTarget(level, pos, state)) return false
        if (block is CropBlock || block is StemBlock) return true
        if (state.`is`(BlockTags.CROPS)) return true
        if (isCaveVine(state) && !CaveVines.hasGlowBerries(state)) return true
        if (block is BushBlock) return true
        return isModAgeCrop(state)
    }

    fun applyBonemeal(level: ServerLevel, pos: BlockPos, stack: ItemStack): Boolean {
        if (!stack.`is`(Items.BONE_MEAL) || !canBonemeal(level, pos)) return false
        val state = level.getBlockState(pos)
        val block = state.block as BonemealableBlock
        if (block.isBonemealSuccess(level, level.random, pos, state)) {
            block.performBonemeal(level, level.random, pos, state)
        }
        stack.shrink(1)
        growthStars(level, pos)
        level.playSound(null, pos, SoundEvents.BONE_MEAL_USE, SoundSource.BLOCKS, 1f, 1f)
        return true
    }

    /** Зелёные звёздочки, как у костной муки в ванили. */
    private fun growthStars(level: ServerLevel, pos: BlockPos) {
        val state = level.getBlockState(pos)
        if (state.isAir) return
        val solid = state.isSolidRender(level, pos)
        val count = if (solid) 24 else 14
        val ySpan = if (solid) 0.6 else state.getShape(level, pos).max(Direction.Axis.Y).coerceAtLeast(0.25)
        level.sendParticles(
            net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER,
            pos.x + 0.5,
            pos.y + ySpan * 0.65,
            pos.z + 0.5,
            count,
            0.35,
            ySpan * 0.35,
            0.35,
            0.0,
        )
    }

    fun isMatureCrop(level: Level, pos: BlockPos): Boolean {
        val state = level.getBlockState(pos)
        if (isPricklyBush(state)) return false
        val block = state.block
        if (block is StemBlock || block is AttachedStemBlock) return false
        if (block is CropBlock) return block.isMaxAge(state)
        if (block is NetherWartBlock) return state.getValue(NetherWartBlock.AGE) >= 3
        if (isStemFruit(state) && CropRules.shouldHarvestStemFruit(hasStemNeighbor(level, pos))) return true
        if (block is SugarCaneBlock && level.getBlockState(pos.below()).block is SugarCaneBlock) return true
        if (CaveVines.hasGlowBerries(state)) return true
        val age = ageProperty(state)
        if (age != null && isAgeCrop(state)) {
            val max = age.possibleValues.maxOrNull() ?: return false
            if (state.getValue(age) < max) return false
            val grower = block as? BonemealableBlock ?: return true
            return !grower.isValidBonemealTarget(level, pos, state)
        }
        return false
    }

    fun isImmatureCrop(level: Level, pos: BlockPos): Boolean {
        val state = level.getBlockState(pos)
        if (isPricklyBush(state)) return false
        val block = state.block
        if (block is NetherWartBlock) return false
        if (block is StemBlock || block is AttachedStemBlock) return false
        if (isCaveVine(state) && !CaveVines.hasGlowBerries(state)) return true
        if (block is CropBlock) return !block.isMaxAge(state)
        if (state.`is`(BlockTags.CROPS) && block is BonemealableBlock) {
            return block.isValidBonemealTarget(level, pos, state)
        }
        val age = ageProperty(state)
        if (age != null && isAgeCrop(state)) {
            val max = age.possibleValues.maxOrNull() ?: return false
            if (state.getValue(age) < max) return true
            val grower = block as? BonemealableBlock ?: return false
            return grower.isValidBonemealTarget(level, pos, state)
        }
        return false
    }

    fun isEmptyFarmland(level: Level, pos: BlockPos): Boolean {
        if (!level.isLoaded(pos) || !level.isLoaded(pos.above())) return false
        val state = level.getBlockState(pos)
        if (isAnchorCrop(level, pos)) return false
        if (state.block is SugarCaneBlock || isCaveVine(state)) return false
        val above = level.getBlockState(pos.above())
        if (!canReplaceWithCrop(above)) return false
        if (state.`is`(Blocks.SOUL_SAND)) return above.isAir
        if (isClassicFarmSoil(state)) return above.isAir
        if (!isLooseSoil(state)) return false
        return hasSpecialCropNearby(level, pos)
    }

    fun needsWater(level: Level, pos: BlockPos): Boolean {
        if (isImmatureCrop(level, pos)) return true
        val state = level.getBlockState(pos)
        if (state.block is FarmBlock && state.hasProperty(FarmBlock.MOISTURE)) {
            val cropAbove = level.getBlockState(pos.above())
            if (!cropAbove.isAir && state.getValue(FarmBlock.MOISTURE) < 7) return true
        }
        return false
    }

    fun previewDrops(level: ServerLevel, pos: BlockPos, harvester: Entity): List<ItemStack> {
        val state = level.getBlockState(pos)
        val block = state.block
        return when {
            CaveVines.hasGlowBerries(state) -> listOf(ItemStack(net.minecraft.world.item.Items.GLOW_BERRIES))
            block is SugarCaneBlock -> previewCane(level, pos, harvester)
            isStemFruit(state) -> {
                Block.getDrops(state, level, pos, level.getBlockEntity(pos), harvester, ItemStack.EMPTY)
            }
            else -> Block.getDrops(state, level, pos, level.getBlockEntity(pos), harvester, ItemStack.EMPTY)
        }
    }

    fun harvest(level: ServerLevel, pos: BlockPos, harvester: Entity): List<ItemStack> {
        val state = level.getBlockState(pos)
        val block = state.block
        val drops: List<ItemStack>
        val sound: net.minecraft.sounds.SoundEvent
        when {
            CaveVines.hasGlowBerries(state) -> {
                sound = SoundEvents.CAVE_VINES_PICK_BERRIES
                val berriesProp = net.minecraft.world.level.block.state.properties.BlockStateProperties.BERRIES
                if (state.hasProperty(berriesProp)) {
                    level.setBlock(pos, state.setValue(berriesProp, false), Block.UPDATE_ALL)
                }
                drops = listOf(ItemStack(net.minecraft.world.item.Items.GLOW_BERRIES))
            }
            block is SugarCaneBlock -> {
                sound = SoundEvents.GRASS_BREAK
                drops = harvestCane(level, pos, harvester)
            }
            isStemFruit(state) -> {
                sound = SoundEvents.WOOD_BREAK
                drops = Block.getDrops(state, level, pos, level.getBlockEntity(pos), harvester, ItemStack.EMPTY)
                level.removeBlock(pos, false)
            }
            block is NetherWartBlock -> {
                sound = SoundEvents.NETHER_WART_BREAK
                drops = Block.getDrops(state, level, pos, level.getBlockEntity(pos), harvester, ItemStack.EMPTY)
                level.removeBlock(pos, false)
            }
            else -> {
                sound = SoundEvents.CROP_BREAK
                drops = Block.getDrops(state, level, pos, level.getBlockEntity(pos), harvester, ItemStack.EMPTY)
                level.removeBlock(pos, false)
            }
        }
        level.playSound(null, pos, sound, SoundSource.BLOCKS, 1f, 1f)
        return drops
    }

    fun water(level: ServerLevel, pos: BlockPos) {
        hydrateFarmland(level, pos)
        maybeGrowGlowBerries(level, pos)
        maybeGrowGlowBerries(level, pos.above())
        maybeGrowGlowBerries(level, pos.below())
        level.sendParticles(
            net.minecraft.core.particles.ParticleTypes.RAIN,
            pos.x + 0.5,
            pos.y + 0.7,
            pos.z + 0.5,
            6,
            0.2,
            0.1,
            0.2,
            0.0,
        )
        level.playSound(null, pos, SoundEvents.BUBBLE_COLUMN_BUBBLE_POP, SoundSource.BLOCKS, 0.6f, 1.2f)
    }

    fun canReachBerries(entityY: Double, pos: BlockPos): Boolean =
        CropRules.canReachBerries(entityY, pos.y.toDouble(), ModConfig.BERRY_REACH)

    fun isCaveVine(state: BlockState): Boolean {
        val block = state.block
        return block is CaveVines || state.`is`(Blocks.CAVE_VINES) || state.`is`(Blocks.CAVE_VINES_PLANT)
    }

    fun isStemFruit(state: BlockState): Boolean {
        val block = state.block
        if (block is StemBlock || block is AttachedStemBlock) return false
        if (state.`is`(Blocks.MELON) || state.`is`(Blocks.PUMPKIN)) return true
        val path = BuiltInRegistries.BLOCK.getKey(block).path
        if (path.contains("stem") || path.contains("carved") || path.contains("lantern") || path.contains("pie")) return false
        return path.endsWith("melon") || path.endsWith("pumpkin")
    }

    private fun maybeGrowGlowBerries(level: ServerLevel, pos: BlockPos) {
        val state = level.getBlockState(pos)
        if (!isCaveVine(state) || CaveVines.hasGlowBerries(state)) return
        if (level.random.nextInt(3) != 0) return
        val berries = net.minecraft.world.level.block.state.properties.BlockStateProperties.BERRIES
        if (state.hasProperty(berries)) {
            level.setBlock(pos, state.setValue(berries, true), Block.UPDATE_ALL)
        }
    }

    /** Плод вырос: присоединённый стебель смотрит на него и это стебель того же плода. */
    fun hasStemNeighbor(level: Level, pos: BlockPos): Boolean {
        val fruit = level.getBlockState(pos)
        if (!isStemFruit(fruit)) return false
        for (dir in Direction.Plane.HORIZONTAL) {
            val neighbor = level.getBlockState(pos.relative(dir))
            val stem = neighbor.block as? AttachedStemBlock ?: continue
            if (neighbor.getValue(AttachedStemBlock.FACING) != dir.opposite) continue
            if (stemMatchesFruit(stem, fruit)) return true
        }
        return false
    }

    private fun stemMatchesFruit(stem: AttachedStemBlock, fruit: BlockState): Boolean {
        if (stem === Blocks.ATTACHED_MELON_STEM) return fruit.`is`(Blocks.MELON)
        if (stem === Blocks.ATTACHED_PUMPKIN_STEM) return fruit.`is`(Blocks.PUMPKIN)
        val stemPath = BuiltInRegistries.BLOCK.getKey(stem).path
        val fruitPath = BuiltInRegistries.BLOCK.getKey(fruit.block).path
        val stemKind = stemPath.removePrefix("attached_").removeSuffix("_stem")
        if (stemKind != stemPath && (fruitPath == stemKind || fruitPath.endsWith("_$stemKind"))) return true
        return stemPath.contains("melon") && fruitPath.endsWith("melon") ||
            stemPath.contains("pumpkin") && !stemPath.contains("melon") && fruitPath.endsWith("pumpkin")
    }

    fun hydrateFarmland(level: ServerLevel, pos: BlockPos) {
        val candidates = listOf(pos, pos.below())
        for (soil in candidates) {
            val soilState = level.getBlockState(soil)
            if (soilState.block is FarmBlock && soilState.hasProperty(FarmBlock.MOISTURE)) {
                if (soilState.getValue(FarmBlock.MOISTURE) < 7) {
                    level.setBlock(soil, soilState.setValue(FarmBlock.MOISTURE, 7), Block.UPDATE_ALL)
                }
            }
        }
    }

    private fun previewCane(level: ServerLevel, pos: BlockPos, harvester: Entity): List<ItemStack> {
        var top = pos
        while (level.getBlockState(top.above()).block is SugarCaneBlock) {
            top = top.above()
        }
        var bottom = pos
        while (level.getBlockState(bottom.below()).block is SugarCaneBlock) {
            bottom = bottom.below()
        }
        val drops = ArrayList<ItemStack>()
        var cursor = top
        while (cursor.y > bottom.y) {
            val state = level.getBlockState(cursor)
            drops += Block.getDrops(state, level, cursor, null, harvester, ItemStack.EMPTY)
            cursor = cursor.below()
        }
        return drops
    }

    private fun harvestCane(level: ServerLevel, pos: BlockPos, harvester: Entity): List<ItemStack> {
        var top = pos
        while (level.getBlockState(top.above()).block is SugarCaneBlock) {
            top = top.above()
        }
        var bottom = pos
        while (level.getBlockState(bottom.below()).block is SugarCaneBlock) {
            bottom = bottom.below()
        }
        val drops = ArrayList<ItemStack>()
        var cursor = top
        while (cursor.y > bottom.y) {
            val state = level.getBlockState(cursor)
            drops += Block.getDrops(state, level, cursor, null, harvester, ItemStack.EMPTY)
            level.removeBlock(cursor, false)
            cursor = cursor.below()
        }
        return drops
    }

    /** Культура, от которой ферма растёт: рис в воде, куст, обычная грядка. Колючий куст сюда не входит. */
    fun isAnchorCrop(level: Level, pos: BlockPos): Boolean {
        if (!level.isLoaded(pos)) return false
        return isMatureCrop(level, pos) || isImmatureCrop(level, pos)
    }

    fun isPricklyBush(state: BlockState): Boolean {
        val block = state.block
        if (block is SweetBerryBushBlock) return true
        if (state.`is`(ModTags.PRICKLY_BUSHES)) return true
        return CropRules.isPricklyPath(BuiltInRegistries.BLOCK.getKey(block).path)
    }

    private fun isAgeCrop(state: BlockState): Boolean {
        if (isPricklyBush(state)) return false
        val block = state.block
        if (state.`is`(BlockTags.CROPS) || block is BushBlock) return true
        return isModAgeCrop(state)
    }

    /** Чужой блок с возрастом и костной мукой: виноградная лоза, которая не наследует куст. */
    private fun isModAgeCrop(state: BlockState): Boolean {
        val block = state.block
        if (block is CropBlock || block is BushBlock || block is StemBlock || block is NetherWartBlock) return false
        if (ageProperty(state) == null) return false
        if (BuiltInRegistries.BLOCK.getKey(block).namespace == "minecraft") return false
        return block is BonemealableBlock
    }

    /** Рис в воде или на грязи и куст не на пашне. Пшеница на пашне сюда не входит: её и так находит грядка. */
    fun isSpecialAnchor(level: Level, pos: BlockPos): Boolean {
        if (!level.isLoaded(pos) || !isAnchorCrop(level, pos)) return false
        val state = level.getBlockState(pos)
        val block = state.block
        if (block is BushBlock && block !is CropBlock) return true
        if (state.hasProperty(BlockStateProperties.WATERLOGGED) && state.getValue(BlockStateProperties.WATERLOGGED)) return true
        return !isClassicFarmSoil(level.getBlockState(pos.below()))
    }

    private fun hasSpecialCropNearby(level: Level, pos: BlockPos): Boolean {
        if (isSpecialAnchor(level, pos.above())) return true
        for (dir in Direction.Plane.HORIZONTAL) {
            val side = pos.relative(dir)
            if (isSpecialAnchor(level, side) || isSpecialAnchor(level, side.above())) return true
        }
        return false
    }

    private fun placedState(level: Level, pos: BlockPos, block: Block): BlockState {
        var state = block.defaultBlockState()
        val age = ageProperty(state)
        if (age != null) {
            val youngest = age.possibleValues.minOrNull()
            if (youngest != null) state = state.setValue(age, youngest)
        }
        if (state.hasProperty(BlockStateProperties.WATERLOGGED)) {
            state = state.setValue(BlockStateProperties.WATERLOGGED, level.getFluidState(pos).`is`(FluidTags.WATER))
        }
        return state
    }

    private fun canReplaceWithCrop(state: BlockState): Boolean {
        if (state.isAir) return true
        val fluid = state.fluidState
        return state.block is LiquidBlock && fluid.`is`(FluidTags.WATER) && fluid.isSource
    }

    private fun isClassicFarmSoil(state: BlockState): Boolean {
        if (state.block is FarmBlock || state.`is`(ModTags.FARMLAND)) return true
        return BuiltInRegistries.BLOCK.getKey(state.block).path.contains("farmland")
    }

    private fun isLooseSoil(state: BlockState): Boolean =
        state.`is`(BlockTags.DIRT) || state.`is`(Blocks.MUD)

    private fun ageProperty(state: BlockState): IntegerProperty? {
        return state.properties.firstOrNull { it.name == "age" && it is IntegerProperty } as? IntegerProperty
    }
}
