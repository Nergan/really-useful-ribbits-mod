package com.reallyusefulribbits.mod.profession

import com.reallyusefulribbits.mod.logic.ProfessionKind
import com.reallyusefulribbits.mod.util.professionKind
import com.reallyusefulribbits.mod.util.work
import com.yungnickyoung.minecraft.ribbits.entity.RibbitEntity
import net.minecraft.world.entity.ai.goal.TemptGoal
import net.minecraft.world.item.Items
import net.minecraft.world.item.crafting.Ingredient
import net.minecraft.world.level.pathfinder.PathType

object RibbitSafety {
    fun ensure(ribbit: RibbitEntity) {
        val data = ribbit.work()
        if (data.goalsReady) return
        data.goalsReady = true
        ribbit.setPathfindingMalus(PathType.DAMAGE_FIRE, 16f)
        ribbit.setPathfindingMalus(PathType.DANGER_FIRE, 16f)
        ribbit.setPathfindingMalus(PathType.LAVA, 16f)
        ribbit.setPathfindingMalus(PathType.DAMAGE_OTHER, 8f)
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
