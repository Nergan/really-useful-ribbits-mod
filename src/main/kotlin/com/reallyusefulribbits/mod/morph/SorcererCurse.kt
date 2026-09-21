package com.reallyusefulribbits.mod.morph

import com.reallyusefulribbits.mod.attach.ModAttachments
import com.reallyusefulribbits.mod.attach.PlayerVisualData
import com.reallyusefulribbits.mod.event.PlayerTickHandler
import com.reallyusefulribbits.mod.network.PlayerVisualPayload
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.GameType
import net.neoforged.neoforge.network.PacketDistributor

object SorcererCurse {
    fun visual(player: Player): PlayerVisualData = player.getData(ModAttachments.VISUAL.get())

    fun sync(player: ServerPlayer) {
        val data = visual(player)
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(
            player,
            PlayerVisualPayload(player.uuid, data.upsideDown, data.morphId),
        )
    }

    fun rememberBase(player: ServerPlayer) {
        val data = visual(player)
        if (data.attributesSaved) return
        data.savedMaxHealth = player.getAttribute(Attributes.MAX_HEALTH)?.baseValue ?: 20.0
        data.savedScale = player.getAttribute(Attributes.SCALE)?.baseValue ?: 1.0
        data.savedSpeed = player.getAttribute(Attributes.MOVEMENT_SPEED)?.baseValue ?: 0.1
        data.savedAttack = player.getAttribute(Attributes.ATTACK_DAMAGE)?.baseValue ?: 1.0
        data.savedKnockback = player.getAttribute(Attributes.ATTACK_KNOCKBACK)?.baseValue ?: 0.0
        data.savedStep = player.getAttribute(Attributes.STEP_HEIGHT)?.baseValue ?: 0.6
        data.attributesSaved = true
    }

    fun applyMorph(player: ServerPlayer, type: net.minecraft.world.entity.EntityType<*>) {
        rememberBase(player)
        val dummy = type.create(player.serverLevel()) as? net.minecraft.world.entity.LivingEntity ?: return
        copyLiveAttribute(player, Attributes.MAX_HEALTH, dummy.getAttribute(Attributes.MAX_HEALTH)?.baseValue)
        copyLiveAttribute(player, Attributes.ATTACK_DAMAGE, dummy.getAttribute(Attributes.ATTACK_DAMAGE)?.baseValue)
        copyLiveAttribute(player, Attributes.ATTACK_KNOCKBACK, dummy.getAttribute(Attributes.ATTACK_KNOCKBACK)?.baseValue)
        copyLiveAttribute(player, Attributes.STEP_HEIGHT, dummy.getAttribute(Attributes.STEP_HEIGHT)?.baseValue)
        dummy.discard()
        if (player.health > player.maxHealth) player.health = player.maxHealth
        val data = visual(player)
        data.morphId = BuiltInRegistries.ENTITY_TYPE.getKey(type).toString()
        data.morphFlight = PlayerMorph.canFly(player.serverLevel(), type)
        if (data.morphFlight) {
            player.abilities.mayfly = true
            player.onUpdateAbilities()
        }
        player.refreshDimensions()
        sync(player)
    }

    fun clear(player: ServerPlayer) {
        val data = visual(player)
        player.removeAllEffects()
        data.upsideDown = false
        restoreAttribute(player, Attributes.MAX_HEALTH, data.savedMaxHealth, 20.0)
        restoreAttribute(player, Attributes.SCALE, data.savedScale, 1.0)
        restoreAttribute(player, Attributes.MOVEMENT_SPEED, data.savedSpeed, 0.1)
        restoreAttribute(player, Attributes.ATTACK_DAMAGE, data.savedAttack, 1.0)
        restoreAttribute(player, Attributes.ATTACK_KNOCKBACK, data.savedKnockback, 0.0)
        restoreAttribute(player, Attributes.STEP_HEIGHT, data.savedStep, 0.6)
        if (player.health > player.maxHealth) player.health = player.maxHealth
        data.morphId = ""
        data.morphFlight = false
        data.attributesSaved = false
        data.flightTicks = 0
        if (data.spectatorTicks > 0 || player.gameMode.gameModeForPlayer == GameType.SPECTATOR) {
            val mode = GameType.byName(data.previousGameMode) ?: GameType.SURVIVAL
            if (!player.isCreative) player.setGameMode(mode)
        }
        data.spectatorTicks = 0
        PlayerTickHandler.revokeTemporaryFlight(player)
        player.refreshDimensions()
        sync(player)
    }

    fun hasCurse(player: Player): Boolean {
        val data = visual(player)
        return data.upsideDown ||
            data.flightTicks > 0 ||
            data.morphId.isNotBlank() ||
            data.spectatorTicks > 0 ||
            data.attributesSaved
    }

    private fun copyLiveAttribute(
        player: ServerPlayer,
        attribute: net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute>,
        value: Double?,
    ) {
        if (value == null || value <= 0.0) return
        player.getAttribute(attribute)?.baseValue = value
    }

    private fun restoreAttribute(
        player: ServerPlayer,
        attribute: net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute>,
        saved: Double,
        fallback: Double,
    ) {
        val attr = player.getAttribute(attribute) ?: return
        attr.baseValue = if (saved > 0.0) saved else fallback
    }
}
