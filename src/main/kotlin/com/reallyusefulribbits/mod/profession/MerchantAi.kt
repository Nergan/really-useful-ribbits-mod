package com.reallyusefulribbits.mod.profession

import com.reallyusefulribbits.mod.config.ModConfig
import com.reallyusefulribbits.mod.config.ServerConfig
import com.reallyusefulribbits.mod.inventory.RibbitBags
import com.reallyusefulribbits.mod.logic.MerchantEconomy
import com.reallyusefulribbits.mod.logic.MerchantPhase
import com.reallyusefulribbits.mod.logic.ProfessionKind
import com.reallyusefulribbits.mod.util.work
import com.yungnickyoung.minecraft.ribbits.entity.RibbitEntity
import com.yungnickyoung.minecraft.ribbits.module.SoundModule
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundSource
import net.minecraft.world.entity.npc.Villager
import net.minecraft.world.entity.npc.VillagerTrades
import net.minecraft.world.entity.npc.WanderingTrader
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.trading.ItemCost
import net.minecraft.world.item.trading.Merchant
import net.minecraft.world.item.trading.MerchantOffer
import net.minecraft.world.phys.AABB
object MerchantAi {
    private val STARTERS = listOf(
        Items.BREAD, Items.APPLE, Items.COD, Items.WHEAT, Items.BOOK,
        Items.CLAY_BALL, Items.KELP, Items.LILY_PAD, Items.GLOW_BERRIES, Items.AMETHYST_SHARD,
    )

    fun tick(level: ServerLevel, ribbit: RibbitEntity) {
        val data = ribbit.work()
        when (data.merchantPhaseEnum()) {
            MerchantPhase.INIT -> initStock(ribbit)
            MerchantPhase.SEEK_TRADER -> seekTrader(level, ribbit)
            MerchantPhase.BUILD_OFFERS -> buildOffers(level, ribbit)
            MerchantPhase.SEEK_PLAYER -> seekPlayer(level, ribbit)
            MerchantPhase.HARASS -> harass(level, ribbit)
            MerchantPhase.COOLDOWN -> cooldown(ribbit)
        }
    }

    fun onPlayerOpenedTrade(ribbit: RibbitEntity) {
        val data = ribbit.work()
        data.merchantPhase = MerchantPhase.COOLDOWN.name
        data.merchantTicks = 0
        data.merchantTargetId = -1
    }

    private fun initStock(ribbit: RibbitEntity) {
        val data = ribbit.work()
        if (!data.merchantReady) {
            val emeralds = ItemStack(Items.EMERALD, MerchantEconomy.INITIAL_EMERALDS)
            val amethysts = ItemStack(Items.AMETHYST_SHARD, MerchantEconomy.INITIAL_AMETHYSTS)
            RibbitBags.insert(data, ProfessionKind.MERCHANT, data.applyLimit(emeralds, ProfessionKind.MERCHANT))
            RibbitBags.insert(data, ProfessionKind.MERCHANT, data.applyLimit(amethysts, ProfessionKind.MERCHANT))
            repeat(MerchantEconomy.RANDOM_STARTER_STACKS) {
                val item = STARTERS[ribbit.random.nextInt(STARTERS.size)]
                val stack = ItemStack(item, 1 + ribbit.random.nextInt(16))
                RibbitBags.insert(data, ProfessionKind.MERCHANT, data.applyLimit(stack, ProfessionKind.MERCHANT))
            }
            data.merchantReady = true
        }
        data.merchantPhase = MerchantPhase.SEEK_TRADER.name
        data.merchantTicks = 0
    }

