package com.reallyusefulribbits.mod.profession

import com.reallyusefulribbits.mod.config.ServerConfig
import com.reallyusefulribbits.mod.inventory.RibbitBags
import com.reallyusefulribbits.mod.logic.MerchantEconomy
import com.reallyusefulribbits.mod.logic.MerchantOfferKind
import com.reallyusefulribbits.mod.logic.MerchantPhase
import com.reallyusefulribbits.mod.logic.ProfessionKind
import com.reallyusefulribbits.mod.util.work
import com.yungnickyoung.minecraft.ribbits.entity.RibbitEntity
import com.yungnickyoung.minecraft.ribbits.module.SoundModule
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
            it !== ribbit && it is Merchant
        }
        val trader = traders.minByOrNull { it.distanceToSqr(ribbit) }
        if (trader == null) {
            dataAdvance(ribbit, MerchantPhase.BUILD_OFFERS)
            return
        }
        ribbit.navigation.moveTo(trader, 1.05)
        if (ribbit.distanceToSqr(trader) < 9.0) {
            copyTradeItem(ribbit, trader as Merchant)
            dataAdvance(ribbit, MerchantPhase.BUILD_OFFERS)
        }
    }

    private fun copyTradeItem(ribbit: RibbitEntity, merchant: Merchant) {
        val results = ArrayList<ItemStack>()
        if (merchant is Villager) {
            val listings = VillagerTrades.TRADES[merchant.villagerData.profession]
            val master = listings?.get(5) ?: listings?.values?.lastOrNull()
            master?.forEach { listing ->
                val offer = listing.getOffer(merchant, ribbit.random)
                if (offer != null) results += offer.result.copy()
            }
        }
        if (results.isEmpty()) {
            merchant.offers.forEach { results += it.result.copy() }
        }
        if (results.isEmpty()) return
        val pick = results[ribbit.random.nextInt(results.size)]
        RibbitBags.insert(ribbit.work(), ProfessionKind.MERCHANT, ribbit.work().applyLimit(pick, ProfessionKind.MERCHANT))
    }

    private fun buildOffers(level: ServerLevel, ribbit: RibbitEntity) {
        val data = ribbit.work()
        val ids = (0 until data.usedSlots(ProfessionKind.MERCHANT))
            .map { data.items[it] }
            .filter { !it.isEmpty }
            .map { net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(it.item).toString() }
        val plans = MerchantEconomy.plan(
            inventoryIds = ids,
            copiedId = ids.lastOrNull(),
            random = { min, max -> if (max <= min) min else ribbit.random.nextInt(min, max) },
            itemMax = { id ->
                val item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(
                    net.minecraft.resources.ResourceLocation.parse(id),
                )
                item.defaultMaxStackSize
            },
        )
        val offers = ribbit.offers
        offers.clear()
        for (plan in plans) {
            val item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(
                net.minecraft.resources.ResourceLocation.parse(plan.itemId),
            )
            val goods = ItemStack(item, plan.itemCount)
            val crystals = ItemStack(Items.AMETHYST_SHARD, plan.amethystCount)
            val offer = when (plan.kind) {
                MerchantOfferKind.SELL_FOR_AMETHYST -> MerchantOffer(
                    ItemCost(Items.AMETHYST_SHARD, plan.amethystCount),
                    goods,
                    999,
                    0,
                    0.05f,
                )
                MerchantOfferKind.BUY_FOR_AMETHYST -> MerchantOffer(
                    ItemCost(item, plan.itemCount),
                    crystals,
                    999,
                    0,
                    0.05f,
                )
            }
            offers.add(offer)
        }
        dataAdvance(ribbit, MerchantPhase.SEEK_PLAYER)
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
