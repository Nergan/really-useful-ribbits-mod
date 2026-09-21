package com.reallyusefulribbits.mod.profession

import com.reallyusefulribbits.mod.ReallyUsefulRibbitsMod
import com.reallyusefulribbits.mod.logic.ProfessionKind
import com.reallyusefulribbits.mod.util.professionKind
import com.reallyusefulribbits.mod.util.work
import com.yungnickyoung.minecraft.ribbits.entity.RibbitEntity
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.ai.attributes.AttributeModifier
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.ai.goal.TemptGoal
import net.minecraft.world.item.Items
import net.minecraft.world.item.crafting.Ingredient
import net.minecraft.world.level.pathfinder.PathType

object RibbitSafety {
    private val MERCHANT_SPEED = ResourceLocation.fromNamespaceAndPath(ReallyUsefulRibbitsMod.MOD_ID, "merchant_speed")

    fun ensure(ribbit: RibbitEntity) {
        val data = ribbit.work()
        if (data.goalsReady) return
        data.goalsReady = true
        ribbit.setPathfindingMalus(PathType.DAMAGE_FIRE, 16f)
        ribbit.setPathfindingMalus(PathType.DANGER_FIRE, 16f)
        ribbit.setPathfindingMalus(PathType.LAVA, 16f)
        ribbit.setPathfindingMalus(PathType.DAMAGE_OTHER, 8f)
        if (ribbit.professionKind() == ProfessionKind.MERCHANT) {
            val attr = ribbit.getAttribute(Attributes.MOVEMENT_SPEED)
            if (attr != null && attr.getModifier(MERCHANT_SPEED) == null) {
                attr.addPermanentModifier(
                    AttributeModifier(MERCHANT_SPEED, 0.25, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL),
                )
            }
        }
        if (ribbit.professionKind() == ProfessionKind.SORCERER) {
            ribbit.goalSelector.addGoal(
                3,
                TemptGoal(
                    ribbit,
                    1.15,
                    Ingredient.of(Items.AMETHYST_SHARD, Items.ENCHANTED_GOLDEN_APPLE),
                    false,
                ),
            )
        }
    }
}
