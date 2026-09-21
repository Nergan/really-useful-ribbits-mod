package com.reallyusefulribbits.mod.profession

import com.reallyusefulribbits.mod.attach.ModAttachments
import com.reallyusefulribbits.mod.config.ModConfig
import com.reallyusefulribbits.mod.config.ServerConfig
import com.reallyusefulribbits.mod.event.ModAdvancements
import com.reallyusefulribbits.mod.logic.SorcererAction
import com.reallyusefulribbits.mod.logic.SorcererTable
import com.reallyusefulribbits.mod.network.PlayerVisualPayload
import com.reallyusefulribbits.mod.util.work
import com.yungnickyoung.minecraft.ribbits.entity.RibbitEntity
import net.minecraft.core.BlockPos
import net.minecraft.core.Holder
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.effect.MobEffect
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.MobCategory
import net.minecraft.world.entity.MobSpawnType
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.entity.monster.EnderMan
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.enchantment.EnchantmentHelper
import net.minecraft.world.level.block.BonemealableBlock
import net.minecraft.world.level.levelgen.Heightmap
import net.neoforged.neoforge.network.PacketDistributor
import kotlin.math.cos
import kotlin.math.sin

object SorcererAi {
    fun tick(level: ServerLevel, ribbit: RibbitEntity) {
        val data = ribbit.work()
        if (data.cooldown > 0) data.cooldown--
        if (level.gameTime % ModConfig.ENDERMAN_FLEE_INTERVAL == 0L) {
            scareEndermen(level, ribbit)
        }
    }

    fun onRightClick(level: ServerLevel, ribbit: RibbitEntity, player: ServerPlayer) {
        val data = ribbit.work()
        if (data.cooldown > 0) return
        data.cooldown = ModConfig.SORCERER_CLICK_COOLDOWN
        ribbit.setBuffing(true)
        spawnSpell(level, ribbit)
        val action = SorcererTable.pick(ribbit.random.nextInt(SorcererTable.TOTAL_WEIGHT))
        apply(level, ribbit, player, action)
        ribbit.setBuffing(false)
    }

    private fun apply(level: ServerLevel, ribbit: RibbitEntity, player: ServerPlayer, action: SorcererAction) {
        when (action) {
            SorcererAction.EFFECT_OR_CLEANSE -> effectOrCleanse(player, ribbit)
            SorcererAction.ENCHANT -> enchant(level, player)
            SorcererAction.LAUNCH -> launch(player, ribbit)
            SorcererAction.GROW_PLANTS -> grow(level, ribbit)
            SorcererAction.WEATHER -> weather(level, player)
            SorcererAction.DAY_NIGHT -> dayNight(level, player)
            SorcererAction.SUMMON_PEACEFUL -> summonPeaceful(level, ribbit)
            SorcererAction.LIGHTNING -> lightning(level, ribbit)
            SorcererAction.FLIP -> flip(player)
            SorcererAction.SCALE -> scale(player, ribbit)
            SorcererAction.MAX_HEALTH -> maxHealth(player, ribbit)
            SorcererAction.DIAMOND_RAIN -> diamonds(level, ribbit, player)
            SorcererAction.RANDOM_DIMENSION -> dimension(player)
            SorcererAction.SUMMON_DRAGON -> dragon(level, ribbit, player)
        }
    }

    private fun effectOrCleanse(player: ServerPlayer, ribbit: RibbitEntity) {
        if (ribbit.random.nextBoolean()) {
            player.removeAllEffects()
        } else {
            val effects = BuiltInRegistries.MOB_EFFECT.holders()
                .filter { !it.value().isInstantenous }
                .toList()
            if (effects.isNotEmpty()) {
                val effect: Holder<MobEffect> = effects[ribbit.random.nextInt(effects.size)]
                player.addEffect(MobEffectInstance(effect, -1, 0, false, true))
            }
        }
    }

