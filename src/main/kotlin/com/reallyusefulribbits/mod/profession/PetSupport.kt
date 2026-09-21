package com.reallyusefulribbits.mod.profession

import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.util.RandomSource
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.MobSpawnType
import net.minecraft.world.entity.TamableAnimal
import net.minecraft.world.entity.animal.Cat
import net.minecraft.world.entity.animal.Fox
import net.minecraft.world.entity.animal.Parrot
import net.minecraft.world.entity.animal.Wolf
import net.minecraft.world.entity.animal.horse.AbstractHorse
import net.minecraft.world.entity.animal.horse.Llama

object PetSupport {
    @Volatile
    private var cache: List<EntityType<*>>? = null

    fun petTypes(level: ServerLevel): List<EntityType<*>> {
        cache?.let { return it }
        val found = BuiltInRegistries.ENTITY_TYPE.filter { type ->
            if (!type.canSummon()) return@filter false
            val dummy = type.create(level) ?: return@filter false
            val ok = dummy is TamableAnimal || dummy is AbstractHorse
            dummy.discard()
            ok
        }
        cache = found
        return found
    }

    fun spawnTamed(
        level: ServerLevel,
        player: ServerPlayer,
        type: EntityType<*>,
        random: RandomSource,
    ): Boolean {
        val pos = player.blockPosition().offset(random.nextInt(5) - 2, 0, random.nextInt(5) - 2)
        val spawned = type.spawn(level, pos, MobSpawnType.MOB_SUMMONED) ?: return false
        when (spawned) {
            is TamableAnimal -> {
                spawned.tame(player)
                spawned.setOwnerUUID(player.uuid)
                spawned.isOrderedToSit = false
            }
            is AbstractHorse -> {
                spawned.isTamed = true
                spawned.tameWithName(player)
            }
            else -> {
                spawned.discard()
                return false
            }
        }
        paint(spawned, level, random)
        return true
    }

    private fun paint(entity: LivingEntity, level: ServerLevel, random: RandomSource) {
        when (entity) {
            is Cat -> {
                val variants = level.registryAccess().registryOrThrow(Registries.CAT_VARIANT)
                variants.getRandom(random).ifPresent { entity.variant = it }
            }
            is Wolf -> {
                val variants = level.registryAccess().registryOrThrow(Registries.WOLF_VARIANT)
                variants.getRandom(random).ifPresent { entity.variant = it }
            }
            is Parrot -> {
                val values = Parrot.Variant.entries
                entity.variant = values[random.nextInt(values.size)]
            }
            is Fox -> {
                val values = Fox.Type.entries
                entity.variant = values[random.nextInt(values.size)]
            }
            is Llama -> {
                val values = Llama.Variant.entries
                entity.variant = values[random.nextInt(values.size)]
            }
        }
    }
}