    private fun seekTrader(level: ServerLevel, ribbit: RibbitEntity) {
        val radius = ServerConfig.scanRadius().toDouble()
        val box = ribbit.boundingBox.inflate(radius)
        val traders = level.getEntitiesOfClass(net.minecraft.world.entity.Entity::class.java, box) {
            it !== ribbit && (it is WanderingTrader || it is Merchant)
        }
        val trader = traders.minByOrNull { it.distanceToSqr(ribbit) }
        if (trader == null) {
            val data = ribbit.work()
            data.merchantTicks++
            if (data.merchantTicks >= ModConfig.MERCHANT_TRADER_WAIT_TICKS) {
                dataAdvance(ribbit, MerchantPhase.BUILD_OFFERS)
            }
            return
        }
        ribbit.navigation.moveTo(trader, 1.05)
        if (ribbit.distanceToSqr(trader) < 9.0) {
            rememberVisit(ribbit, trader as Merchant)
            dataAdvance(ribbit, MerchantPhase.BUILD_OFFERS)
        }
    }

    private fun rememberVisit(ribbit: RibbitEntity, merchant: Merchant) {
        val data = ribbit.work()
        val offers = merchant.offers
        if (offers.isEmpty() && merchant is Villager) {
            val listings = VillagerTrades.TRADES[merchant.villagerData.profession]
            val master = listings?.get(5) ?: listings?.values?.lastOrNull()
            master?.forEach { listing ->
                val offer = listing.getOffer(merchant, ribbit.random) ?: return@forEach
                storeVisit(data, offer.result, offer.costA.count)
            }
        } else {
            for (offer in offers) {
                storeVisit(data, offer.result, offer.costA.count)
            }
        }
        if (data.copiedGoods.isNotEmpty()) {
            RibbitBags.insert(
                data,
                ProfessionKind.MERCHANT,
                data.applyLimit(data.copiedGoods.last().copy(), ProfessionKind.MERCHANT),
            )
        }
    }

    private fun storeVisit(data: com.reallyusefulribbits.mod.attach.RibbitWorkData, result: ItemStack, price: Int) {
        if (result.isEmpty) return
        val id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(result.item).toString()
        data.copiedTradeIds += id
        data.copiedTradePrices += price.coerceIn(1, 32)
        data.copiedGoods += result.copy()
        while (data.copiedTradeIds.size > MerchantEconomy.MAX_COPIED_TRADES) {
            data.copiedTradeIds.removeAt(0)
            data.copiedTradePrices.removeAt(0)
            if (data.copiedGoods.isNotEmpty()) data.copiedGoods.removeAt(0)
        }
    }

