package com.reallyusefulribbits.mod.event

import com.reallyusefulribbits.mod.ReallyUsefulRibbitsMod
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer

object ModAdvancements {
    private fun id(path: String): ResourceLocation = ResourceLocation.fromNamespaceAndPath(ReallyUsefulRibbitsMod.MOD_ID, path)

    private val NEW_HOME = id("new_home")
    private val OVERDID_IT = id("overdid_it")
    private val FOMA = id("foma")
    private val GET_IT_OFF = id("get_it_off")
    private val GHOST = id("ghost")
    private val NO_MAGIC_TODAY = id("no_magic_today")
    private val HEXED = id("hexed")
    private val DELETE_WORLD = id("delete_world")
    private val AT_WHAT_COST = id("at_what_cost")
    private val VVVVVV = id("vvvvvv")
    private val FIRE_NATION = id("fire_nation")
    private val PERUN = id("perun")
    private val TRUE_FREEDOM = id("true_freedom")
    private val MUSYA = id("musya")
    private val HOW_TO_WALK = id("how_to_walk")
    private val WALLED_IN = id("walled_in")
    private val APOCALYPSE = id("apocalypse")
    private val SCRIPTURE = id("scripture")
    private val WORSE_THAN_LOSING = id("worse_than_losing")

    fun grantNewHome(player: ServerPlayer) = grant(player, NEW_HOME, "set_home")

    fun grantOverdidIt(player: ServerPlayer) = grant(player, OVERDID_IT, "summoned_dragon")

    fun grantFoma(player: ServerPlayer) = grant(player, FOMA, "summoned_foma")

    fun grantGetItOff(player: ServerPlayer) = grant(player, GET_IT_OFF, "used_apple")

    fun grantGhost(player: ServerPlayer) = grant(player, GHOST, "became_ghost")

    fun grantNoMagicToday(player: ServerPlayer) = grant(player, NO_MAGIC_TODAY, "boom")

    fun grantHexed(player: ServerPlayer) = grant(player, HEXED, "hexed")

    fun grantDeleteWorld(player: ServerPlayer) = grant(player, DELETE_WORLD, "summoned")

    fun grantAtWhatCost(player: ServerPlayer) = grant(player, AT_WHAT_COST, "defeated")

    fun grantVvvvvv(player: ServerPlayer) = grant(player, VVVVVV, "cursed")

    fun grantFireNation(player: ServerPlayer) = grant(player, FIRE_NATION, "cursed")

    fun grantPerun(player: ServerPlayer) = grant(player, PERUN, "cursed")

    fun grantTrueFreedom(player: ServerPlayer) = grant(player, TRUE_FREEDOM, "cursed")

    fun grantMusya(player: ServerPlayer) = grant(player, MUSYA, "summoned")

    fun grantHowToWalk(player: ServerPlayer) = grant(player, HOW_TO_WALK, "cursed")

    fun grantWalledIn(player: ServerPlayer) = grant(player, WALLED_IN, "cursed")

    fun grantApocalypse(player: ServerPlayer) = grant(player, APOCALYPSE, "cast")

    fun grantScripture(player: ServerPlayer) = grant(player, SCRIPTURE, "cast")

    fun grantWorseThanLosing(player: ServerPlayer) = grant(player, WORSE_THAN_LOSING, "cast")

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