    private fun enchant(level: ServerLevel, player: ServerPlayer) {
        val stacks = player.inventory.items + player.inventory.armor + listOf(player.mainHandItem, player.offhandItem)
        val candidates = stacks.filter { !it.isEmpty }
        if (candidates.isEmpty()) return
        val stack = candidates[player.random.nextInt(candidates.size)]
        val registry = level.registryAccess().registryOrThrow(Registries.ENCHANTMENT)
        val options = registry.holders().filter { stack.supportsEnchantment(it) }.toList()
        if (options.isNotEmpty()) {
            val enchantment = options[player.random.nextInt(options.size)]
            val max = enchantment.value().getMaxLevel().coerceAtLeast(1)
            stack.enchant(enchantment, 1 + player.random.nextInt(max))
        }
        level.playSound(null, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1f, 1f)
        @Suppress("UNUSED_VARIABLE")
        val helper = EnchantmentHelper.getEnchantmentsForCrafting(stack)
    }

    private fun launch(player: ServerPlayer, ribbit: RibbitEntity) {
        val height = SorcererTable.launchHeight(ribbit.random.nextInt())
        player.deltaMovement = player.deltaMovement.add(0.0, 0.28 + height * 0.09, 0.0)
        player.hurtMarked = true
        player.level().playSound(null, player.blockPosition(), SoundEvents.BREEZE_IDLE_AIR, SoundSource.PLAYERS, 1f, 1f)
    }

    private fun grow(level: ServerLevel, ribbit: RibbitEntity) {
        val radius = ServerConfig.scanRadius()
        val origin = ribbit.blockPosition()
        var count = 0
        for (dx in -radius..radius) {
            for (dz in -radius..radius) {
                if (dx * dx + dz * dz > radius * radius) continue
                for (dy in -8..8) {
                    val pos = origin.offset(dx, dy, dz)
                    val state = level.getBlockState(pos)
                    val block = state.block
                    if (block is BonemealableBlock && block.isValidBonemealTarget(level, pos, state)) {
                        if (block.isBonemealSuccess(level, level.random, pos, state)) {
                            block.performBonemeal(level, level.random, pos, state)
                            count++
                        }
                    }
                    if (count > 256) break
                }
            }
        }
        level.playSound(null, ribbit.blockPosition(), SoundEvents.BONE_MEAL_USE, SoundSource.BLOCKS, 1f, 1f)
    }

    private fun weather(level: ServerLevel, player: ServerPlayer) {
        val raining = level.isRaining || level.isThundering
        if (raining) {
            level.setWeatherParameters(6000, 0, false, false)
        } else {
            val thunder = level.random.nextBoolean()
            level.setWeatherParameters(0, 6000, true, thunder)
        }
        level.playSound(null, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.WEATHER, 1f, 0.8f)
    }

    private fun dayNight(level: ServerLevel, player: ServerPlayer) {
        val day = level.isDay
        val target = if (day) 18000L else 1000L
        val worldData = level.server.worldData
        if (worldData is net.minecraft.world.level.storage.ServerLevelData) {
            worldData.dayTime = target
        } else {
            runCatching { worldData.overworldData().dayTime = target }
        }
        level.playSound(null, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1f, 1.3f)
    }

    private fun summonPeaceful(level: ServerLevel, ribbit: RibbitEntity) {
        val types = BuiltInRegistries.ENTITY_TYPE.filter { type ->
            val cat = type.category
            type.canSummon() && (
                cat == MobCategory.CREATURE ||
                    cat == MobCategory.AMBIENT ||
                    cat == MobCategory.AXOLOTLS ||
                    cat == MobCategory.WATER_AMBIENT ||
                    cat == MobCategory.WATER_CREATURE ||
                    cat == MobCategory.UNDERGROUND_WATER_CREATURE
                )
        }
        if (types.isEmpty()) return
        val type = types[ribbit.random.nextInt(types.size)]
        val pos = ribbit.blockPosition().offset(ribbit.random.nextInt(5) - 2, 0, ribbit.random.nextInt(5) - 2)
        type.spawn(level, pos, MobSpawnType.MOB_SUMMONED)
        level.playSound(null, ribbit.blockPosition(), SoundEvents.EVOKER_PREPARE_WOLOLO, SoundSource.NEUTRAL, 1f, 1f)
    }

    private fun lightning(level: ServerLevel, ribbit: RibbitEntity) {
        val bolt = EntityType.LIGHTNING_BOLT.create(level) ?: return
        val pos = ribbit.blockPosition().offset(ribbit.random.nextInt(7) - 3, 0, ribbit.random.nextInt(7) - 3)
        bolt.moveTo(pos.x + 0.5, pos.y.toDouble(), pos.z + 0.5)
        level.addFreshEntity(bolt)
    }