    private fun buildOffers(level: ServerLevel, ribbit: RibbitEntity) {
        val data = ribbit.work()
        val offers = ribbit.offers
        if (offers.size < MerchantEconomy.SLOT_COUNT) {
            val needed = MerchantEconomy.SLOT_COUNT - offers.size
            for (offer in createOffers(ribbit, needed)) {
                offers.add(offer)
            }
        } else {
            val start = MerchantEconomy.quarterStart(data.offerQuarter)
            val replacements = createOffers(ribbit, MerchantEconomy.QUARTER_SIZE)
            val kept = ArrayList(offers)
            for (i in replacements.indices) {
                val index = start + i
                if (index < kept.size) {
                    kept[index] = replacements[i]
                } else {
                    kept += replacements[i]
                }
            }
            offers.clear()
            kept.forEach { offers.add(it) }
            data.offerQuarter = (data.offerQuarter + 1) % 4
        }
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, ribbit.x, ribbit.y + 0.9, ribbit.z, 14, 0.35, 0.3, 0.35, 0.02)
        dataAdvance(ribbit, MerchantPhase.SEEK_PLAYER)
    }

    private fun createOffers(ribbit: RibbitEntity, count: Int): List<MerchantOffer> {
        val data = ribbit.work()
        val copies = data.copiedGoods.mapIndexedNotNull { index, stack ->
            if (stack.isEmpty) null else stack to data.copiedTradePrices.getOrElse(index) { 4 }
        }
        val fallback = (0 until data.usedSlots(ProfessionKind.MERCHANT))
            .map { data.items[it] }
            .filter { !it.isEmpty }
            .map { it to 4 }
        val pool = if (copies.isNotEmpty()) copies else fallback
        if (pool.isEmpty()) return emptyList()
        val random = { min: Int, max: Int -> if (max <= min) min else ribbit.random.nextInt(min, max) }
        return List(count) {
            val pick = pool[ribbit.random.nextInt(pool.size)]
            val goods = pick.first.copy()
            goods.count = MerchantEconomy.itemCountFor(goods.maxStackSize, random)
            val price = MerchantEconomy.averagePrice(
                copies.filter { it.first.item == goods.item }.map { it.second },
                pick.second,
            )
            MerchantOffer(
                ItemCost(Items.AMETHYST_SHARD, price),
                goods,
                999,
                0,
                0.05f,
            )
        }
    }

    private fun seekPlayer(level: ServerLevel, ribbit: RibbitEntity) {
        val radius = ServerConfig.scanRadius().toDouble()
        val players = level.getEntitiesOfClass(Player::class.java, AABB.ofSize(ribbit.position(), radius * 2, 16.0, radius * 2)) {
            it.isAlive && !it.isSpectator
        }
        val player = players.minByOrNull { it.distanceToSqr(ribbit) }
        if (player == null) {
            ribbit.work().merchantTicks++
            if (ribbit.work().merchantTicks > 80) dataAdvance(ribbit, MerchantPhase.COOLDOWN)
            return
        }
        ribbit.navigation.moveTo(player, 1.1)
        ribbit.lookControl.setLookAt(player)
        if (ribbit.work().merchantTicks % ModConfig.MERCHANT_GLOW_INTERVAL == 0) {
            level.sendParticles(ParticleTypes.GLOW, ribbit.x, ribbit.y + 0.65, ribbit.z, 4, 0.2, 0.25, 0.2, 0.0)
        }
        ribbit.work().merchantTargetId = player.id
        dataAdvance(ribbit, MerchantPhase.HARASS)
    }

    private fun harass(level: ServerLevel, ribbit: RibbitEntity) {
        val data = ribbit.work()
        val target = level.getEntity(data.merchantTargetId) as? Player
        if (target == null || !target.isAlive) {
            dataAdvance(ribbit, MerchantPhase.COOLDOWN)
            return
        }
        if (ribbit.isTrading) {
            onPlayerOpenedTrade(ribbit)
            return
        }
        data.merchantTicks++
        ribbit.navigation.moveTo(target, 1.15)
        ribbit.lookControl.setLookAt(target)
        if (data.merchantTicks % ModConfig.MERCHANT_GLOW_INTERVAL == 0) {
            level.sendParticles(ParticleTypes.GLOW, ribbit.x, ribbit.y + 0.65, ribbit.z, 5, 0.22, 0.28, 0.22, 0.0)
        }
        if (data.merchantTicks % MerchantEconomy.QUACK_INTERVAL_TICKS == 0) {
            level.playSound(
                null,
                ribbit.blockPosition(),
                SoundModule.ENTITY_RIBBIT_HURT.get(),
                SoundSource.NEUTRAL,
                0.9f,
                1.1f,
            )
        }
        if (data.merchantTicks >= MerchantEconomy.HARASS_TICKS) {
            dataAdvance(ribbit, MerchantPhase.COOLDOWN)
        }
    }

    private fun cooldown(ribbit: RibbitEntity) {
        val data = ribbit.work()
        data.merchantTicks++
        ribbit.navigation.stop()
        if (data.merchantTicks >= MerchantEconomy.COOLDOWN_TICKS) {
            dataAdvance(ribbit, MerchantPhase.SEEK_TRADER)
        }
    }

    private fun dataAdvance(ribbit: RibbitEntity, phase: MerchantPhase) {
        val data = ribbit.work()
        data.merchantPhase = phase.name
        data.merchantTicks = 0
    }
}
