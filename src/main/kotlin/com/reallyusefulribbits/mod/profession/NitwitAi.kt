package com.reallyusefulribbits.mod.profession

import com.reallyusefulribbits.mod.ReallyUsefulRibbitsMod
import com.reallyusefulribbits.mod.config.ModConfig
import com.reallyusefulribbits.mod.config.ServerConfig
import com.reallyusefulribbits.mod.inventory.RibbitBags
import com.reallyusefulribbits.mod.logic.ProfessionKind
import com.reallyusefulribbits.mod.util.work
import com.yungnickyoung.minecraft.ribbits.entity.RibbitEntity
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.Vec3
import kotlin.math.atan2

object NitwitAi {
    private const val GIFT_TAG = ReallyUsefulRibbitsMod.MOD_ID + "_gift"
    private const val COLLECT = "COLLECT"
    private const val DELIVER = "DELIVER"

    fun tick(level: ServerLevel, ribbit: RibbitEntity) {
        applyLuck(level, ribbit)
        val data = ribbit.work()
        if (data.task != DELIVER) data.task = COLLECT
        if (data.cooldown > 0) data.cooldown--
        val carrying = RibbitBags.hasItems(data, ProfessionKind.NITWIT)
        if (RibbitBags.isFull(data, ProfessionKind.NITWIT) || (carrying && data.cooldown <= 0 && data.task == COLLECT)) {
            data.task = DELIVER
        }
        if (data.task == DELIVER && carrying) {
            if (deliver(level, ribbit)) return
        }
        data.task = COLLECT
        collect(level, ribbit)
    }

    private fun applyLuck(level: ServerLevel, ribbit: RibbitEntity) {
        if (level.gameTime % ModConfig.NITWIT_LUCK_INTERVAL != 0L) return
        val radius = ServerConfig.scanRadius().toDouble()
        val box = ribbit.boundingBox.inflate(radius)
        for (entity in level.getEntitiesOfClass(LivingEntity::class.java, box)) {
            entity.addEffect(
                MobEffectInstance(MobEffects.LUCK, ModConfig.NITWIT_LUCK_DURATION, 0, true, true),
            )
        }
    }

    private fun collect(level: ServerLevel, ribbit: RibbitEntity): Boolean {
        val data = ribbit.work()
        if (RibbitBags.isFull(data, ProfessionKind.NITWIT)) return false
        val loot = nearestLoot(level, ribbit) ?: return false
        val reach = ribbit.distanceToSqr(loot) <= ModConfig.NITWIT_PICKUP_REACH_SQ
        if (!reach) {
            ribbit.navigation.moveTo(loot.x, loot.y, loot.z, 1.05)
            face(ribbit, loot.x, loot.z)
            return true
        }
        ribbit.navigation.stop()
        face(ribbit, loot.x, loot.z)
        val hadItems = RibbitBags.hasItems(data, ProfessionKind.NITWIT)
        val leftover = RibbitBags.insert(data, ProfessionKind.NITWIT, loot.item.copy())
        if (leftover.isEmpty) {
            loot.discard()
        } else if (leftover.count != loot.item.count) {
            loot.item = leftover
        } else {
            return true
        }
        level.playSound(null, ribbit.blockPosition(), SoundEvents.ITEM_PICKUP, SoundSource.NEUTRAL, 0.4f, 1.15f)
        if (!hadItems) {
            data.cooldown = ModConfig.NITWIT_GIFT_MIN_TICKS + ribbit.random.nextInt(ModConfig.NITWIT_GIFT_EXTRA_TICKS)
        }
        return true
    }

    private fun deliver(level: ServerLevel, ribbit: RibbitEntity): Boolean {
        val data = ribbit.work()
        val radius = ServerConfig.scanRadius().toDouble()
        val player = level.getEntitiesOfClass(Player::class.java, ribbit.boundingBox.inflate(radius)) {
            it.isAlive && !it.isSpectator
        }.minByOrNull { it.distanceToSqr(ribbit) } ?: return false
        face(ribbit, player.x, player.z)
        ribbit.lookControl.setLookAt(player, 180f, 180f)
        if (ribbit.distanceToSqr(player) > ModConfig.NITWIT_PLAYER_REACH_SQ) {
            ribbit.navigation.moveTo(player, 1.1)
            return true
        }
        ribbit.navigation.stop()
        data.taskTicks++
        if (data.taskTicks < ModConfig.NITWIT_TOSS_INTERVAL) return true
        data.taskTicks = 0
        val stack = RibbitBags.takeSlot(data, ProfessionKind.NITWIT) ?: return false
        toss(level, ribbit, player, stack)
        if (!RibbitBags.hasItems(data, ProfessionKind.NITWIT)) {
            data.task = COLLECT
            data.cooldown = ModConfig.NITWIT_GIFT_MIN_TICKS + ribbit.random.nextInt(ModConfig.NITWIT_GIFT_EXTRA_TICKS)
        }
        return true
    }

    private fun toss(level: ServerLevel, ribbit: RibbitEntity, player: Player, stack: net.minecraft.world.item.ItemStack) {
        val drop = ItemEntity(level, ribbit.x, ribbit.eyeY - 0.3, ribbit.z, stack)
        drop.setPickUpDelay(40)
        drop.addTag(GIFT_TAG)
        drop.setThrower(ribbit)
        val away = Vec3(player.x - ribbit.x, 0.0, player.z - ribbit.z)
        val flat = if (away.lengthSqr() > 1.0e-4) away.normalize() else Vec3(0.0, 0.0, 1.0)
        val wobble = (ribbit.random.nextFloat() - ribbit.random.nextFloat()) * 0.04
        drop.deltaMovement = Vec3(flat.x * 0.3 + wobble, 0.12, flat.z * 0.3 + wobble)
        level.addFreshEntity(drop)
        level.playSound(null, ribbit.blockPosition(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.3f, 0.7f)
    }

    private fun nearestLoot(level: ServerLevel, ribbit: RibbitEntity): ItemEntity? {
        val radius = ServerConfig.scanRadius().toDouble()
        return level.getEntitiesOfClass(ItemEntity::class.java, ribbit.boundingBox.inflate(radius)) { item ->
            item.isAlive &&
                !item.item.isEmpty &&
                item.tickCount >= 10 &&
                !item.hasPickUpDelay() &&
                !(item.tags.contains(GIFT_TAG) && item.tickCount < ModConfig.NITWIT_GIFT_IGNORE_TICKS)
        }.minByOrNull { it.distanceToSqr(ribbit) }
    }

    private fun face(ribbit: RibbitEntity, x: Double, z: Double) {
        val yaw = Math.toDegrees(atan2(-(x - ribbit.x), z - ribbit.z)).toFloat()
        ribbit.yRot = yaw
        ribbit.yHeadRot = yaw
        ribbit.yBodyRot = yaw
    }
}
