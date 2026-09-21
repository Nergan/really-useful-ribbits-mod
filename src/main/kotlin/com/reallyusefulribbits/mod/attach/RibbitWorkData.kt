package com.reallyusefulribbits.mod.attach

import com.reallyusefulribbits.mod.logic.FarmerTask
import com.reallyusefulribbits.mod.logic.FishingPhaseTimes
import com.reallyusefulribbits.mod.logic.MerchantPhase
import com.reallyusefulribbits.mod.logic.ProfessionKind
import net.minecraft.core.BlockPos
import net.minecraft.core.HolderLookup
import net.minecraft.core.NonNullList
import net.minecraft.core.component.DataComponents
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.NbtUtils
import net.minecraft.nbt.Tag
import net.minecraft.world.item.ItemStack

class RibbitWorkData {
    var containerPos: BlockPos? = null
    var waterPos: BlockPos? = null
    var farmOrigin: BlockPos? = null
    var task: String = FarmerTask.IDLE.name
    var taskTicks: Int = 0
    var cooldown: Int = 0
    var lastScanAt: Long = 0
    var lastFarmScanAt: Long = 0
    var fishingElapsed: Int = 0
    var fishingWait: Int = 0
    var fishingApproach: Int = 0
    var fishingBite: Int = 0
    var fishingActive: Boolean = false
    var bobberX: Double = 0.0
    var bobberY: Double = 0.0
    var bobberZ: Double = 0.0
    var merchantPhase: String = MerchantPhase.INIT.name
    var merchantTargetId: Int = -1
    var merchantTicks: Int = 0
    var merchantReady: Boolean = false
    var riding: Boolean = false
    var savedInstrument: String = "none"
    val items: NonNullList<ItemStack> = NonNullList.withSize(27, ItemStack.EMPTY)

    fun fishingTimes(): FishingPhaseTimes = FishingPhaseTimes(fishingWait, fishingApproach, fishingBite)

    fun farmerTask(): FarmerTask = runCatching { FarmerTask.valueOf(task) }.getOrDefault(FarmerTask.IDLE)

    fun merchantPhaseEnum(): MerchantPhase =
        runCatching { MerchantPhase.valueOf(merchantPhase) }.getOrDefault(MerchantPhase.INIT)

    fun slotLimit(kind: ProfessionKind): Int = when (kind) {
        ProfessionKind.FISHERMAN -> 16
        ProfessionKind.FARMER -> 64
        ProfessionKind.MERCHANT -> 256
        else -> 64
    }

    fun usedSlots(kind: ProfessionKind): Int = when (kind) {
        ProfessionKind.FISHERMAN -> 1
        ProfessionKind.FARMER -> 4
        ProfessionKind.MERCHANT -> 27
        else -> 0
    }

    fun applyLimit(stack: ItemStack, kind: ProfessionKind): ItemStack {
        if (stack.isEmpty) return stack
        val cap = minOf(slotLimit(kind), stack.item.defaultMaxStackSize)
        stack.set(DataComponents.MAX_STACK_SIZE, cap)
        if (stack.count > cap) stack.count = cap
        return stack
    }

    fun save(provider: HolderLookup.Provider): CompoundTag {
        val tag = CompoundTag()
        containerPos?.let { tag.put("Container", NbtUtils.writeBlockPos(it)) }
        waterPos?.let { tag.put("Water", NbtUtils.writeBlockPos(it)) }
        farmOrigin?.let { tag.put("Farm", NbtUtils.writeBlockPos(it)) }
        tag.putString("Task", task)
        tag.putInt("TaskTicks", taskTicks)
        tag.putInt("Cooldown", cooldown)
        tag.putLong("LastScan", lastScanAt)
        tag.putLong("LastFarmScan", lastFarmScanAt)
        tag.putInt("FishElapsed", fishingElapsed)
        tag.putInt("FishWait", fishingWait)
        tag.putInt("FishApproach", fishingApproach)
        tag.putInt("FishBite", fishingBite)
        tag.putBoolean("FishActive", fishingActive)
        tag.putDouble("BobberX", bobberX)
        tag.putDouble("BobberY", bobberY)
        tag.putDouble("BobberZ", bobberZ)
        tag.putString("MerchantPhase", merchantPhase)
        tag.putInt("MerchantTarget", merchantTargetId)
        tag.putInt("MerchantTicks", merchantTicks)
        tag.putBoolean("MerchantReady", merchantReady)
        tag.putBoolean("Riding", riding)
        tag.putString("SavedInstrument", savedInstrument)
        val list = ListTag()
        for (stack in items) {
            val itemTag = CompoundTag()
            if (!stack.isEmpty) {
                list.add(stack.save(provider, itemTag))
            } else {
                list.add(itemTag)
            }
        }
        tag.put("Items", list)
        return tag
    }

    companion object {
        fun load(tag: CompoundTag, provider: HolderLookup.Provider): RibbitWorkData {
            val data = RibbitWorkData()
            if (tag.contains("Container")) data.containerPos = NbtUtils.readBlockPos(tag, "Container").orElse(null)
            if (tag.contains("Water")) data.waterPos = NbtUtils.readBlockPos(tag, "Water").orElse(null)
            if (tag.contains("Farm")) data.farmOrigin = NbtUtils.readBlockPos(tag, "Farm").orElse(null)
            data.task = tag.getString("Task").ifEmpty { FarmerTask.IDLE.name }
            data.taskTicks = tag.getInt("TaskTicks")
            data.cooldown = tag.getInt("Cooldown")
            data.lastScanAt = tag.getLong("LastScan")
            data.lastFarmScanAt = tag.getLong("LastFarmScan")
            data.fishingElapsed = tag.getInt("FishElapsed")
            data.fishingWait = tag.getInt("FishWait")
            data.fishingApproach = tag.getInt("FishApproach")
            data.fishingBite = tag.getInt("FishBite")
            data.fishingActive = tag.getBoolean("FishActive")
            data.bobberX = tag.getDouble("BobberX")
            data.bobberY = tag.getDouble("BobberY")
            data.bobberZ = tag.getDouble("BobberZ")
            data.merchantPhase = tag.getString("MerchantPhase").ifEmpty { MerchantPhase.INIT.name }
            data.merchantTargetId = tag.getInt("MerchantTarget")
            data.merchantTicks = tag.getInt("MerchantTicks")
            data.merchantReady = tag.getBoolean("MerchantReady")
            data.riding = tag.getBoolean("Riding")
            data.savedInstrument = tag.getString("SavedInstrument").ifEmpty { "none" }
            val list = tag.getList("Items", Tag.TAG_COMPOUND.toInt())
            for (i in 0 until minOf(list.size, data.items.size)) {
                val itemTag = list.getCompound(i)
                data.items[i] = if (itemTag.isEmpty) {
                    ItemStack.EMPTY
                } else {
                    ItemStack.parse(provider, itemTag).orElse(ItemStack.EMPTY)
                }
            }
            return data
        }
    }
}
