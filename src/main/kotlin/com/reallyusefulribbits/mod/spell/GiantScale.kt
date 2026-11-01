package com.reallyusefulribbits.mod.spell

import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.ai.attributes.RangedAttribute

/** Гигантский варден и кот больше ванильного потолка размера и здоровья. */
object GiantScale {
    const val SCALE_CAP: Double = 128.0
    const val HEALTH_CAP: Double = 1_000_000.0

    /** Шире дракона: таким тушам не нужен перебор каждого блока в хитбоксе. */
    private const val GIANT_WIDTH: Float = 20f

    fun capFor(attribute: RangedAttribute): Double? = when (attribute.descriptionId) {
        "attribute.name.generic.scale" -> SCALE_CAP
        "attribute.name.generic.max_health" -> HEALTH_CAP
        else -> null
    }

    fun isGiant(entity: Entity): Boolean = entity is LivingEntity && entity.bbWidth > GIANT_WIDTH
}
