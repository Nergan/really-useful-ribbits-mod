package com.reallyusefulribbits.mod.client

import net.minecraft.world.phys.Vec3

/** Небо «Удаляй мир». Цвет считается из ванильного, чтобы день и ночь всё ещё различались. */
object ClientDoom {
    @Volatile
    var active: Boolean = false

    fun sky(color: Vec3): Vec3 = Vec3(0.55 + color.x * 0.3, 0.03 + color.y * 0.08, 0.03 + color.z * 0.08)

    fun fog(color: Vec3): Vec3 = Vec3(0.35 + color.x * 0.3, 0.02 + color.y * 0.08, 0.02 + color.z * 0.08)

    fun clouds(color: Vec3): Vec3 = Vec3(0.6 + color.x * 0.3, 0.12 + color.y * 0.1, 0.1 + color.z * 0.1)
}
