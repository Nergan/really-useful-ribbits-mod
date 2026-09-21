package com.reallyusefulribbits.mod.event

import com.reallyusefulribbits.mod.ReallyUsefulRibbitsMod
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer

object ModAdvancements {
    private val NEW_HOME = ResourceLocation.fromNamespaceAndPath(ReallyUsefulRibbitsMod.MOD_ID, "new_home")
    private val OVERDID_IT = ResourceLocation.fromNamespaceAndPath(ReallyUsefulRibbitsMod.MOD_ID, "overdid_it")
    private val FOMA = ResourceLocation.fromNamespaceAndPath(ReallyUsefulRibbitsMod.MOD_ID, "foma")
    private val GET_IT_OFF = ResourceLocation.fromNamespaceAndPath(ReallyUsefulRibbitsMod.MOD_ID, "get_it_off")
    private val GHOST = ResourceLocation.fromNamespaceAndPath(ReallyUsefulRibbitsMod.MOD_ID, "ghost")
    private val NO_MAGIC_TODAY = ResourceLocation.fromNamespaceAndPath(ReallyUsefulRibbitsMod.MOD_ID, "no_magic_today")
    private val HEXED = ResourceLocation.fromNamespaceAndPath(ReallyUsefulRibbitsMod.MOD_ID, "hexed")

    fun grantNewHome(player: ServerPlayer) = grant(player, NEW_HOME, "set_home")

    fun grantOverdidIt(player: ServerPlayer) = grant(player, OVERDID_IT, "summoned_dragon")

    fun grantFoma(player: ServerPlayer) = grant(player, FOMA, "summoned_foma")

    fun grantGetItOff(player: ServerPlayer) = grant(player, GET_IT_OFF, "used_apple")

    fun grantGhost(player: ServerPlayer) = grant(player, GHOST, "became_ghost")

    fun grantNoMagicToday(player: ServerPlayer) = grant(player, NO_MAGIC_TODAY, "boom")

    fun grantHexed(player: ServerPlayer) = grant(player, HEXED, "hexed")

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
