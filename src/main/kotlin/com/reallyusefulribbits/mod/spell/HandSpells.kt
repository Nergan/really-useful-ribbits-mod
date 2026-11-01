package com.reallyusefulribbits.mod.spell

import com.reallyusefulribbits.mod.loader.RuntimeHooks
import com.reallyusefulribbits.mod.network.HandCastPayload
import com.reallyusefulribbits.mod.network.LightningBeamPayload
import net.minecraft.core.BlockPos
import net.minecraft.core.registries.Registries
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.damagesource.DamageTypes
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.projectile.LargeFireball
import net.minecraft.world.level.ClipContext
import net.minecraft.world.phys.AABB
import java.util.UUID
import kotlin.math.abs

/** Пустая рука: огненный шар гаста, молния до края прогрузки, переворот гравитации VVVVVV. */
object HandSpells {
    private const val HAND_COOLDOWN = 10L
    private const val FLIP_COOLDOWN = 6L
    private const val BEAM_DAMAGE = 8f
    private const val SKY_MARGIN = 64

    private val lastHand = HashMap<UUID, Long>()
    private val lastFlip = HashMap<UUID, Long>()
    private val alternate = HashMap<UUID, Boolean>()

    fun handle(player: ServerPlayer, kind: Int) {
        if (!player.isAlive || player.isSpectator) return
        val data = RuntimeHooks.visual(player)
        val now = player.serverLevel().gameTime
        when (kind) {
            HandCastPayload.FLIP -> {
                if (!data.vvvvvv || now - (lastFlip[player.uuid] ?: Long.MIN_VALUE / 2) < FLIP_COOLDOWN) return
                lastFlip[player.uuid] = now
                flipGravity(player)
            }
            HandCastPayload.HAND -> {
                if (!player.mainHandItem.isEmpty) return
                if (!data.fireballs && !data.lightningHands) return
                if (now - (lastHand[player.uuid] ?: Long.MIN_VALUE / 2) < HAND_COOLDOWN) return
                lastHand[player.uuid] = now
                val lightning = when {
                    data.fireballs && data.lightningHands -> {
                        val next = !(alternate[player.uuid] ?: false)
                        alternate[player.uuid] = next
                        next
                    }
                    else -> data.lightningHands
                }
                if (lightning) throwLightning(player) else throwFireball(player)
            }
        }
    }

    fun flipGravity(player: ServerPlayer) {
        val gravity = player.getAttribute(Attributes.GRAVITY) ?: return
        val magnitude = abs(gravity.baseValue).takeIf { it > 1.0e-4 } ?: LivingEntity.DEFAULT_BASE_GRAVITY
        gravity.baseValue = if (gravity.baseValue >= 0.0) -magnitude else magnitude
        player.resetFallDistance()
        player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.8f, 1.8f)
    }

    /** Над миром потолка нет, и улетевший вверх игрок без этого не вернётся. */
    fun restoreGravityAboveWorld(player: ServerPlayer) {
        if (player.y < player.level().maxBuildHeight + SKY_MARGIN) return
        val gravity = player.getAttribute(Attributes.GRAVITY) ?: return
        if (gravity.baseValue < 0.0) flipGravity(player)
    }

    private fun throwFireball(player: ServerPlayer) {
        val level = player.serverLevel()
        val look = player.lookAngle
        val fireball = LargeFireball(level, player, look, 1)
        val hand = player.eyePosition.add(look.scale(1.2)).add(0.0, -0.25, 0.0)
        fireball.setPos(hand.x, hand.y, hand.z)
        level.addFreshEntity(fireball)
        level.playSound(null, player.blockPosition(), SoundEvents.GHAST_SHOOT, SoundSource.PLAYERS, 1f, 1f)
    }

    /** Молния бьёт по прямой до края загрузки сервера и задевает всех на пути. */
    private fun throwLightning(player: ServerPlayer) {
        val level = player.serverLevel()
        val range = (player.server.playerList.viewDistance * 16).coerceIn(32, 512).toDouble()
        val eye = player.eyePosition
        val look = player.lookAngle
        val start = eye.add(look.scale(0.6)).add(0.0, -0.2, 0.0)
        val far = eye.add(look.scale(range))
        val hit = level.clip(ClipContext(eye, far, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player))
        val end = hit.location
        val holder = level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DamageTypes.LIGHTNING_BOLT)
        val source = DamageSource(holder, player, player)
        val path = AABB(eye, end).inflate(1.5)
        for (entity in level.getEntities(player, path) { it is LivingEntity && it.isAlive }) {
            val box = entity.boundingBox.inflate(0.6)
            if (box.clip(eye, end).isEmpty && !box.contains(eye)) continue
            entity.hurt(source, BEAM_DAMAGE)
            entity.igniteForSeconds(5f)
        }
        if (end.distanceToSqr(eye) > 36.0) {
            EntityType.LIGHTNING_BOLT.create(level)?.let { bolt ->
                bolt.moveTo(end.x, end.y, end.z)
                bolt.setCause(player)
                level.addFreshEntity(bolt)
            }
        }
        val beam = LightningBeamPayload(start, end, level.random.nextLong())
        for (watcher in level.players()) {
            if (watcher.distanceToSqr(eye) < 512.0 * 512.0) RuntimeHooks.sendPayload(watcher, beam)
        }
        level.playSound(null, player.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 2f, 1.3f)
        level.playSound(null, BlockPos.containing(end), SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.WEATHER, 2f, 1f)
    }

    fun forget(player: ServerPlayer) {
        lastHand.remove(player.uuid)
        lastFlip.remove(player.uuid)
        alternate.remove(player.uuid)
    }
}
