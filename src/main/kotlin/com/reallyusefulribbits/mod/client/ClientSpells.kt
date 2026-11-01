package com.reallyusefulribbits.mod.client

import com.reallyusefulribbits.mod.loader.RuntimeHooks
import com.reallyusefulribbits.mod.morph.SorcererCurse
import com.reallyusefulribbits.mod.network.HandCastPayload
import net.minecraft.client.Minecraft
import net.minecraft.client.player.LocalPlayer
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.phys.HitResult
import kotlin.math.abs

/** Клиентская сторона проклятий: клавиши VVVVVV и пустой руки, шейдер, перевёрнутое управление. */
object ClientSpells {
    private const val FLIP_COOLDOWN = 8
    private const val HAND_COOLDOWN = 10
    private const val DOUBLE_TAP = 7

    private var jumpWasDown = false
    private var useWasDown = false

    /** Свой счётчик: `tickCount` игрока обнуляется после смерти и смены измерения. */
    private var ticks = 0
    private var lastFlip = -100
    private var lastHand = -100
    private var pendingFlipAt = -1

    private fun localFlags(): Int {
        val player = Minecraft.getInstance().player ?: return 0
        return ClientVisuals.flags(player.uuid)
    }

    fun controlsInverted(): Boolean = localFlags() and SorcererCurse.FLAG_INVERTED != 0

    fun wantedShader(): String {
        val player = Minecraft.getInstance().player ?: return ""
        return ClientVisuals.shader(player.uuid)
    }

    fun startTick(minecraft: Minecraft) {
        val player = minecraft.player
        if (player == null || minecraft.level == null) {
            ClientDoom.active = false
            ClientBeams.clear()
            jumpWasDown = false
            useWasDown = false
            pendingFlipAt = -1
            return
        }
        ticks++
        val flags = ClientVisuals.flags(player.uuid)
        val free = minecraft.screen == null
        val jump = minecraft.options.keyJump
        val jumpDown = jump.isDown
        val vvvvvv = flags and SorcererCurse.FLAG_VVVVVV != 0
        if (vvvvvv && free && canFlip(player)) {
            if (jumpDown && !jumpWasDown) {
                // Тому, кто умеет летать, двойное нажатие включает полёт, поэтому переворот ждёт второго.
                when {
                    !player.abilities.mayfly -> flip(player)
                    pendingFlipAt >= 0 -> {
                        pendingFlipAt = -1
                        player.abilities.flying = true
                        player.onUpdateAbilities()
                    }
                    else -> pendingFlipAt = ticks + DOUBLE_TAP
                }
            }
            if (jumpDown) jump.setDown(false)
        }
        if (pendingFlipAt in 0..ticks) {
            pendingFlipAt = -1
            if (vvvvvv && canFlip(player)) flip(player)
        }
        jumpWasDown = jumpDown
        val useDown = minecraft.options.keyUse.isDown
        val hands = flags and (SorcererCurse.FLAG_FIREBALLS or SorcererCurse.FLAG_LIGHTNING) != 0
        if (hands && free && useDown && !useWasDown && player.mainHandItem.isEmpty) {
            val hit = minecraft.hitResult
            if ((hit == null || hit.type == HitResult.Type.MISS) && ticks - lastHand >= HAND_COOLDOWN) {
                lastHand = ticks
                player.swing(InteractionHand.MAIN_HAND)
                RuntimeHooks.sendToServer(HandCastPayload(HandCastPayload.HAND))
            }
        }
        useWasDown = useDown
    }

    fun endTick(minecraft: Minecraft) {
        ClientBeams.tick()
        val player = minecraft.player ?: return
        if (player.getAttributeValue(Attributes.GRAVITY) >= 0.0 || player.abilities.flying) return
        if (player.onGround() || !player.verticalCollision) return
        if (!player.level().noCollision(player, player.boundingBox.move(0.0, 0.05, 0.0))) player.setOnGround(true)
    }

    /** В воде, верхом и в полёте прыжок остаётся прыжком. */
    private fun canFlip(player: LocalPlayer): Boolean =
        !player.isInWater && !player.isInLava && !player.isPassenger && !player.abilities.flying && !player.isSleeping

    private fun flip(player: LocalPlayer) {
        if (ticks - lastFlip < FLIP_COOLDOWN) return
        lastFlip = ticks
        predictFlip(player)
        RuntimeHooks.sendToServer(HandCastPayload(HandCastPayload.FLIP))
    }

    private fun predictFlip(player: LocalPlayer) {
        val gravity = player.getAttribute(Attributes.GRAVITY) ?: return
        val magnitude = abs(gravity.baseValue).takeIf { it > 1.0e-4 } ?: LivingEntity.DEFAULT_BASE_GRAVITY
        gravity.baseValue = if (gravity.baseValue >= 0.0) -magnitude else magnitude
    }
}
