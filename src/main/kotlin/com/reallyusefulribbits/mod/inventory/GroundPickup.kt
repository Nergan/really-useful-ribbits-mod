package com.reallyusefulribbits.mod.inventory

import com.reallyusefulribbits.mod.config.ModConfig
import com.reallyusefulribbits.mod.logic.ProfessionKind
import com.reallyusefulribbits.mod.util.work
import com.reallyusefulribbits.mod.world.CropSupport
import com.yungnickyoung.minecraft.ribbits.entity.RibbitEntity
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.tags.ItemTags
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.CropBlock
import net.minecraft.world.level.block.NetherWartBlock
import net.minecraft.world.level.block.SugarCaneBlock

object GroundPickup {
    fun tick(level: ServerLevel, ribbit: RibbitEntity, kind: ProfessionKind) {
        if (RibbitBags.isFull(ribbit.work(), kind)) return
        val box = ribbit.boundingBox.inflate(ModConfig.GROUND_PICKUP_RANGE)
        val items = level.getEntitiesOfClass(ItemEntity::class.java, box) { item ->
            item.isAlive && item.tickCount >= 8 && isRelevant(kind, item.item)
        }
        for (item in items) {
            val leftover = RibbitBags.insert(ribbit.work(), kind, item.item.copy())
            if (leftover.isEmpty) {
                item.discard()
            } else if (leftover.count != item.item.count) {
                item.item = leftover
            } else {
                continue
            }
            level.playSound(null, ribbit.blockPosition(), SoundEvents.ITEM_PICKUP, SoundSource.NEUTRAL, 0.35f, 1.1f)
            if (RibbitBags.isFull(ribbit.work(), kind)) return
        }
    }

    fun isRelevant(kind: ProfessionKind, stack: ItemStack): Boolean = when (kind) {
        ProfessionKind.FARMER -> isFarmerLoot(stack)
        ProfessionKind.FISHERMAN -> isFisherLoot(stack)
        else -> false
    }

    fun isFarmerLoot(stack: ItemStack): Boolean {
        if (stack.isEmpty) return false
        if (CropSupport.plantableBlock(stack) != null) return true
        if (stack.`is`(ItemTags.VILLAGER_PLANTABLE_SEEDS)) return true
        val item = stack.item
        if (item is BlockItem) {
            val block = item.block
            if (CropSupport.isStemFruit(block.defaultBlockState())) return true
            if (block is CropBlock || block is NetherWartBlock || block is SugarCaneBlock) return true
        }
        return item == Items.WHEAT ||
            item == Items.CARROT ||
            item == Items.POTATO ||
            item == Items.BEETROOT ||
            item == Items.BEETROOT_SEEDS ||
            item == Items.WHEAT_SEEDS ||
            item == Items.PUMPKIN_SEEDS ||
            item == Items.MELON_SEEDS ||
            item == Items.MELON_SLICE ||
            item == Items.MELON ||
            item == Items.PUMPKIN ||
            item == Items.NETHER_WART ||
            item == Items.SUGAR_CANE ||
            item == Items.GLOW_BERRIES ||
            item == Items.TORCHFLOWER_SEEDS ||
            item == Items.PITCHER_POD ||
            item == Items.BONE_MEAL
    }

    fun isFisherLoot(stack: ItemStack): Boolean {
        if (stack.isEmpty) return false
        if (stack.`is`(ItemTags.FISHES)) return true
        val item = stack.item
        return item == Items.COD ||
            item == Items.SALMON ||
            item == Items.TROPICAL_FISH ||
            item == Items.PUFFERFISH ||
            item == Items.LILY_PAD ||
            item == Items.BOWL ||
            item == Items.LEATHER ||
            item == Items.BONE ||
            item == Items.STRING ||
            item == Items.INK_SAC ||
            item == Items.NAUTILUS_SHELL ||
            item == Items.SADDLE ||
            item == Items.NAME_TAG ||
            item == Items.FISHING_ROD ||
            item == Items.ENCHANTED_BOOK ||
            item == Items.STICK ||
            item == Items.TRIPWIRE_HOOK ||
            item == Items.ROTTEN_FLESH ||
            item == Items.BAMBOO ||
            item == Items.COCOA_BEANS
    }
}
