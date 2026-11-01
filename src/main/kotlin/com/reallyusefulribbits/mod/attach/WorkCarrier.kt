package com.reallyusefulribbits.mod.attach

/** Fabric хранит работу риббита в mixin. NeoForge этим интерфейсом не пользуется. */
interface WorkCarrier {
    fun `reallyusefulribbits$work`(): RibbitWorkData

    fun `reallyusefulribbits$setWork`(data: RibbitWorkData)
}
