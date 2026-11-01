package com.reallyusefulribbits.mod.attach

/** Fabric хранит облик игрока в mixin. NeoForge этим интерфейсом не пользуется. */
interface VisualCarrier {
    fun `reallyusefulribbits$visual`(): PlayerVisualData

    fun `reallyusefulribbits$setVisual`(data: PlayerVisualData)
}
