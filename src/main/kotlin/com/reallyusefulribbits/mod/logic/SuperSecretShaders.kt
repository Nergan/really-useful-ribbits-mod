package com.reallyusefulribbits.mod.logic

/**
 * Эффекты бывшей кнопки Super Secret Settings. Четыре шейдера ещё лежат в игре,
 * остальные собраны этим модом из её программ и своих фрагментных шейдеров.
 */
object SuperSecretShaders {
    val SHADERS: List<String> = listOf(
        "minecraft:shaders/post/invert.json",
        "minecraft:shaders/post/creeper.json",
        "minecraft:shaders/post/spider.json",
        "minecraft:shaders/post/blur.json",
        "reallyusefulribbits:shaders/post/bits.json",
        "reallyusefulribbits:shaders/post/desaturate.json",
        "reallyusefulribbits:shaders/post/green.json",
        "reallyusefulribbits:shaders/post/sepia.json",
        "reallyusefulribbits:shaders/post/blobs.json",
        "reallyusefulribbits:shaders/post/flip.json",
        "reallyusefulribbits:shaders/post/wobble.json",
        "reallyusefulribbits:shaders/post/deconverge.json",
    )

    fun pick(roll: Int, current: String): String {
        val choices = SHADERS.filter { it != current }
        return choices[Math.floorMod(roll, choices.size)]
    }
}
