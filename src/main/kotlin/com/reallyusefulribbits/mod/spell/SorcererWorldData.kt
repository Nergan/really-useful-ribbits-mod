package com.reallyusefulribbits.mod.spell

import net.minecraft.core.BlockPos
import net.minecraft.core.HolderLookup
import net.minecraft.core.registries.Registries
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.NbtUtils
import net.minecraft.nbt.Tag
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.MinecraftServer
import net.minecraft.world.level.Level
import net.minecraft.world.level.saveddata.SavedData
import java.util.UUID

/** Состояние мира от заклинаний чародея. Живёт в данных верхнего мира. */
class SorcererWorldData : SavedData() {
    var doomActive: Boolean = false
    var doomWarden: UUID? = null
    var doomDimension: ResourceKey<Level> = Level.OVERWORLD
    var doomLastPos: BlockPos = BlockPos.ZERO
    var doomBloomOrigin: BlockPos? = null
    var corruptionActive: Boolean = false
    val rooms: MutableList<CompoundTag> = ArrayList()

    override fun save(tag: CompoundTag, registries: HolderLookup.Provider): CompoundTag {
        tag.putBoolean("DoomActive", doomActive)
        doomWarden?.let { tag.putUUID("DoomWarden", it) }
        tag.putString("DoomDimension", doomDimension.location().toString())
        tag.put("DoomLastPos", NbtUtils.writeBlockPos(doomLastPos))
        doomBloomOrigin?.let { tag.put("DoomBloomOrigin", NbtUtils.writeBlockPos(it)) }
        tag.putBoolean("CorruptionActive", corruptionActive)
        val list = ListTag()
        rooms.forEach { list.add(it) }
        tag.put("Rooms", list)
        return tag
    }

    fun changed() {
        setDirty()
    }

    companion object {
        private const val NAME = "reallyusefulribbits_sorcerer"

        private val FACTORY: SavedData.Factory<SorcererWorldData> = SavedData.Factory(
            { SorcererWorldData() },
            { tag, _ -> load(tag) },
            null,
        )

        fun get(server: MinecraftServer): SorcererWorldData =
            server.overworld().dataStorage.computeIfAbsent(FACTORY, NAME)

        private fun load(tag: CompoundTag): SorcererWorldData {
            val data = SorcererWorldData()
            data.doomActive = tag.getBoolean("DoomActive")
            data.doomWarden = if (tag.hasUUID("DoomWarden")) tag.getUUID("DoomWarden") else null
            val dimension = ResourceLocation.tryParse(tag.getString("DoomDimension"))
            if (dimension != null) data.doomDimension = ResourceKey.create(Registries.DIMENSION, dimension)
            data.doomLastPos = NbtUtils.readBlockPos(tag, "DoomLastPos").orElse(BlockPos.ZERO)
            data.doomBloomOrigin = NbtUtils.readBlockPos(tag, "DoomBloomOrigin").orElse(null)
            data.corruptionActive = tag.getBoolean("CorruptionActive")
            val rooms = tag.getList("Rooms", Tag.TAG_COMPOUND.toInt())
            for (index in 0 until rooms.size) data.rooms += rooms.getCompound(index)
            return data
        }
    }
}
