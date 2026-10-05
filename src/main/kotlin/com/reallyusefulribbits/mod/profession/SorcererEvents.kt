package com.reallyusefulribbits.mod.profession

import com.reallyusefulribbits.mod.logic.ProfessionKind
import com.reallyusefulribbits.mod.util.professionKind
import com.yungnickyoung.minecraft.ribbits.entity.RibbitEntity
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.damagesource.DamageTypes
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LightningBolt

object SorcererEvents {
    fun blocksDamage(entity: Entity, source: DamageSource): Boolean {
        val ribbit = entity as? RibbitEntity ?: return false
        if (ribbit.professionKind() != ProfessionKind.SORCERER) return false
        return source.`is`(DamageTypes.LIGHTNING_BOLT) ||
            source.entity is LightningBolt ||
            source.directEntity is LightningBolt ||
            source.`is`(DamageTypes.EXPLOSION) ||
            source.`is`(DamageTypes.PLAYER_EXPLOSION)
    }

    fun protectFromExplosion(affected: MutableList<Entity>) {
        affected.removeIf { entity ->
            val ribbit = entity as? RibbitEntity ?: return@removeIf false
            ribbit.professionKind() == ProfessionKind.SORCERER
        }
    }
}
