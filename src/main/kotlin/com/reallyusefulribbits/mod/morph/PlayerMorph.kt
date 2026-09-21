package com.reallyusefulribbits.mod.morph

import com.reallyusefulribbits.mod.attach.ModAttachments
import com.reallyusefulribbits.mod.client.ClientVisuals
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.EntityDimensions
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.FlyingMob
import net.minecraft.world.entity.MobCategory
import net.minecraft.world.entity.Pose
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation
import net.minecraft.world.entity.Mob
import net.minecraft.world.entity.animal.AbstractFish
import net.minecraft.world.entity.animal.Dolphin
import net.minecraft.world.entity.animal.FlyingAnimal
import net.minecraft.world.entity.animal.Squid
import net.minecraft.world.entity.animal.Turtle
import net.minecraft.world.entity.animal.axolotl.Axolotl
import net.minecraft.world.entity.monster.Guardian
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import java.util.concurrent.ConcurrentHashMap

object PlayerMorph {
    private val flyCache = ConcurrentHashMap<EntityType<*>, Boolean>()
    private val landDeathCache = ConcurrentHashMap<EntityType<*>, Boolean>()

    @JvmStatic
    fun typeOf(player: Player): EntityType<*>? {
        val id = if (player.level().isClientSide) {
            ClientVisuals.morphId(player.uuid)
        } else {
            player.getData(ModAttachments.VISUAL.get()).morphId
        }
        return parse(id)
    }

    @JvmStatic
    fun dimensionsOf(player: Player, pose: Pose): EntityDimensions? {
        val type = typeOf(player) ?: return null
        val base = type.dimensions
        return when (pose) {
            Pose.CROUCHING -> EntityDimensions.scalable(base.width, (base.height * 0.75f).coerceAtLeast(0.4f))
            Pose.SWIMMING, Pose.FALL_FLYING, Pose.SPIN_ATTACK ->
                EntityDimensions.scalable(base.width, (base.height * 0.45f).coerceAtLeast(0.3f))
            else -> base
        }
    }

    @JvmStatic
    fun eyeHeightOf(player: Player, pose: Pose): Float? {
        val dims = dimensionsOf(player, pose) ?: return null
        return dims.eyeHeight
    }

    fun parse(id: String): EntityType<*>? {
        if (id.isBlank()) return null
        return try {
            BuiltInRegistries.ENTITY_TYPE.getOptional(ResourceLocation.parse(id)).orElse(null)
        } catch (_: Exception) {
            null
        }
    }

    fun canFly(level: Level, type: EntityType<*>): Boolean {
        return flyCache.getOrPut(type) {
            val dummy = type.create(level) ?: return@getOrPut false
            val fly = dummy is FlyingMob ||
                dummy is FlyingAnimal ||
                (dummy is Mob && dummy.navigation is FlyingPathNavigation)
            dummy.discard()
            fly
        }
    }

    fun diesOnLand(level: Level, type: EntityType<*>): Boolean {
        return landDeathCache.getOrPut(type) {
            val dummy = type.create(level) ?: return@getOrPut waterCategory(type)
            val amphibious = dummy is Dolphin || dummy is Turtle || dummy is Axolotl
            val dies = !amphibious && (dummy is AbstractFish || dummy is Squid || dummy is Guardian || waterCategory(type))
            dummy.discard()
            dies
        }
    }

    @JvmStatic
    fun isHumanoidType(type: EntityType<*>): Boolean {
        if (type == EntityType.PLAYER) return true
        val path = BuiltInRegistries.ENTITY_TYPE.getKey(type).path
        if (path.contains("horse") || path.contains("wolf") || path.contains("boat")) return false
        return path.contains("villager") ||
            path.contains("vindicator") ||
            path.contains("pillager") ||
            path.contains("evoker") ||
            path.contains("illusioner") ||
            path.contains("witch") ||
            path.contains("enderman") ||
            path.contains("piglin") ||
            path.contains("husk") ||
            path.contains("drowned") ||
            path.contains("stray") ||
            path.contains("bogged") ||
            path == "giant" ||
            path.contains("zombie") ||
            path.contains("skeleton")
    }

    fun isMorphable(type: EntityType<*>): Boolean {
        if (!type.canSummon()) return false
        if (type == EntityType.PLAYER || type == EntityType.ENDER_DRAGON) return false
        if (type == EntityType.WITHER || type == EntityType.WARDEN) return false
        if (type.category == MobCategory.MISC) return false
        val dims = type.dimensions
        return dims.width <= 4.5f && dims.height <= 6.5f
    }

    private fun waterCategory(type: EntityType<*>): Boolean {
        val cat = type.category
        return cat == MobCategory.WATER_AMBIENT ||
            cat == MobCategory.WATER_CREATURE ||
            cat == MobCategory.UNDERGROUND_WATER_CREATURE
    }
}
