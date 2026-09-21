package com.reallyusefulribbits.mod.profession

import com.reallyusefulribbits.mod.config.ModConfig
import com.reallyusefulribbits.mod.config.ServerConfig
import com.yungnickyoung.minecraft.ribbits.entity.RibbitEntity
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.LivingEntity

object NitwitAi {
    fun tick(level: ServerLevel, ribbit: RibbitEntity) {
        if (level.gameTime % ModConfig.NITWIT_LUCK_INTERVAL != 0L) return
        val radius = ServerConfig.scanRadius().toDouble()
        val box = ribbit.boundingBox.inflate(radius)
        for (entity in level.getEntitiesOfClass(LivingEntity::class.java, box)) {
            entity.addEffect(
                MobEffectInstance(MobEffects.LUCK, ModConfig.NITWIT_LUCK_DURATION, 0, true, true),
            )
        }
    }
}
