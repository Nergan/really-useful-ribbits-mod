package com.reallyusefulribbits.mod.spell

import com.reallyusefulribbits.mod.event.ModAdvancements
import com.reallyusefulribbits.mod.logic.RoomShape
import net.minecraft.core.BlockPos
import net.minecraft.core.registries.Registries
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.NbtUtils
import net.minecraft.nbt.Tag
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.LightBlock

/**
 * «Замуровали!»: куб 16×16×16 из бедрока вокруг игрока. Через пять минут
 * всё, что стояло на его месте, возвращается вместе с содержимым сундуков.
 */
object BedrockRoom {
    const val DURATION_TICKS: Int = 20 * 60 * 5
    private const val FLAGS = Block.UPDATE_CLIENTS or Block.UPDATE_KNOWN_SHAPE

    fun build(level: ServerLevel, player: ServerPlayer): Boolean {
        val shape = RoomShape.around(player.blockX, player.blockY, player.blockZ)
        if (shape.minY < level.minBuildHeight || shape.maxY >= level.maxBuildHeight) return false
        if (!shape.corners().all { (x, z) -> level.hasChunk(x shr 4, z shr 4) }) return false
        val registries = level.registryAccess()
        val saved = ListTag()
        val cursor = BlockPos.MutableBlockPos()
        shape.forEach { x, y, z ->
            cursor.set(x, y, z)
            val entry = CompoundTag()
            entry.putInt("X", x)
            entry.putInt("Y", y)
            entry.putInt("Z", z)
            entry.put("State", NbtUtils.writeBlockState(level.getBlockState(cursor)))
            level.getBlockEntity(cursor)?.let { entry.put("Block", it.saveWithFullMetadata(registries)) }
            saved.add(entry)
        }
        shape.forEach { x, y, z ->
            cursor.set(x, y, z)
            if (level.getBlockEntity(cursor) != null) level.removeBlockEntity(cursor)
            val state = if (shape.isShell(x, y, z)) Blocks.BEDROCK.defaultBlockState() else Blocks.AIR.defaultBlockState()
            level.setBlock(cursor, state, FLAGS)
        }
        val lamp = BlockPos(shape.centerX, shape.maxY - 1, shape.centerZ)
        level.setBlock(lamp, Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, 15), FLAGS)
        player.teleportTo(shape.centerX + 0.5, shape.minY + 1.0, shape.centerZ + 0.5)
        val room = CompoundTag()
        room.putString("Dimension", level.dimension().location().toString())
        room.putLong("Until", level.server.overworld().gameTime + DURATION_TICKS)
        room.put("Blocks", saved)
        val data = SorcererWorldData.get(level.server)
        data.rooms += room
        data.changed()
        level.playSound(null, player.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 1f, 0.5f)
        ModAdvancements.grantWalledIn(player)
        return true
    }

    fun tick(server: MinecraftServer) {
        if (server.tickCount % 20 != 0) return
        val data = SorcererWorldData.get(server)
        if (data.rooms.isEmpty()) return
        val now = server.overworld().gameTime
        val iterator = data.rooms.iterator()
        var changed = false
        while (iterator.hasNext()) {
            val room = iterator.next()
            if (room.getLong("Until") > now) continue
            val key = ResourceLocation.tryParse(room.getString("Dimension"))
            val level = key?.let { server.getLevel(ResourceKey.create(Registries.DIMENSION, it)) }
            if (level == null) {
                iterator.remove()
                changed = true
                continue
            }
            if (restore(level, room)) {
                iterator.remove()
                changed = true
            }
        }
        if (changed) data.changed()
    }

    /** Ждёт, пока чанки комнаты загружены: восстановление пишет каждый блок. */
    private fun restore(level: ServerLevel, room: CompoundTag): Boolean {
        val blocks = room.getList("Blocks", Tag.TAG_COMPOUND.toInt())
        if (blocks.isEmpty()) return true
        val first = blocks.getCompound(0)
        val last = blocks.getCompound(blocks.size - 1)
        val corners = listOf(first.getInt("X") to first.getInt("Z"), last.getInt("X") to last.getInt("Z"))
        if (!corners.all { (x, z) -> level.hasChunk(x shr 4, z shr 4) }) return false
        val lookup = level.holderLookup(Registries.BLOCK)
        val registries = level.registryAccess()
        val cursor = BlockPos.MutableBlockPos()
        for (index in 0 until blocks.size) {
            val entry = blocks.getCompound(index)
            cursor.set(entry.getInt("X"), entry.getInt("Y"), entry.getInt("Z"))
            val state = NbtUtils.readBlockState(lookup, entry.getCompound("State"))
            level.setBlock(cursor, state, FLAGS)
            if (entry.contains("Block")) {
                level.getBlockEntity(cursor)?.let {
                    it.loadWithComponents(entry.getCompound("Block"), registries)
                    it.setChanged()
                }
            }
        }
        level.playSound(null, cursor, SoundEvents.BEACON_DEACTIVATE, SoundSource.BLOCKS, 1f, 0.6f)
        return true
    }
}
