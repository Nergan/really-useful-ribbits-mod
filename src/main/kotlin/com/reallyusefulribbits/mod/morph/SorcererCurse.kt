package com.reallyusefulribbits.mod.morph

import com.reallyusefulribbits.mod.attach.PlayerVisualData
import com.reallyusefulribbits.mod.event.PlayerTickHandler
import com.reallyusefulribbits.mod.loader.RuntimeHooks
import com.reallyusefulribbits.mod.network.PlayerVisualPayload
import net.minecraft.ChatFormatting
import net.minecraft.core.Holder
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.effect.MobEffect
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.GameType

/**
 * Проклятия чародея держатся и после смерти. Снимает их зачарованное золотое яблоко,
 * отданное чародею. Обычные эффекты зелий снимает ещё и молоко.
 */
object SorcererCurse {
    const val FLAG_FIREBALLS = 1
    const val FLAG_LIGHTNING = 2
    const val FLAG_VVVVVV = 4
    const val FLAG_INVERTED = 8

    fun visual(player: Player): PlayerVisualData = RuntimeHooks.visual(player)

    fun flags(data: PlayerVisualData): Int {
        var flags = 0
        if (data.fireballs) flags = flags or FLAG_FIREBALLS
        if (data.lightningHands) flags = flags or FLAG_LIGHTNING
        if (data.vvvvvv) flags = flags or FLAG_VVVVVV
        if (data.invertedControls) flags = flags or FLAG_INVERTED
        return flags
    }

    fun payload(player: Player): PlayerVisualPayload {
        val data = visual(player)
        return PlayerVisualPayload(
            playerId = player.uuid,
            upsideDown = data.upsideDown,
            morphId = data.morphId,
            flags = flags(data),
            nickname = data.nickname,
            nickColor = data.nickColor,
            shader = data.shader,
            timers = if (player is ServerPlayer) CurseClock.describe(player) else "",
        )
    }

    fun sync(player: ServerPlayer) {
        RuntimeHooks.sendTrackingAndSelf(player, payload(player))
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
        data.savedGravity = player.getAttribute(Attributes.GRAVITY)?.baseValue?.let { kotlin.math.abs(it) }
            ?: LivingEntity.DEFAULT_BASE_GRAVITY
        data.attributesSaved = true
    }

    fun applyMorph(player: ServerPlayer, type: net.minecraft.world.entity.EntityType<*>) {
        rememberBase(player)
        val dummy = type.create(player.serverLevel()) as? LivingEntity ?: return
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
        CurseClock.arm(player, "morph")
        sync(player)
    }

    /** Бесконечный эффект, который вернётся после смерти, пока его не снимут молоком или яблоком. */
    fun addEffect(player: ServerPlayer, effect: Holder<MobEffect>, amplifier: Int) {
        val duration = CurseClock.ticks("effect_or_cleanse")
        player.addEffect(MobEffectInstance(effect, duration, amplifier, false, true))
        val id = effect.unwrapKey().map { it.location().toString() }.orElse("") ?: ""
        if (id.isEmpty()) return
        val data = visual(player)
        data.curseEffects.removeIf { it.substringBefore('|') == id }
        data.curseEffects += "$id|$amplifier|$duration"
    }

    fun forgetEffects(player: Player) {
        visual(player).curseEffects.clear()
    }

    /** Эффект, который игрок снял сам (молоко, тотем), больше не возвращается. */
    fun pruneEffects(player: ServerPlayer) {
        if (!player.isAlive) return
        val data = visual(player)
        if (data.curseEffects.isEmpty()) return
        data.curseEffects.removeIf { entry ->
            val holder = effectHolder(entry.substringBefore('|')) ?: return@removeIf true
            !player.hasEffect(holder)
        }
    }

    /** После возрождения: эффекты, полёт и облик возвращаются, а VVVVVV снова тянет вниз. Атрибуты игра переносит сама. */
    fun afterRespawn(player: ServerPlayer) {
        val data = visual(player)
        player.getAttribute(Attributes.GRAVITY)?.let { it.baseValue = kotlin.math.abs(it.baseValue) }
        for (entry in data.curseEffects.toList()) {
            CurseClock.applyStoredEffect(player, entry)
        }
        if ((data.foreverFlight || data.morphFlight || data.flightTicks > 0) && !player.isCreative && !player.isSpectator) {
            player.abilities.mayfly = true
            player.onUpdateAbilities()
        }
        player.refreshDimensions()
        sync(player)
        if (data.nickname.isNotBlank()) broadcastName(player)
    }

    fun setNickname(player: ServerPlayer, nickname: String, color: Int) {
        val data = visual(player)
        data.nickname = nickname
        data.nickColor = color
        sync(player)
        broadcastName(player)
    }

    fun nickname(player: Player): Component? {
        val name: String
        val color: Int
        if (player.level().isClientSide) {
            val entry = com.reallyusefulribbits.mod.client.ClientVisuals.nickname(player.uuid) ?: return null
            name = entry.nickname
            color = entry.nickColor
        } else {
            val data = visual(player)
            if (data.nickname.isBlank()) return null
            name = data.nickname
            color = data.nickColor
        }
        val text = Component.literal(name)
        val format = ChatFormatting.getById(color)
        return if (format != null && format.isColor) text.withStyle(format) else text
    }

    private fun broadcastName(player: ServerPlayer) {
        player.server.playerList.broadcastAll(
            ClientboundPlayerInfoUpdatePacket(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME, player),
        )
    }

