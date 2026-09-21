package com.reallyusefulribbits.mod.client

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object ClientVisuals {
    private val upsideDown = ConcurrentHashMap<UUID, Boolean>()

    fun set(id: UUID, value: Boolean) {
        if (value) upsideDown[id] = true else upsideDown.remove(id)
    }

    fun isUpsideDown(id: UUID): Boolean = upsideDown[id] == true
}
