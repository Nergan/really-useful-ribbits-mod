package com.reallyusefulribbits.mod.profession

import com.reallyusefulribbits.mod.logic.ProfessionKind
import com.reallyusefulribbits.mod.util.professionKind
import com.yungnickyoung.minecraft.ribbits.entity.RibbitEntity
import net.minecraft.world.damagesource.DamageTypes
import net.minecraft.world.entity.LightningBolt
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent
import net.neoforged.neoforge.event.level.ExplosionEvent

object SorcererEvents {
    @SubscribeEvent
    fun onIncomingDamage(event: LivingIncomingDamageEvent) {
        val ribbit = event.entity as? RibbitEntity ?: return
        if (ribbit.professionKind() != ProfessionKind.SORCERER) return
        val source = event.source
        if (source.`is`(DamageTypes.LIGHTNING_BOLT) ||
            source.entity is LightningBolt ||
            source.directEntity is LightningBolt ||
            source.`is`(DamageTypes.EXPLOSION) ||
            source.`is`(DamageTypes.PLAYER_EXPLOSION)
        ) {
            event.isCanceled = true
        }
    }

    @SubscribeEvent
    fun onExplosion(event: ExplosionEvent.Detonate) {
        event.affectedEntities.removeIf { entity ->
            val ribbit = entity as? RibbitEntity ?: return@removeIf false
            ribbit.professionKind() == ProfessionKind.SORCERER
        }
    }
}
