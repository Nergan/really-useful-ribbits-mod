package com.reallyusefulribbits.mod.logic

/**
 * Сколько секунд держится состояние заклинания. -1 — пока его не снимут.
 * Комната из бедрока и час наблюдателя сохраняют прежнее время, остальные по умолчанию бесконечны.
 */
object SpellDurations {
    const val FOREVER = -1
    const val MAX_SECONDS = 60 * 60 * 24 * 7

    data class State(val id: String, val defaultSeconds: Int)

    val STATES: List<State> = listOf(
        State("effect_or_cleanse", FOREVER),
        State("flip", FOREVER),
        State("scale", FOREVER),
        State("max_health", FOREVER),
        State("morph", FOREVER),
        State("spectator", 60 * 60),
        State("creative_flight", FOREVER),
        State("super_secret_settings", FOREVER),
        State("inverted_controls", FOREVER),
        State("vvvvvv", FOREVER),
        State("nickname", FOREVER),
        State("fireballs", FOREVER),
        State("lightning_hands", FOREVER),
        State("bedrock_room", 60 * 5),
    )

    fun defaults(): Map<String, Int> = STATES.associate { it.id to it.defaultSeconds }

    fun clamp(seconds: Int): Int = if (seconds < 0) FOREVER else seconds.coerceAtMost(MAX_SECONDS)

    fun seconds(id: String, stored: Map<String, Int> = emptyMap()): Int =
        clamp(stored[id] ?: STATES.firstOrNull { it.id == id }?.defaultSeconds ?: FOREVER)

    fun ticks(seconds: Int): Int = if (seconds < 0) -1 else seconds * 20
}