    private fun flip(player: ServerPlayer) {
        val visual = player.getData(ModAttachments.VISUAL.get())
        visual.upsideDown = !visual.upsideDown
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, PlayerVisualPayload(player.uuid, visual.upsideDown))
        player.level().playSound(null, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1f, 0.6f)
    }

    private fun scale(player: ServerPlayer, ribbit: RibbitEntity) {
        val attr = player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.SCALE) ?: return
        attr.baseValue = SorcererTable.applyScaleSteps(attr.baseValue, SorcererTable.scaleSteps(ribbit.random.nextInt()))
        player.level().playSound(null, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1f, 1.6f)
    }

    private fun maxHealth(player: ServerPlayer, ribbit: RibbitEntity) {
        val attr = player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH) ?: return
        attr.baseValue = SorcererTable.maxHealth(ribbit.random.nextInt()).toDouble()
        if (player.health > player.maxHealth) player.health = player.maxHealth
        player.level().playSound(null, player.blockPosition(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 0.8f, 1f)
    }

    private fun diamonds(level: ServerLevel, ribbit: RibbitEntity, player: ServerPlayer) {
        val radius = ServerConfig.scanRadius()
        repeat(256) {
            val angle = ribbit.random.nextDouble() * Math.PI * 2
            val dist = ribbit.random.nextDouble() * radius
            val x = ribbit.x + cos(angle) * dist
            val z = ribbit.z + sin(angle) * dist
            val y = ribbit.y + 8 + ribbit.random.nextDouble() * 6
            level.addFreshEntity(ItemEntity(level, x, y, z, ItemStack(Items.DIAMOND)))
        }
        level.playSound(null, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1f, 0.5f)
    }

    private fun dimension(player: ServerPlayer) {
        val dest = player.server.allLevels.filter { it.dimension() != player.level().dimension() }.randomOrNull() ?: return
        val spawn = dest.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, dest.sharedSpawnPos)
        player.teleportTo(dest, spawn.x + 0.5, spawn.y.toDouble(), spawn.z + 0.5, player.yRot, player.xRot)
        player.level().playSound(null, player.blockPosition(), SoundEvents.PORTAL_AMBIENT, SoundSource.PLAYERS, 1f, 1f)
    }

    private fun dragon(level: ServerLevel, ribbit: RibbitEntity, player: ServerPlayer) {
        val pos = ribbit.blockPosition().above(10)
        EntityType.ENDER_DRAGON.spawn(level, pos, MobSpawnType.TRIGGERED)
        for (other in level.server.playerList.players) {
            other.playNotifySound(SoundEvents.EVOKER_PREPARE_ATTACK, SoundSource.HOSTILE, 1f, 0.7f)
        }
        ModAdvancements.grantOverdidIt(player)
    }

    private fun scareEndermen(level: ServerLevel, ribbit: RibbitEntity) {
        val radius = ServerConfig.scanRadius().toDouble()
        val box = ribbit.boundingBox.inflate(radius)
        for (enderman in level.getEntitiesOfClass(EnderMan::class.java, box)) {
            enderman.target = null
            val away = enderman.position().subtract(ribbit.position())
            val dest = if (away.lengthSqr() < 0.01) {
                enderman.blockPosition().offset(8, 0, 0)
            } else {
                val n = away.normalize().scale(12.0)
                BlockPos.containing(enderman.x + n.x, enderman.y, enderman.z + n.z)
            }
            enderman.navigation.moveTo(dest.x.toDouble(), dest.y.toDouble(), dest.z.toDouble(), 1.4)
        }
    }

    private fun spawnSpell(level: ServerLevel, ribbit: RibbitEntity) {
        val particle = BuiltInRegistries.PARTICLE_TYPE.get(
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("ribbits", "spell"),
        ) as? net.minecraft.core.particles.SimpleParticleType ?: ParticleTypes.WITCH
        for (i in 0 until 16) {
            val theta = i * (Math.PI * 2 / 16)
            level.sendParticles(
                particle,
                ribbit.x + cos(theta) * 1.2,
                ribbit.y + 0.2,
                ribbit.z + sin(theta) * 1.2,
                1,
                0.0,
                0.0,
                0.0,
                0.0,
            )
        }
        level.sendParticles(ParticleTypes.WITCH, ribbit.x, ribbit.y + 1.0, ribbit.z, 8, 0.3, 0.4, 0.3, 0.01)
    }
}
