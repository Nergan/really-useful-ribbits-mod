package com.reallyusefulribbits.mod.client

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation

/** Оставшееся время состояний чародея, в том числе тех, которых нет среди эффектов игры. */
object CurseHud {
    private var snapshot = ""
    private var atTick = 0

    fun render(graphics: GuiGraphics) {
        val minecraft = Minecraft.getInstance()
        val player = minecraft.player ?: return
        val raw = ClientVisuals.timers(player.uuid)
        if (raw.isBlank()) return
        if (raw != snapshot) {
            snapshot = raw
            atTick = player.tickCount
        }
        val elapsed = ((player.tickCount - atTick) / 20).coerceAtLeast(0)
        val font = minecraft.font
        var y = 4
        for (piece in raw.split(',')) {
            val split = piece.split('=', limit = 2)
            if (split.size != 2) continue
            val seconds = split[1].toIntOrNull() ?: continue
            val left = if (seconds < 0) -1 else (seconds - elapsed).coerceAtLeast(0)
            val name = nameOf(split[0])
            val time = if (left < 0) {
                Component.translatable("reallyusefulribbits.curse.forever")
            } else {
                Component.literal(clock(left))
            }
            val line = Component.translatable("reallyusefulribbits.curse.line", name, time)
            graphics.drawString(font, line, 4, y, 0xFFFFFF, true)
            y += font.lineHeight + 2
        }
    }

    private fun nameOf(id: String): Component {
        if (id.startsWith("effect:")) {
            val effectId = id.removePrefix("effect:")
            val location = runCatching { ResourceLocation.parse(effectId) }.getOrNull()
            val holder = location?.let { BuiltInRegistries.MOB_EFFECT.getHolder(it).orElse(null) }
            if (holder != null) return holder.value().displayName
            return Component.literal(effectId)
        }
        return Component.translatable("reallyusefulribbits.curse.$id")
    }

    private fun clock(seconds: Int): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val rest = seconds % 60
        return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, rest) else "%d:%02d".format(minutes, rest)
    }
}
