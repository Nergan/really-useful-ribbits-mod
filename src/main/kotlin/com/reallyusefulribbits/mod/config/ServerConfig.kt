package com.reallyusefulribbits.mod.config

import com.reallyusefulribbits.mod.logic.ScanRadius
import net.neoforged.neoforge.common.ModConfigSpec

/**
 * Конфиг типа SERVER: в мультиплеере значения задаёт сервер и рассылает клиентам.
 * Экран: Mods → Really Useful Ribbits → Config.
 * Ключи локализации: `<modid>.configuration.<path>`.
 */
class ServerConfig(builder: ModConfigSpec.Builder) {

    companion object {
        private const val KEY_PREFIX = "reallyusefulribbits.configuration"

        val SPEC: ModConfigSpec
        val CONFIG: ServerConfig

        init {
            val pair = ModConfigSpec.Builder().configure(::ServerConfig)
            CONFIG = pair.getLeft()
            SPEC = pair.getRight()
        }

        fun scanRadius(): Int = ScanRadius.clamp(CONFIG.scanRadius.get())

        fun chaosLevel(): Int = CONFIG.chaosLevel.get().coerceIn(0, 100)
    }

    val scanRadius: ModConfigSpec.IntValue
    val chaosLevel: ModConfigSpec.IntValue

    init {
        builder.push("work")
        scanRadius = builder
            .comment(
                "How far a ribbit looks for water, farms, containers, luck and sorcerer plant growth.",
                "Server-only: dedicated servers use world/serverconfig/reallyusefulribbits-server.toml.",
            )
            .translation("$KEY_PREFIX.work.scan_radius")
            .defineInRange("scan_radius", ScanRadius.DEFAULT, ScanRadius.MIN, ScanRadius.MAX)
        builder.pop()

        builder.push("sorcerer")
        chaosLevel = builder
            .comment(
                "How even sorcerer effect odds become. 0 keeps the default weighted table;",
                "100 makes every effect almost equally likely.",
            )
            .translation("$KEY_PREFIX.sorcerer.chaos_level")
            .defineInRange("chaos_level", 0, 0, 100)
        builder.pop()
    }
}
