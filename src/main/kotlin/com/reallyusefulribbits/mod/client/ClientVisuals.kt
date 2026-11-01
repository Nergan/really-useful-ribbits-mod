package com.reallyusefulribbits.mod.client

import com.reallyusefulribbits.mod.network.PlayerVisualPayload
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.player.Player
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/** Облик игроков, который сервер прислал клиенту. Сам класс клиентских классов не трогает. */
object ClientVisuals {
    private val entries = ConcurrentHashMap<UUID, PlayerVisualPayload>()

    fun set(id: UUID, inverted: Boolean, morphId: String = "") {
        set(PlayerVisualPayload(id, inverted, morphId))
    }

    fun set(payload: PlayerVisualPayload) {
        val empty = !payload.upsideDown && payload.morphId.isBlank() && payload.flags == 0 &&
            payload.nickname.isBlank() && payload.shader.isBlank()
        if (empty) entries.remove(payload.playerId) else entries[payload.playerId] = payload
    }

    fun isUpsideDown(id: UUID): Boolean = entries[id]?.upsideDown == true

    /** Переворот проклятием и перевёрнутая гравитация VVVVVV гасят друг друга. */
    fun rendersUpsideDown(player: Player): Boolean {
        if (morphType(player.uuid) != null) return false
        val gravityFlipped = player.getAttributeValue(Attributes.GRAVITY) < 0.0
        return isUpsideDown(player.uuid) != gravityFlipped
    }

    fun morphId(id: UUID): String = entries[id]?.morphId.orEmpty()

    fun morphType(id: UUID): EntityType<*>? {
        val raw = entries[id]?.morphId ?: return null
        if (raw.isBlank()) return null
        return try {
            BuiltInRegistries.ENTITY_TYPE.getOptional(ResourceLocation.parse(raw)).orElse(null)
        } catch (_: Exception) {
            null
        }
    }

    fun flags(id: UUID): Int = entries[id]?.flags ?: 0

    fun nickname(id: UUID): PlayerVisualPayload? = entries[id]?.takeIf { it.nickname.isNotBlank() }

    fun shader(id: UUID): String = entries[id]?.shader.orEmpty()

    fun clear(id: UUID) {
        entries.remove(id)
    }

    fun clearAll() {
        entries.clear()
    }
}
