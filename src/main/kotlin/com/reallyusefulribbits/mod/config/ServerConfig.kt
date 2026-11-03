package com.reallyusefulribbits.mod.config

import com.reallyusefulribbits.mod.logic.ScanRadius
import com.reallyusefulribbits.mod.logic.SorcererAction
import com.reallyusefulribbits.mod.logic.SorcererTable
import com.reallyusefulribbits.mod.logic.SpellDurations
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

        fun spellPoints(action: SorcererAction): Int =
            CONFIG.spellPoints[action]?.get()?.coerceIn(0, SorcererTable.MAX_POINTS) ?: action.defaultPoints

        fun spellSeconds(id: String): Int =
            SpellDurations.clamp(CONFIG.spellDurations[id]?.get() ?: SpellDurations.seconds(id))
    }

    val scanRadius: ModConfigSpec.IntValue
    val chaosLevel: ModConfigSpec.IntValue
    val spellPoints: Map<SorcererAction, ModConfigSpec.IntValue>
    val spellDurations: Map<String, ModConfigSpec.IntValue>

    init {
        builder.push("work")
        scanRadius = builder
            .comment(
                "How far a ribbit looks for water, farms, containers, luck and sorcerer plant growth.",
                "The file is config/reallyusefulribbits-server.toml; a copy in <world>/serverconfig/ overrides it for one world.",
            )
            .translation("$KEY_PREFIX.work.scan_radius")
            .defineInRange("scan_radius", ScanRadius.DEFAULT, ScanRadius.MIN, ScanRadius.MAX)
        builder.pop()

        builder.push("sorcerer")
        chaosLevel = builder
            .comment(
                "How even sorcerer spell odds become. 0 keeps the spell points as they are;",
                "100 makes every enabled spell equally likely.",
            )
            .translation("$KEY_PREFIX.sorcerer.chaos_level")
            .defineInRange("chaos_level", SorcererTable.DEFAULT_CHAOS, 0, 100)
        builder
            .comment(
                "Spell points. A spell's chance is its points divided by the sum of all points.",
                "0 turns a spell off. The defaults add up to ${SorcererTable.DEFAULT_TOTAL}; with them 1 point is a 0.00001% chance.",
            )
            .translation("$KEY_PREFIX.sorcerer.spells")
            .push("spells")
        spellPoints = SorcererAction.entries.associateWith { action ->
            builder
                .translation("$KEY_PREFIX.sorcerer.spells.${action.id}")
                .defineInRange(action.id, action.defaultPoints, 0, SorcererTable.MAX_POINTS)
        }
        builder.pop()
        builder
            .comment(
                "How many seconds a sorcerer state lasts.",
                "-1 lasts until something removes it. Spectator defaults to one hour, the bedrock room to five minutes.",
            )
            .translation("$KEY_PREFIX.sorcerer.durations")
            .push("durations")
        spellDurations = SpellDurations.STATES.associate { state ->
            state.id to builder
                .translation("$KEY_PREFIX.sorcerer.durations.${state.id}")
                .defineInRange(state.id, state.defaultSeconds, SpellDurations.FOREVER, SpellDurations.MAX_SECONDS)
        }
        builder.pop()
        builder.pop()
    }
}
