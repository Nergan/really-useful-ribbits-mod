package com.reallyusefulribbits.mod.world

import net.minecraft.core.BlockPos
import net.minecraft.resources.ResourceKey
import net.minecraft.world.level.Level

object BlockReservation {
    private data class Claim(val entityId: Int, val expiresAt: Long)

    private val claims = HashMap<Pair<ResourceKey<Level>, Long>, Claim>()

    fun holder(level: Level, pos: BlockPos, now: Long): Int? {
        val current = claims[level.dimension() to pos.asLong()] ?: return null
        if (current.expiresAt <= now) return null
        return current.entityId
    }

    fun tryClaim(level: Level, pos: BlockPos, entityId: Int, now: Long, duration: Int = 80): Boolean {
        val key = level.dimension() to pos.asLong()
        val current = claims[key]
        if (current != null && current.entityId != entityId && current.expiresAt > now) return false
        claims[key] = Claim(entityId, now + duration)
        return true
    }

    fun release(level: Level, pos: BlockPos, entityId: Int) {
        val key = level.dimension() to pos.asLong()
        val current = claims[key]
        if (current != null && current.entityId == entityId) claims.remove(key)
    }

    fun cleanup(now: Long) {
        claims.entries.removeIf { it.value.expiresAt <= now }
    }
}