    fun clear(player: ServerPlayer) {
        val data = visual(player)
        player.removeAllEffects()
        data.curseEffects.clear()
        data.upsideDown = false
        restoreAttribute(player, Attributes.MAX_HEALTH, data.savedMaxHealth, 20.0)
        restoreAttribute(player, Attributes.SCALE, data.savedScale, 1.0)
        restoreAttribute(player, Attributes.MOVEMENT_SPEED, data.savedSpeed, 0.1)
        restoreAttribute(player, Attributes.ATTACK_DAMAGE, data.savedAttack, 1.0)
        restoreAttribute(player, Attributes.ATTACK_KNOCKBACK, data.savedKnockback, 0.0)
        restoreAttribute(player, Attributes.STEP_HEIGHT, data.savedStep, 0.6)
        player.getAttribute(Attributes.GRAVITY)?.let { gravity ->
            gravity.baseValue = kotlin.math.abs(if (data.attributesSaved) data.savedGravity else gravity.baseValue)
                .takeIf { it > 1.0e-4 } ?: LivingEntity.DEFAULT_BASE_GRAVITY
        }
        if (player.health > player.maxHealth) player.health = player.maxHealth
        data.morphId = ""
        data.morphFlight = false
        data.attributesSaved = false
        data.flightTicks = 0
        data.vvvvvv = false
        data.fireballs = false
        data.lightningHands = false
        data.foreverFlight = false
        data.invertedControls = false
        data.shader = ""
        val hadNickname = data.nickname.isNotBlank()
        data.nickname = ""
        data.nickColor = -1
        if (data.spectatorTicks > 0 || player.gameMode.gameModeForPlayer == GameType.SPECTATOR) {
            val mode = GameType.byName(data.previousGameMode) ?: GameType.SURVIVAL
            if (!player.isCreative) player.setGameMode(mode)
        }
        data.spectatorTicks = 0
        data.until.clear()
        PlayerTickHandler.revokeTemporaryFlight(player)
        player.refreshDimensions()
        sync(player)
        if (hadNickname) broadcastName(player)
    }

    fun expire(player: ServerPlayer, id: String) {
        val data = visual(player)
        when (id) {
            "flip" -> data.upsideDown = false
            "scale" -> restoreAttribute(player, Attributes.SCALE, data.savedScale, 1.0)
            "max_health" -> {
                restoreAttribute(player, Attributes.MAX_HEALTH, data.savedMaxHealth, 20.0)
                if (player.health > player.maxHealth) player.health = player.maxHealth
            }
            "morph" -> {
                data.morphId = ""
                data.morphFlight = false
                restoreAttribute(player, Attributes.ATTACK_DAMAGE, data.savedAttack, 1.0)
                restoreAttribute(player, Attributes.ATTACK_KNOCKBACK, data.savedKnockback, 0.0)
                restoreAttribute(player, Attributes.STEP_HEIGHT, data.savedStep, 0.6)
                if (!data.until.containsKey("max_health")) {
                    restoreAttribute(player, Attributes.MAX_HEALTH, data.savedMaxHealth, 20.0)
                }
                if (!data.until.containsKey("scale")) restoreAttribute(player, Attributes.SCALE, data.savedScale, 1.0)
                if (player.health > player.maxHealth) player.health = player.maxHealth
                player.refreshDimensions()
            }
            "creative_flight" -> {
                data.foreverFlight = false
                PlayerTickHandler.revokeTemporaryFlight(player)
            }
            "super_secret_settings" -> data.shader = ""
            "inverted_controls" -> data.invertedControls = false
            "vvvvvv" -> {
                data.vvvvvv = false
                player.getAttribute(Attributes.GRAVITY)?.let { gravity ->
                    gravity.baseValue = kotlin.math.abs(if (data.attributesSaved) data.savedGravity else gravity.baseValue)
                        .takeIf { it > 1.0e-4 } ?: LivingEntity.DEFAULT_BASE_GRAVITY
                }
            }
            "nickname" -> {
                val had = data.nickname.isNotBlank()
                data.nickname = ""
                data.nickColor = -1
                if (had) broadcastName(player)
            }
            "fireballs" -> data.fireballs = false
            "lightning_hands" -> data.lightningHands = false
            "bedrock_room" -> Unit
        }
        sync(player)
    }

    fun hasCurse(player: Player): Boolean {
        val data = visual(player)
        return data.upsideDown ||
            data.flightTicks > 0 ||
            data.morphId.isNotBlank() ||
            data.spectatorTicks != 0 ||
            data.attributesSaved ||
            data.curseEffects.isNotEmpty() ||
            data.vvvvvv ||
            data.fireballs ||
            data.lightningHands ||
            data.foreverFlight ||
            data.invertedControls ||
            data.shader.isNotBlank() ||
            data.nickname.isNotBlank()
    }

    private fun effectHolder(id: String): Holder<MobEffect>? {
        val location = ResourceLocation.tryParse(id) ?: return null
        return BuiltInRegistries.MOB_EFFECT.getHolder(location).orElse(null)
    }

    private fun copyLiveAttribute(
        player: ServerPlayer,
        attribute: Holder<net.minecraft.world.entity.ai.attributes.Attribute>,
        value: Double?,
    ) {
        if (value == null || value <= 0.0) return
        player.getAttribute(attribute)?.baseValue = value
    }

    private fun restoreAttribute(
        player: ServerPlayer,
        attribute: Holder<net.minecraft.world.entity.ai.attributes.Attribute>,
        saved: Double,
        fallback: Double,
    ) {
        val attr = player.getAttribute(attribute) ?: return
        attr.baseValue = if (saved > 0.0) saved else fallback
    }
}
