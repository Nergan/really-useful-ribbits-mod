package com.reallyusefulribbits.mod.profession

import com.reallyusefulribbits.mod.attach.ModAttachments
import com.reallyusefulribbits.mod.config.ModConfig
import com.reallyusefulribbits.mod.config.ServerConfig
import com.reallyusefulribbits.mod.event.ModAdvancements
import com.reallyusefulribbits.mod.event.PlayerTickHandler
import com.reallyusefulribbits.mod.logic.SorcererAction
import com.reallyusefulribbits.mod.logic.SorcererTable
import com.reallyusefulribbits.mod.morph.PlayerMorph
import com.reallyusefulribbits.mod.morph.SorcererCurse
import com.reallyusefulribbits.mod.util.DelayedTasks
import com.reallyusefulribbits.mod.util.work
import com.reallyusefulribbits.mod.world.SafeLanding
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
import net.minecraft.tags.StructureTags
import net.minecraft.world.effect.MobEffect
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.MobCategory
import net.minecraft.world.entity.MobSpawnType
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.entity.monster.EnderMan
import net.minecraft.world.entity.monster.Phantom
import net.minecraft.world.level.GameType
import net.minecraft.world.level.Level
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.enchantment.EnchantmentHelper
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.BonemealableBlock
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

    fun tryCast(level: ServerLevel, ribbit: RibbitEntity, player: ServerPlayer): Boolean {
        ribbit.lookControl.setLookAt(player, 180f, 180f)
        ribbit.lookAt(player, 180f, 180f)
        val data = ribbit.work()
        if (data.cooldown > 0) return false
        val allowed = applicable(level, ribbit, player).toMutableSet()
        if (allowed.isEmpty()) return false
        data.cooldown = ModConfig.SORCERER_CLICK_COOLDOWN
        ribbit.setBuffing(true)
        spawnSpell(level, ribbit)
        var applied = false
        var chosen: SorcererAction? = null
        repeat(6) {
            if (allowed.isEmpty() || applied) return@repeat
            val action = SorcererTable.pick(ribbit.random.nextInt(), ServerConfig.chaosLevel(), allowed)
            applied = apply(level, ribbit, player, action)
            if (applied) chosen = action
            allowed.remove(action)
        }
        chosen?.let { playActionSound(level, ribbit, player, it) }
        ribbit.setBuffing(false)
        return applied
    }

    fun cleanseWithApple(level: ServerLevel, ribbit: RibbitEntity, player: ServerPlayer): Boolean {
        ribbit.lookControl.setLookAt(player, 180f, 180f)
        ribbit.lookAt(player, 180f, 180f)
        SorcererCurse.clear(player)
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, player.x, player.y + 1.0, player.z, 16, 0.4, 0.5, 0.4, 0.02)
        level.playSound(null, player.blockPosition(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 0.9f, 1.35f)
        level.playSound(null, ribbit.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.NEUTRAL, 1f, 1.2f)
        ModAdvancements.grantGetItOff(player)
        return true
    }

    private fun applicable(level: ServerLevel, ribbit: RibbitEntity, player: ServerPlayer): List<SorcererAction> {
        return SorcererTable.WEIGHTS.map { it.value }.filter { action ->
            when (action) {
                SorcererAction.ENCHANT -> enchantCandidates(level, player).isNotEmpty()
                SorcererAction.GROW_PLANTS -> hasNearbyGrowable(level, ribbit)
                SorcererAction.SUMMON_MOBS -> summonableTypes().isNotEmpty()
                SorcererAction.SUMMON_PETS -> PetSupport.petTypes(level).isNotEmpty()
                SorcererAction.MORPH -> morphTypes().isNotEmpty()
                SorcererAction.RANDOM_DIMENSION -> player.server.allLevels.any { it.dimension() != player.level().dimension() }
                SorcererAction.STRUCTURE_TRIP -> true
                else -> true
            }
        }
    }

    private fun apply(level: ServerLevel, ribbit: RibbitEntity, player: ServerPlayer, action: SorcererAction): Boolean {
        return when (action) {
            SorcererAction.EFFECT_OR_CLEANSE -> {
                effectOrCleanse(player, ribbit)
                true
            }
            SorcererAction.ENCHANT -> enchant(level, player)
            SorcererAction.LAUNCH -> {
                launch(player, ribbit)
                true
            }
            SorcererAction.GROW_PLANTS -> grow(level, ribbit)
            SorcererAction.WEATHER -> {
                weather(level, player)
                true
            }
            SorcererAction.DAY_NIGHT -> {
                dayNight(level, player)
                true
            }
            SorcererAction.SUMMON_MOBS -> summonMobs(level, ribbit)
            SorcererAction.LIGHTNING -> {
                lightning(level, ribbit)
                true
            }
            SorcererAction.FLIP -> {
                flip(player)
                true
            }
            SorcererAction.SCALE -> {
                scale(player, ribbit)
                true
            }
            SorcererAction.MAX_HEALTH -> {
                maxHealth(player, ribbit)
                true
            }
            SorcererAction.IGNITE_AREA -> ignite(level, ribbit)
            SorcererAction.STRUCTURE_TRIP -> structureTrip(level, ribbit, player)
            SorcererAction.DIAMOND_RAIN -> {
                diamonds(level, ribbit, player)
                true
            }
            SorcererAction.RANDOM_DIMENSION -> dimension(player)
            SorcererAction.CREATIVE_FLIGHT -> {
                flight(player)
                true
            }
            SorcererAction.SUMMON_PETS -> summonPets(level, ribbit, player)
            SorcererAction.SUMMON_GIANT_PHANTOM -> giantPhantom(level, ribbit, player)
            SorcererAction.SPECTATOR -> spectator(player)
            SorcererAction.MEGA_EXPLOSION -> megaExplosion(level, ribbit, player)
            SorcererAction.MORPH -> morph(level, ribbit, player)
            SorcererAction.SUMMON_DRAGON -> {
                dragon(level, ribbit, player)
                true
            }
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

    private fun enchantCandidates(level: ServerLevel, player: ServerPlayer): List<ItemStack> {
        val stacks = player.inventory.items + player.inventory.armor + listOf(player.mainHandItem, player.offhandItem)
        val registry = level.registryAccess().registryOrThrow(Registries.ENCHANTMENT)
        return stacks.filter { stack ->
            !stack.isEmpty && registry.holders().toList().any { holder -> stack.supportsEnchantment(holder) }
        }
    }

    private fun enchant(level: ServerLevel, player: ServerPlayer): Boolean {
        val candidates = enchantCandidates(level, player)
        if (candidates.isEmpty()) return false
        val stack = candidates[player.random.nextInt(candidates.size)]
        val registry = level.registryAccess().registryOrThrow(Registries.ENCHANTMENT)
        val options = registry.holders().toList().filter { stack.supportsEnchantment(it) }
        if (options.isEmpty()) return false
        val enchantment = options[player.random.nextInt(options.size)]
        val max = enchantment.value().getMaxLevel().coerceAtLeast(1)
        stack.enchant(enchantment, 1 + player.random.nextInt(max))
        level.playSound(null, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1f, 1f)
        @Suppress("UNUSED_VARIABLE")
        val helper = EnchantmentHelper.getEnchantmentsForCrafting(stack)
        return true
    }

    private fun launch(player: ServerPlayer, ribbit: RibbitEntity) {
        val height = SorcererTable.launchHeight(ribbit.random.nextInt())
        player.deltaMovement = player.deltaMovement.add(0.0, 0.28 + height * 0.09, 0.0)
        player.hurtMarked = true
        player.level().playSound(null, player.blockPosition(), SoundEvents.BREEZE_IDLE_AIR, SoundSource.PLAYERS, 1f, 1f)
    }

    private fun hasNearbyGrowable(level: ServerLevel, ribbit: RibbitEntity): Boolean {
        val origin = ribbit.blockPosition()
        val radius = ModConfig.GROW_RADIUS
        for (dx in -radius..radius) {
            for (dz in -radius..radius) {
                if (dx * dx + dz * dz > radius * radius) continue
                for (dy in -4..4) {
                    val pos = origin.offset(dx, dy, dz)
                    val state = level.getBlockState(pos)
                    val block = state.block
                    if (block is BonemealableBlock && block.isValidBonemealTarget(level, pos, state)) return true
                }
            }
        }
        return false
    }

    private fun grow(level: ServerLevel, ribbit: RibbitEntity): Boolean {
        val radius = ModConfig.GROW_RADIUS
        val origin = ribbit.blockPosition()
        val targets = ArrayList<BlockPos>()
        for (dx in -radius..radius) {
            for (dz in -radius..radius) {
                if (dx * dx + dz * dz > radius * radius) continue
                for (dy in -4..4) {
                    targets += origin.offset(dx, dy, dz)
                }
            }
        }
        targets.sortBy { it.distSqr(origin) }
        var count = 0
        for (pos in targets) {
            val state = level.getBlockState(pos)
            val block = state.block
            if (block is BonemealableBlock && block.isValidBonemealTarget(level, pos, state)) {
                if (block.isBonemealSuccess(level, level.random, pos, state)) {
                    block.performBonemeal(level, level.random, pos, state)
                    count++
                }
            }
            if (count >= 64) break
        }
        if (count == 0) return false
        level.playSound(null, ribbit.blockPosition(), SoundEvents.BONE_MEAL_USE, SoundSource.BLOCKS, 1f, 1f)
        return true
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

    private fun summonableTypes(): List<EntityType<*>> {
        return BuiltInRegistries.ENTITY_TYPE.filter { type ->
            type.canSummon() &&
                type != EntityType.ENDER_DRAGON &&
                type != EntityType.PLAYER &&
                type.category != MobCategory.MISC
        }
    }

    private fun summonMobs(level: ServerLevel, ribbit: RibbitEntity): Boolean {
        val types = summonableTypes()
        if (types.isEmpty()) return false
        val hostiles = types.filter { it.category == MobCategory.MONSTER }
        val peaceful = types.filter { it.category != MobCategory.MONSTER }
        val pool = if (hostiles.isNotEmpty() && ribbit.random.nextDouble() < SorcererTable.hostileSummonChance()) {
            hostiles
        } else {
            peaceful.ifEmpty { types }
        }
        val type = pool[ribbit.random.nextInt(pool.size)]
        val count = SorcererTable.summonCount(ribbit.random.nextInt())
        var spawned = 0
        repeat(count) {
            val pos = ribbit.blockPosition().offset(ribbit.random.nextInt(7) - 3, 0, ribbit.random.nextInt(7) - 3)
            if (type.spawn(level, pos, MobSpawnType.MOB_SUMMONED) != null) spawned++
        }
        if (spawned == 0) return false
        level.playSound(null, ribbit.blockPosition(), SoundEvents.EVOKER_PREPARE_WOLOLO, SoundSource.NEUTRAL, 1f, 1f)
        return true
    }

    private fun lightning(level: ServerLevel, ribbit: RibbitEntity) {
        ribbit.invulnerableTime = 60
        val bolt = EntityType.LIGHTNING_BOLT.create(level) ?: return
        var pos = ribbit.blockPosition().offset(ribbit.random.nextInt(7) - 3, 0, ribbit.random.nextInt(7) - 3)
        if (pos.distManhattan(ribbit.blockPosition()) < 3) {
            pos = ribbit.blockPosition().offset(4, 0, 0)
        }
        bolt.moveTo(pos.x + 0.5, pos.y.toDouble(), pos.z + 0.5)
        level.addFreshEntity(bolt)
    }

    private fun flip(player: ServerPlayer) {
        SorcererCurse.rememberBase(player)
        val visual = player.getData(ModAttachments.VISUAL.get())
        visual.upsideDown = !visual.upsideDown
        SorcererCurse.sync(player)
    }

    private fun scale(player: ServerPlayer, ribbit: RibbitEntity) {
        SorcererCurse.rememberBase(player)
        val attr = player.getAttribute(Attributes.SCALE) ?: return
        val grow = ribbit.random.nextBoolean()
        val steps = SorcererTable.scaleDelta(grow, ribbit.random.nextInt())
        attr.baseValue = SorcererTable.applyScaleSteps(attr.baseValue, steps)
    }

    private fun maxHealth(player: ServerPlayer, ribbit: RibbitEntity) {
        SorcererCurse.rememberBase(player)
        val attr = player.getAttribute(Attributes.MAX_HEALTH) ?: return
        attr.baseValue = SorcererTable.maxHealth(ribbit.random.nextInt()).toDouble()
        if (player.health > player.maxHealth) player.health = player.maxHealth
    }

    private fun ignite(level: ServerLevel, ribbit: RibbitEntity): Boolean {
        val radius = (ServerConfig.scanRadius() / 4).coerceAtLeast(4)
        val origin = ribbit.blockPosition()
        var lit = 0
        ribbit.setRemainingFireTicks(0)
        ribbit.invulnerableTime = 40
        for (dx in -radius..radius) {
            for (dz in -radius..radius) {
                if (dx * dx + dz * dz > radius * radius) continue
                if (kotlin.math.abs(dx) < 2 && kotlin.math.abs(dz) < 2) continue
                for (dy in -2..2) {
                    val firePos = origin.offset(dx, dy, dz)
                    if (!level.getBlockState(firePos).isAir) continue
                    val fire = Blocks.FIRE.defaultBlockState()
                    if (!fire.canSurvive(level, firePos)) continue
                    level.setBlock(firePos, fire, 3)
                    lit++
                }
            }
        }
        if (lit == 0) return false
        level.playSound(null, ribbit.blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 1f, 0.8f)
        return true
    }

    private fun structureTrip(level: ServerLevel, ribbit: RibbitEntity, player: ServerPlayer): Boolean {
        val dest = findExistingStructure(level, player.blockPosition()) ?: return false
        val safe = SafeLanding.find(level, dest)
        val returnLevelKey = level.dimension()
        val returnPos = player.position()
        val returnRot = player.yRot
        val returnPitch = player.xRot
        val playerId = player.uuid
        player.addEffect(MobEffectInstance(MobEffects.INVISIBILITY, ModConfig.STRUCTURE_TRIP_TICKS, 0, false, true))
        player.teleportTo(level, safe.x, safe.y, safe.z, player.yRot, player.xRot)
        level.playSound(null, player.blockPosition(), SoundEvents.CHORUS_FRUIT_TELEPORT, SoundSource.PLAYERS, 1f, 1f)
        DelayedTasks.later(level.server, ModConfig.STRUCTURE_TRIP_TICKS) {
            val backLevel = level.server.getLevel(returnLevelKey) ?: return@later
            val returning = backLevel.server.playerList.getPlayer(playerId) ?: return@later
            if (!returning.isAlive) return@later
            returning.teleportTo(backLevel, returnPos.x, returnPos.y, returnPos.z, returnRot, returnPitch)
            backLevel.playSound(null, returning.blockPosition(), SoundEvents.CHORUS_FRUIT_TELEPORT, SoundSource.PLAYERS, 1f, 0.8f)
        }
        @Suppress("UNUSED_VARIABLE")
        val ignored = ribbit.id
        return true
    }

    private fun findExistingStructure(level: ServerLevel, origin: BlockPos): BlockPos? {
        val registry = level.registryAccess().registryOrThrow(Registries.STRUCTURE)
        val tags = listOf(
            StructureTags.VILLAGE,
            StructureTags.RUINED_PORTAL,
            StructureTags.ON_TREASURE_MAPS,
            StructureTags.ON_OCEAN_EXPLORER_MAPS,
            StructureTags.ON_WOODLAND_EXPLORER_MAPS,
            StructureTags.ON_TRIAL_CHAMBERS_MAPS,
        ).shuffled()
        for (tag in tags) {
            if (registry.getTag(tag).isEmpty) continue
            val found = level.findNearestMapStructure(tag, origin, 64, false) ?: continue
            return found
        }
        return null
    }

    private fun diamonds(level: ServerLevel, ribbit: RibbitEntity, player: ServerPlayer) {
        val originX = ribbit.x
        val originY = ribbit.y
        val originZ = ribbit.z
        val perWave = ModConfig.DIAMOND_RAIN_COUNT / ModConfig.DIAMOND_RAIN_WAVES
        repeat(ModConfig.DIAMOND_RAIN_WAVES) { wave ->
            DelayedTasks.later(level.server, 1 + wave * 4) {
                if (!level.isLoaded(BlockPos.containing(originX, originY, originZ))) return@later
                repeat(perWave) {
                    val angle = level.random.nextDouble() * Math.PI * 2
                    val dist = level.random.nextDouble() * 6.5
                    val x = originX + cos(angle) * dist
                    val z = originZ + sin(angle) * dist
                    val y = originY + 14.0 + level.random.nextDouble() * 8.0
                    val entity = ItemEntity(level, x, y, z, ItemStack(Items.DIAMOND))
                    entity.setDeltaMovement(0.0, -0.05, 0.0)
                    entity.setPickUpDelay(15)
                    level.addFreshEntity(entity)
                }
            }
        }
        level.playSound(null, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1f, 0.5f)
    }

    private fun dimension(player: ServerPlayer): Boolean {
        val dest = player.server.allLevels.filter { it.dimension() != player.level().dimension() }.randomOrNull() ?: return false
        val landing = SafeLanding.find(dest, dest.sharedSpawnPos)
        player.teleportTo(dest, landing.x, landing.y, landing.z, player.yRot, player.xRot)
        dest.playSound(null, player.blockPosition(), SoundEvents.PORTAL_AMBIENT, SoundSource.PLAYERS, 1f, 1f)
        return true
    }

    private fun flight(player: ServerPlayer) {
        PlayerTickHandler.grantTemporaryFlight(player, ModConfig.FLIGHT_TICKS)
        player.level().playSound(null, player.blockPosition(), SoundEvents.ELYTRA_FLYING, SoundSource.PLAYERS, 0.6f, 1.4f)
        player.serverLevel().sendParticles(ParticleTypes.CLOUD, player.x, player.y + 1.0, player.z, 12, 0.3, 0.2, 0.3, 0.02)
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

    private fun summonPets(level: ServerLevel, ribbit: RibbitEntity, player: ServerPlayer): Boolean {
        val types = PetSupport.petTypes(level)
        if (types.isEmpty()) return false
        val count = SorcererTable.petCount(ribbit.random.nextInt())
        var spawned = 0
        repeat(count) {
            val type = types[ribbit.random.nextInt(types.size)]
            if (PetSupport.spawnTamed(level, player, type, ribbit.random)) spawned++
        }
        if (spawned > 0) {
            level.sendParticles(ParticleTypes.HEART, player.x, player.y + 1.0, player.z, 10, 0.6, 0.4, 0.6, 0.02)
        }
        return spawned > 0
    }

    private fun giantPhantom(level: ServerLevel, ribbit: RibbitEntity, player: ServerPlayer): Boolean {
        val phantom = EntityType.PHANTOM.create(level) as? Phantom ?: return false
        val pos = ribbit.blockPosition().offset(ribbit.random.nextInt(7) - 3, 6, ribbit.random.nextInt(7) - 3)
        phantom.moveTo(pos.x + 0.5, pos.y.toDouble(), pos.z + 0.5)
        phantom.phantomSize = SorcererTable.phantomSize(ribbit.random.nextInt())
        phantom.addEffect(MobEffectInstance(MobEffects.GLOWING, 20 * 60 * 10, 0, false, false))
        level.addFreshEntity(phantom)
        level.sendParticles(ParticleTypes.GLOW, phantom.x, phantom.y, phantom.z, 24, 1.2, 0.6, 1.2, 0.02)
        ModAdvancements.grantFoma(player)
        return true
    }

    private fun spectator(player: ServerPlayer): Boolean {
        val visual = player.getData(ModAttachments.VISUAL.get())
        if (player.gameMode.gameModeForPlayer != GameType.SPECTATOR) {
            visual.previousGameMode = player.gameMode.gameModeForPlayer.getName()
        }
        visual.spectatorTicks = ModConfig.SPECTATOR_TICKS
        player.setGameMode(GameType.SPECTATOR)
        player.serverLevel().sendParticles(ParticleTypes.SOUL, player.x, player.y + 1.0, player.z, 18, 0.4, 0.6, 0.4, 0.02)
        ModAdvancements.grantGhost(player)
        return true
    }

    private fun megaExplosion(level: ServerLevel, ribbit: RibbitEntity, player: ServerPlayer): Boolean {
        val radius = ServerConfig.scanRadius().coerceAtLeast(8)
        var dx: Int
        var dz: Int
        do {
            dx = ribbit.random.nextInt(radius * 2 + 1) - radius
            dz = ribbit.random.nextInt(radius * 2 + 1) - radius
        } while (dx * dx + dz * dz < 36)
        val pos = ribbit.blockPosition().offset(dx, 0, dz)
        val power = SorcererTable.explosionPower(ribbit.random.nextInt())
        ribbit.invulnerableTime = 80
        level.explode(ribbit, pos.x + 0.5, pos.y + 0.5, pos.z + 0.5, power, Level.ExplosionInteraction.TNT)
        ModAdvancements.grantNoMagicToday(player)
        return true
    }

    private fun morphTypes(): List<EntityType<*>> =
        BuiltInRegistries.ENTITY_TYPE.filter { PlayerMorph.isMorphable(it) }

    private fun morph(level: ServerLevel, ribbit: RibbitEntity, player: ServerPlayer): Boolean {
        val types = morphTypes()
        if (types.isEmpty()) return false
        val type = types[ribbit.random.nextInt(types.size)]
        SorcererCurse.applyMorph(player, type)
        level.sendParticles(ParticleTypes.WITCH, player.x, player.y + 1.0, player.z, 20, 0.4, 0.7, 0.4, 0.03)
        ModAdvancements.grantHexed(player)
        return true
    }

    private fun playActionSound(level: ServerLevel, ribbit: RibbitEntity, player: ServerPlayer, action: SorcererAction) {
        val at = player.blockPosition()
        val frog = ribbit.blockPosition()
        when (action) {
            SorcererAction.EFFECT_OR_CLEANSE ->
                level.playSound(null, at, SoundEvents.WITCH_DRINK, SoundSource.PLAYERS, 1f, 1.1f)
            SorcererAction.ENCHANT ->
                level.playSound(null, at, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1f, 1f)
            SorcererAction.LAUNCH ->
                level.playSound(null, at, SoundEvents.BREEZE_IDLE_AIR, SoundSource.PLAYERS, 1f, 0.8f)
            SorcererAction.GROW_PLANTS ->
                level.playSound(null, frog, SoundEvents.BONE_MEAL_USE, SoundSource.BLOCKS, 1f, 1f)
            SorcererAction.WEATHER ->
                level.playSound(null, at, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 0.6f, 1.3f)
            SorcererAction.DAY_NIGHT ->
                level.playSound(null, at, SoundEvents.BELL_BLOCK, SoundSource.PLAYERS, 0.7f, 1.4f)
            SorcererAction.SUMMON_MOBS ->
                level.playSound(null, frog, SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.NEUTRAL, 1f, 1f)
            SorcererAction.LIGHTNING ->
                level.playSound(null, frog, SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.WEATHER, 1f, 1f)
            SorcererAction.FLIP ->
                level.playSound(null, at, SoundEvents.ENDER_EYE_DEATH, SoundSource.PLAYERS, 1f, 0.5f)
            SorcererAction.SCALE ->
                level.playSound(null, at, SoundEvents.CHORUS_FRUIT_TELEPORT, SoundSource.PLAYERS, 0.8f, 1.6f)
            SorcererAction.MAX_HEALTH ->
                level.playSound(null, at, SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 0.8f, 1f)
            SorcererAction.IGNITE_AREA ->
                level.playSound(null, frog, SoundEvents.GHAST_SHOOT, SoundSource.BLOCKS, 1f, 0.7f)
            SorcererAction.STRUCTURE_TRIP ->
                level.playSound(null, at, SoundEvents.CHORUS_FRUIT_TELEPORT, SoundSource.PLAYERS, 1f, 0.8f)
            SorcererAction.SUMMON_PETS ->
                level.playSound(null, frog, SoundEvents.CAT_PURREOW, SoundSource.NEUTRAL, 1f, 1.1f)
            SorcererAction.DIAMOND_RAIN ->
                level.playSound(null, at, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.2f, 0.6f)
            SorcererAction.MORPH ->
                level.playSound(null, at, SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 1f, 0.7f)
            SorcererAction.SUMMON_GIANT_PHANTOM ->
                level.playSound(null, frog, SoundEvents.PHANTOM_AMBIENT, SoundSource.HOSTILE, 1.2f, 0.5f)
            SorcererAction.RANDOM_DIMENSION ->
                level.playSound(null, at, SoundEvents.PORTAL_TRAVEL, SoundSource.PLAYERS, 0.6f, 1.1f)
            SorcererAction.SPECTATOR ->
                level.playSound(null, at, SoundEvents.ILLUSIONER_MIRROR_MOVE, SoundSource.PLAYERS, 1f, 0.8f)
            SorcererAction.MEGA_EXPLOSION ->
                level.playSound(null, frog, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 1.4f, 0.6f)
            SorcererAction.CREATIVE_FLIGHT ->
                level.playSound(null, at, SoundEvents.ELYTRA_FLYING, SoundSource.PLAYERS, 0.7f, 1.4f)
            SorcererAction.SUMMON_DRAGON ->
                level.playSound(null, frog, SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 1f, 0.8f)
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
