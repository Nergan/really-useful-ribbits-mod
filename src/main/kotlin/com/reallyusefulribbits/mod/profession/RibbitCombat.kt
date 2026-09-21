package com.reallyusefulribbits.mod.profession

import com.reallyusefulribbits.mod.ReallyUsefulRibbitsMod
import com.reallyusefulribbits.mod.config.ModConfig
import com.reallyusefulribbits.mod.logic.ProfessionKind
import com.reallyusefulribbits.mod.util.professionKind
import com.reallyusefulribbits.mod.util.work
import com.yungnickyoung.minecraft.ribbits.entity.RibbitEntity
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.ai.attributes.AttributeModifier
import net.minecraft.world.entity.ai.attributes.Attributes
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent

object RibbitCombat {
    private val FLEE_SPEED = ResourceLocation.fromNamespaceAndPath(ReallyUsefulRibbitsMod.MOD_ID, "flee_speed")

    @SubscribeEvent
    fun onHurt(event: LivingIncomingDamageEvent) {
        val ribbit = event.entity as? RibbitEntity ?: return
        if (event.amount <= 0f) return
        val attacker = event.source.entity as? LivingEntity
        val data = ribbit.work()
        data.fleeTicks = ModConfig.FLEE_TICKS
        if (attacker != null) {
            data.fleeX = attacker.x
            data.fleeZ = attacker.z
            if (ribbit.professionKind() == ProfessionKind.SORCERER) {
                attacker.addEffect(MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20 * 60, 1, false, true))
            }
        } else {
            data.fleeX = ribbit.x
            data.fleeZ = ribbit.z
        }
        applyFleeSpeed(ribbit, true)
    }

    fun tickFlee(ribbit: RibbitEntity): Boolean {
        val data = ribbit.work()
        if (data.fleeTicks <= 0) {
            applyFleeSpeed(ribbit, false)
            return false
        }
        data.fleeTicks--
        val dx = ribbit.x - data.fleeX
        val dz = ribbit.z - data.fleeZ
        val len = kotlin.math.sqrt(dx * dx + dz * dz)
        val nx = if (len < 0.01) 8.0 else dx / len * 10.0
        val nz = if (len < 0.01) 0.0 else dz / len * 10.0
        ribbit.setFishing(false)
        ribbit.setWatering(false)
        ribbit.navigation.moveTo(ribbit.x + nx, ribbit.y, ribbit.z + nz, 1.0)
        if (data.fleeTicks <= 0) applyFleeSpeed(ribbit, false)
        return true
    }

    private fun applyFleeSpeed(ribbit: RibbitEntity, on: Boolean) {
        val attr = ribbit.getAttribute(Attributes.MOVEMENT_SPEED) ?: return
        attr.removeModifier(FLEE_SPEED)
        if (on) {
            attr.addTransientModifier(
                AttributeModifier(FLEE_SPEED, 0.5, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL),
            )
        }
    }
}
