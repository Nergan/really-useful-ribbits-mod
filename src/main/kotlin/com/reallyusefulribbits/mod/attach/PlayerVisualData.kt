package com.reallyusefulribbits.mod.attach

import net.minecraft.nbt.CompoundTag

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
    var receivedGuide: Boolean = false,
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
        out.putBoolean("ReceivedGuide", receivedGuide)
        return out
    }

    companion object {
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
            receivedGuide = tag.getBoolean("ReceivedGuide"),
        )
    }
}
