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
import net.minecraft.tags.ItemTags
import net.minecraft.world.entity.Entity
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.LevelEvent
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.AttachedStemBlock
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.BonemealableBlock
import net.minecraft.world.level.block.BushBlock
import net.minecraft.world.level.block.CaveVines
import net.minecraft.world.level.block.CropBlock
import net.minecraft.world.level.block.FarmBlock
import net.minecraft.world.level.block.NetherWartBlock
import net.minecraft.world.level.block.StemBlock
import net.minecraft.world.level.block.SugarCaneBlock
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.IntegerProperty

object CropSupport {
    fun isFarmBlock(level: Level, pos: BlockPos): Boolean {
        val state = level.getBlockState(pos)
        val block = state.block
        if (state.`is`(ModTags.FARMLAND)) return true
        if (block is FarmBlock) return true
        val id = BuiltInRegistries.BLOCK.getKey(block)
        if (id.path.contains("farmland")) return true
        if (state.block is SugarCaneBlock && level.getBlockState(pos.below()).block !is SugarCaneBlock) return true
        if (state.`is`(Blocks.SOUL_SAND)) return true
        if (block is CaveVines || state.`is`(Blocks.CAVE_VINES) || state.`is`(Blocks.CAVE_VINES_PLANT)) return true
        return false
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
            if (block is CropBlock || block is StemBlock || block is NetherWartBlock || block is SugarCaneBlock) return block
            if (block.defaultBlockState().`is`(BlockTags.CROPS)) return block
            if (stack.`is`(ItemTags.VILLAGER_PLANTABLE_SEEDS)) return block
        }
        return null
    }

    fun canPlant(level: Level, soil: BlockPos, stack: ItemStack): Boolean {
        val block = plantableBlock(stack) ?: return false
        return canPlantOn(level, soil, block)
    }

    /** Песок душ принимает только незерский нарост, даже если он в теге фермы. */
    fun canPlantOn(level: Level, soil: BlockPos, plant: Block): Boolean {
        val soilState = level.getBlockState(soil)
        val above = level.getBlockState(soil.above())
        if (!above.isAir) return false
        val soulSand = soilState.`is`(Blocks.SOUL_SAND)
        return when (plant) {
            is NetherWartBlock -> soulSand
            is SugarCaneBlock -> !soulSand && (
                soilState.`is`(Blocks.SAND) || soilState.`is`(Blocks.RED_SAND) ||
                    soilState.`is`(BlockTags.DIRT) || soilState.block is FarmBlock
                )
            else -> !soulSand && (soilState.block is FarmBlock || soilState.`is`(ModTags.FARMLAND))
        }
    }

    fun plant(level: ServerLevel, soil: BlockPos, stack: ItemStack): Boolean {
        val block = plantableBlock(stack) ?: return false
        if (!canPlantOn(level, soil, block)) return false
        level.setBlock(soil.above(), block.defaultBlockState(), Block.UPDATE_ALL)
        stack.shrink(1)
        level.playSound(null, soil, SoundEvents.CROP_PLANTED, SoundSource.BLOCKS, 1f, 1f)
        return true
    }

    fun canBonemeal(level: Level, pos: BlockPos): Boolean {
        val state = level.getBlockState(pos)
        val block = state.block as? BonemealableBlock ?: return false
        if (!block.isValidBonemealTarget(level, pos, state)) return false
        if (block is CropBlock || block is StemBlock) return true
        if (state.`is`(BlockTags.CROPS)) return true
        return isCaveVine(state) && !CaveVines.hasGlowBerries(state)
    }

    fun applyBonemeal(level: ServerLevel, pos: BlockPos, stack: ItemStack): Boolean {
        if (!stack.`is`(Items.BONE_MEAL) || !canBonemeal(level, pos)) return false
        val state = level.getBlockState(pos)
        val block = state.block as BonemealableBlock
        if (block.isBonemealSuccess(level, level.random, pos, state)) {
            block.performBonemeal(level, level.random, pos, state)
        }
        stack.shrink(1)
        level.levelEvent(LevelEvent.PARTICLES_AND_SOUND_PLANT_GROWTH, pos, 0)
        return true
    }

    fun isMatureCrop(level: Level, pos: BlockPos): Boolean {
        val state = level.getBlockState(pos)
        val block = state.block
        if (block is StemBlock || block is AttachedStemBlock) return false
        if (block is CropBlock) return block.isMaxAge(state)
        if (block is NetherWartBlock) return state.getValue(NetherWartBlock.AGE) >= 3
        if (isStemFruit(state) && CropRules.shouldHarvestStemFruit(hasStemNeighbor(level, pos))) return true
        if (block is SugarCaneBlock && level.getBlockState(pos.below()).block is SugarCaneBlock) return true
        if (CaveVines.hasGlowBerries(state)) return true
        val age = ageProperty(state)
        if (age != null) {
            val max = age.possibleValues.maxOrNull() ?: return false
            return state.getValue(age) >= max && (state.`is`(BlockTags.CROPS) || block is BushBlock)
        }
        return false
    }

    fun isImmatureCrop(level: Level, pos: BlockPos): Boolean {
        val state = level.getBlockState(pos)
        val block = state.block
        if (block is NetherWartBlock) return false
        if (block is StemBlock || block is AttachedStemBlock) return false
        if (isCaveVine(state) && !CaveVines.hasGlowBerries(state)) return true
        if (block is CropBlock) return !block.isMaxAge(state)
        if (state.`is`(BlockTags.CROPS) && block is BonemealableBlock) {
            return block.isValidBonemealTarget(level, pos, state)
        }
        val age = ageProperty(state)
        if (age != null && (state.`is`(BlockTags.CROPS) || block is BushBlock)) {
            val max = age.possibleValues.maxOrNull() ?: return false
            return state.getValue(age) < max
        }
        return false
    }

    fun isEmptyFarmland(level: Level, pos: BlockPos): Boolean {
        if (!isFarmBlock(level, pos)) return false
        if (level.getBlockState(pos).block is SugarCaneBlock) return false
        if (isCaveVine(level.getBlockState(pos))) return false
        if (level.getBlockState(pos).`is`(Blocks.SOUL_SAND)) {
            return level.getBlockState(pos.above()).isAir
        }
        return level.getBlockState(pos.above()).isAir
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

    fun hasStemNeighbor(level: Level, pos: BlockPos): Boolean {
        for (dir in Direction.Plane.HORIZONTAL) {
            val neighbor = level.getBlockState(pos.relative(dir))
            if (neighbor.block is StemBlock || neighbor.block is AttachedStemBlock) return true
        }
        return false
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

    private fun ageProperty(state: BlockState): IntegerProperty? {
        return state.properties.firstOrNull { it.name == "age" && it is IntegerProperty } as? IntegerProperty
    }
}
