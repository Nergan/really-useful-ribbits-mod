package com.reallyusefulribbits.mod.home

import com.reallyusefulribbits.mod.ReallyUsefulRibbitsMod
import com.reallyusefulribbits.mod.event.ModAdvancements
import com.reallyusefulribbits.mod.inventory.RibbitBags
import com.reallyusefulribbits.mod.logic.ProfessionKind
import com.reallyusefulribbits.mod.util.professionKind
import com.reallyusefulribbits.mod.util.work
import com.yungnickyoung.minecraft.ribbits.entity.RibbitEntity
import net.minecraft.core.component.DataComponents
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.nbt.CompoundTag
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.component.CustomData

object HomePoint {
    private val MARACA = ResourceLocation.fromNamespaceAndPath("ribbits", "maraca")

    fun isMaraca(stack: ItemStack): Boolean {
        if (stack.isEmpty) return false
        return BuiltInRegistries.ITEM.getKey(stack.item) == MARACA
    }

    /**
     * Маракас забирает риббита в яйцо его профессии. Дом больше не переносится.
     * Торговец сюда не попадает: его товары нельзя высыпать на землю.
     */
    fun capture(player: Player, ribbit: RibbitEntity) {
        val level = ribbit.level() as? ServerLevel ?: return
        val kind = ribbit.professionKind()
        if (kind == ProfessionKind.MERCHANT) return
        val eggItem = BuiltInRegistries.ITEM.get(ResourceLocation.parse(eggId(kind)))
        if (eggItem == Items.AIR) {
            ReallyUsefulRibbitsMod.LOGGER.warn("Нет яйца спавна для профессии {}", kind)
            return
        }
        val data = ribbit.work()
        val drops = RibbitBags.extractAll(data, kind)
        data.forgetWorkplace()
        for (stack in drops) {
            val entity = ItemEntity(level, ribbit.x, ribbit.y + 0.2, ribbit.z, stack)
            entity.setPickUpDelay(10)
            level.addFreshEntity(entity)
        }
        val tag = ribbit.saveWithoutId(CompoundTag())
        listOf(
            "Pos", "Motion", "Rotation", "UUID", "FallDistance", "Fire", "Air", "OnGround",
            "PortalCooldown", "Passengers", "HomePosX", "HomePosY", "HomePosZ",
        ).forEach { tag.remove(it) }
        tag.putString("id", BuiltInRegistries.ENTITY_TYPE.getKey(ribbit.type).toString())
        val egg = ItemStack(eggItem)
        egg.set(DataComponents.ENTITY_DATA, CustomData.of(tag))
        if (!player.addItem(egg)) {
            val dropped = ItemEntity(level, player.x, player.y + 0.5, player.z, egg)
            dropped.setPickUpDelay(0)
            level.addFreshEntity(dropped)
        }
        ribbit.discard()
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, player.x, player.y + 0.6, player.z, 10, 0.3, 0.25, 0.3, 0.02)
        level.playSound(null, player.blockPosition(), SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.NEUTRAL, 0.8f, 1.2f)
        if (player is ServerPlayer) ModAdvancements.grantNewHome(player)
    }

    private fun eggId(kind: ProfessionKind): String = when (kind) {
        ProfessionKind.FISHERMAN -> "ribbits:ribbit_fisherman_spawn_egg"
        ProfessionKind.FARMER -> "ribbits:ribbit_gardener_spawn_egg"
        ProfessionKind.SORCERER -> "ribbits:ribbit_sorcerer_spawn_egg"
        ProfessionKind.NITWIT -> "ribbits:ribbit_nitwit_spawn_egg"
        else -> "ribbits:ribbit_nitwit_spawn_egg"
    }
}
