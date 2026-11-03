package com.reallyusefulribbits.mod.attach

import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.StringTag
import net.minecraft.nbt.Tag

class PlayerVisualData(
    var upsideDown: Boolean = false,
    var flightTicks: Int = 0,
    var headRideDismountArmed: Boolean = false,
    var morphId: String = "",
    var morphFlight: Boolean = false,
    var spectatorTicks: Int = 0,
    var previousGameMode: String = "survival",
    var attributesSaved: Boolean = false,
    var savedMaxHealth: Double = 20.0,
    var savedScale: Double = 1.0,
    var savedSpeed: Double = 0.1,
    var savedAttack: Double = 1.0,
    var savedKnockback: Double = 0.0,
    var savedStep: Double = 0.6,
    var savedGravity: Double = 0.08,
    var receivedGuide: Boolean = false,
    /** Эффекты чародея как `id|уровень`. После смерти они возвращаются, молоко их снимает. */
    val curseEffects: MutableList<String> = ArrayList(),
    var vvvvvv: Boolean = false,
    var fireballs: Boolean = false,
    var lightningHands: Boolean = false,
    var foreverFlight: Boolean = false,
    var nickname: String = "",
    var nickColor: Int = -1,
    var shader: String = "",
    var invertedControls: Boolean = false,
    /** Игровое время, когда состояние спадёт. -1 — пока не снимут яблоком. */
    val until: MutableMap<String, Long> = LinkedHashMap(),
) {
    fun write(): CompoundTag {
        val out = CompoundTag()
        out.putBoolean("UpsideDown", upsideDown)
        out.putInt("FlightTicks", flightTicks)
        out.putString("MorphId", morphId)
        out.putBoolean("MorphFlight", morphFlight)
        out.putInt("SpectatorTicks", spectatorTicks)
        out.putString("PrevGameMode", previousGameMode)
        out.putBoolean("AttrsSaved", attributesSaved)
        out.putDouble("SavedMaxHealth", savedMaxHealth)
        out.putDouble("SavedScale", savedScale)
        out.putDouble("SavedSpeed", savedSpeed)
        out.putDouble("SavedAttack", savedAttack)
        out.putDouble("SavedKnockback", savedKnockback)
        out.putDouble("SavedStep", savedStep)
        out.putDouble("SavedGravity", savedGravity)
        out.putBoolean("ReceivedGuide", receivedGuide)
        val effects = ListTag()
        curseEffects.forEach { effects.add(StringTag.valueOf(it)) }
        out.put("CurseEffects", effects)
        out.putBoolean("Vvvvvv", vvvvvv)
        out.putBoolean("Fireballs", fireballs)
        out.putBoolean("LightningHands", lightningHands)
        out.putBoolean("ForeverFlight", foreverFlight)
        out.putString("Nickname", nickname)
        out.putInt("NickColor", nickColor)
        out.putString("Shader", shader)
        out.putBoolean("InvertedControls", invertedControls)
        val times = CompoundTag()
        until.forEach { (id, ticks) -> times.putLong(id, ticks) }
        out.put("Until", times)
        return out
    }

    fun copy(): PlayerVisualData = load(write())

    companion object {
        private fun readUntil(tag: CompoundTag): MutableMap<String, Long> {
            val out = LinkedHashMap<String, Long>()
            for (key in tag.allKeys) out[key] = tag.getLong(key)
            return out
        }

        @JvmStatic
        fun load(tag: CompoundTag): PlayerVisualData = PlayerVisualData(
            upsideDown = tag.getBoolean("UpsideDown"),
            flightTicks = tag.getInt("FlightTicks"),
            morphId = tag.getString("MorphId"),
            morphFlight = tag.getBoolean("MorphFlight"),
            spectatorTicks = tag.getInt("SpectatorTicks"),
            previousGameMode = tag.getString("PrevGameMode").ifEmpty { "survival" },
            attributesSaved = tag.getBoolean("AttrsSaved"),
            savedMaxHealth = if (tag.contains("SavedMaxHealth")) tag.getDouble("SavedMaxHealth") else 20.0,
            savedScale = if (tag.contains("SavedScale")) tag.getDouble("SavedScale") else 1.0,
            savedSpeed = if (tag.contains("SavedSpeed")) tag.getDouble("SavedSpeed") else 0.1,
            savedAttack = if (tag.contains("SavedAttack")) tag.getDouble("SavedAttack") else 1.0,
            savedKnockback = tag.getDouble("SavedKnockback"),
            savedStep = if (tag.contains("SavedStep")) tag.getDouble("SavedStep") else 0.6,
            savedGravity = if (tag.contains("SavedGravity")) tag.getDouble("SavedGravity") else 0.08,
            receivedGuide = tag.getBoolean("ReceivedGuide"),
            curseEffects = tag.getList("CurseEffects", Tag.TAG_STRING.toInt()).mapTo(ArrayList()) { it.asString },
            vvvvvv = tag.getBoolean("Vvvvvv"),
            fireballs = tag.getBoolean("Fireballs"),
            lightningHands = tag.getBoolean("LightningHands"),
            foreverFlight = tag.getBoolean("ForeverFlight"),
            nickname = tag.getString("Nickname"),
            nickColor = if (tag.contains("NickColor")) tag.getInt("NickColor") else -1,
            shader = tag.getString("Shader"),
            invertedControls = tag.getBoolean("InvertedControls"),
            until = readUntil(tag.getCompound("Until")),
        )
    }
}
