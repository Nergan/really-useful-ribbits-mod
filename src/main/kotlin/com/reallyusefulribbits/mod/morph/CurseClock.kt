package com.reallyusefulribbits.mod.morph

import com.reallyusefulribbits.mod.loader.RuntimeHooks
import com.reallyusefulribbits.mod.logic.SpellDurations
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.entity.player.Player

/** Срок состояний чародея и строка для полоски на клиенте. */
object CurseClock {
    fun ticks(id: String): Int = SpellDurations.ticks(RuntimeHooks.spellSeconds(id))

    fun arm(player: ServerPlayer, id: String) {
        val seconds = RuntimeHooks.spellSeconds(id)
        val data = RuntimeHooks.visual(player)
        data.until[id] = if (seconds < 0) -1L else player.level().gameTime + seconds * 20L
    }

    fun disarm(player: Player, id: String) {
        RuntimeHooks.visual(player).until.remove(id)
    }

    fun tick(player: ServerPlayer) {
        val data = RuntimeHooks.visual(player)
        val now = player.level().gameTime
        val expired = data.until.filter { (_, until) -> until >= 0L && until <= now }.keys.toList()
        for (id in expired) {
            data.until.remove(id)
            SorcererCurse.expire(player, id)
        }
        if (player.tickCount % 20 == 0) {
            refreshEffects(player)
            if (describe(player).isNotEmpty()) SorcererCurse.sync(player)
        }
    }

    /** `id=секунды` через запятую. -1 значит «пока не снимут». */
    fun describe(player: ServerPlayer): String {
        val data = RuntimeHooks.visual(player)
        val now = player.level().gameTime
        val parts = ArrayList<String>()
        fun add(id: String, active: Boolean) {
            if (!active) return
            val until = data.until[id]
            val seconds = when {
                until == null || until < 0L -> -1
                else -> ((until - now) / 20L).toInt().coerceAtLeast(0)
            }
            parts += "$id=$seconds"
        }
        add("flip", data.upsideDown)
        add("scale", data.until.containsKey("scale"))
        add("max_health", data.until.containsKey("max_health"))
        add("morph", data.morphId.isNotBlank())
        add("creative_flight", data.foreverFlight)
        add("super_secret_settings", data.shader.isNotBlank())
        add("inverted_controls", data.invertedControls)
        add("vvvvvv", data.vvvvvv)
        add("nickname", data.nickname.isNotBlank())
        add("fireballs", data.fireballs)
        add("lightning_hands", data.lightningHands)
        add("bedrock_room", data.until.containsKey("bedrock_room"))
        if (data.spectatorTicks != 0) {
            val seconds = if (data.spectatorTicks < 0) -1 else data.spectatorTicks / 20
            parts += "spectator=$seconds"
        }
        for (entry in data.curseEffects) {
            val id = entry.substringBefore('|')
            val holder = BuiltInRegistries.MOB_EFFECT.getHolder(ResourceLocation.parse(id)).orElse(null) ?: continue
            val effect = player.getEffect(holder) ?: continue
            val seconds = if (effect.duration < 0) -1 else effect.duration / 20
            parts += "effect:$id=$seconds"
        }
        return parts.joinToString(",")
    }

    private fun refreshEffects(player: ServerPlayer) {
        val data = RuntimeHooks.visual(player)
        if (data.curseEffects.isEmpty()) return
        val next = ArrayList<String>(data.curseEffects.size)
        for (entry in data.curseEffects) {
            val id = entry.substringBefore('|')
            val amp = entry.split('|').getOrNull(1)?.toIntOrNull() ?: 0
            val holder = BuiltInRegistries.MOB_EFFECT.getHolder(ResourceLocation.parse(id)).orElse(null)
            val effect = holder?.let { player.getEffect(it) }
            if (effect == null) continue
            next += "$id|$amp|${effect.duration}"
        }
        data.curseEffects.clear()
        data.curseEffects.addAll(next)
    }

    fun effectDuration(entry: String): Int {
        val ticks = entry.split('|').getOrNull(2)?.toIntOrNull() ?: -1
        return if (ticks < 0) -1 else ticks
    }

    fun applyStoredEffect(player: ServerPlayer, entry: String) {
        val id = entry.substringBefore('|')
        val amp = entry.split('|').getOrNull(1)?.toIntOrNull() ?: 0
        val holder = BuiltInRegistries.MOB_EFFECT.getHolder(ResourceLocation.parse(id)).orElse(null) ?: return
        player.addEffect(MobEffectInstance(holder, effectDuration(entry), amp, false, true))
    }
}
