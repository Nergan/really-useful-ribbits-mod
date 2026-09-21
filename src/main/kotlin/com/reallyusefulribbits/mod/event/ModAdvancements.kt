package com.reallyusefulribbits.mod.event

import com.reallyusefulribbits.mod.ReallyUsefulRibbitsMod
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer

object ModAdvancements {
    private val NEW_HOME = ResourceLocation.fromNamespaceAndPath(ReallyUsefulRibbitsMod.MOD_ID, "new_home")
    private val OVERDID_IT = ResourceLocation.fromNamespaceAndPath(ReallyUsefulRibbitsMod.MOD_ID, "overdid_it")

    fun grantNewHome(player: ServerPlayer) = grant(player, NEW_HOME, "set_home")

    fun grantOverdidIt(player: ServerPlayer) = grant(player, OVERDID_IT, "summoned_dragon")

    private fun grant(player: ServerPlayer, advancementId: ResourceLocation, criterion: String) {
        try {
            val holder = player.server.advancements.get(advancementId)
            if (holder == null) {
                ReallyUsefulRibbitsMod.LOGGER.warn("Advancement {} is not loaded", advancementId)
                return
            }
            player.advancements.award(holder, criterion)
        } catch (t: Throwable) {
            ReallyUsefulRibbitsMod.LOGGER.warn("Could not grant advancement {}", advancementId, t)
        }
    }
}
