package com.reallyusefulribbits.mod.client

import net.minecraft.world.phys.Vec3

/** Молнии из руки, которые сейчас видны. Рисует их [BeamRenderer]. */
object ClientBeams {
    const val LIFETIME: Int = 10

    class Beam(val from: Vec3, val to: Vec3, val seed: Long) {
        var age: Int = 0
    }

    private val beams = ArrayList<Beam>()

    fun add(from: Vec3, to: Vec3, seed: Long) {
        synchronized(beams) { beams += Beam(from, to, seed) }
    }

    fun tick() {
        synchronized(beams) {
            beams.forEach { it.age++ }
            beams.removeIf { it.age >= LIFETIME }
        }
    }

    fun snapshot(): List<Beam> = synchronized(beams) { beams.toList() }

    fun clear() {
        synchronized(beams) { beams.clear() }
    }
}
