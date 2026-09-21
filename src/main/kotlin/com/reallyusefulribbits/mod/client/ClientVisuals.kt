package com.reallyusefulribbits.mod.client

import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.EntityType
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object ClientVisuals {
    private val upsideDown = ConcurrentHashMap<UUID, Boolean>()
    private val morphs = ConcurrentHashMap<UUID, String>()

    fun set(id: UUID, inverted: Boolean, morphId: String = "") {
        if (inverted) upsideDown[id] = true else upsideDown.remove(id)
        if (morphId.isBlank()) morphs.remove(id) else morphs[id] = morphId
    }

    fun isUpsideDown(id: UUID): Boolean = upsideDown[id] == true

    fun morphId(id: UUID): String = morphs[id].orEmpty()

    fun morphType(id: UUID): EntityType<*>? {
        val raw = morphs[id] ?: return null
        if (raw.isBlank()) return null
        return try {
            BuiltInRegistries.ENTITY_TYPE.getOptional(ResourceLocation.parse(raw)).orElse(null)
        } catch (_: Exception) {
            null
        }
    }

    fun clear(id: UUID) {
        upsideDown.remove(id)
        morphs.remove(id)
    }
}
