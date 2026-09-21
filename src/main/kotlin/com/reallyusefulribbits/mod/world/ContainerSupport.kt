package com.reallyusefulribbits.mod.world

import com.reallyusefulribbits.mod.config.ModConfig
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceKey
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.Container
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.InventoryMenu
import net.minecraft.core.component.DataComponents
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.BarrelBlock
import net.minecraft.world.level.block.EnderChestBlock
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.ContainerOpenersCounter
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import net.neoforged.neoforge.capabilities.Capabilities
import net.neoforged.neoforge.common.util.FakePlayerFactory
import net.neoforged.neoforge.items.IItemHandler
import net.neoforged.neoforge.items.ItemHandlerHelper
import net.neoforged.neoforge.items.wrapper.InvWrapper

object ContainerSupport {
    private data class OpenLid(
        val dimension: ResourceKey<Level>,
        val pos: BlockPos,
        var until: Int,
        val player: Player,
    )

    private val openLids = ArrayList<OpenLid>()

    fun isStorage(level: Level, pos: BlockPos): Boolean {
        val state = level.getBlockState(pos)
        if (state.block is EnderChestBlock) return false
        if (level.getBlockEntity(pos) is Container) return true
        return handler(level, pos) != null
    }

    fun handler(level: Level, pos: BlockPos): IItemHandler? {
        val found = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, null as Direction?)
        if (found != null) return found
        for (side in Direction.entries) {
            val sided = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, side)
            if (sided != null) return sided
        }
        val be = level.getBlockEntity(pos)
        if (be is Container) return InvWrapper(be)
        return null
    }

    fun insertAll(level: Level, pos: BlockPos, stacks: List<ItemStack>): List<ItemStack> {
        val itemHandler = handler(level, pos) ?: return stacks
        val normalized = stacks.map { stripCarryCap(it.copy()) }.filter { !it.isEmpty }
        val leftover = ArrayList<ItemStack>()
        val seen = HashSet<Item>()
        for (stack in normalized) {
            if (!seen.add(stack.item)) continue
            leftover += restackLimited(itemHandler, stack.item)
        }
        for (stack in normalized) {
            val rest = ItemHandlerHelper.insertItemStacked(itemHandler, stack, false)
            if (!rest.isEmpty) leftover += rest
        }
        return leftover
    }

    /** Старые стопки с урезанным лимитом (по 4) снова складываются до обычного размера. */
    private fun restackLimited(handler: IItemHandler, item: Item): List<ItemStack> {
        val gathered = ArrayList<ItemStack>()
        for (slot in 0 until handler.slots) {
            val peek = handler.getStackInSlot(slot)
            if (peek.isEmpty || !peek.`is`(item) || !peek.has(DataComponents.MAX_STACK_SIZE)) continue
            val forced = peek.get(DataComponents.MAX_STACK_SIZE) ?: continue
            if (forced >= item.defaultMaxStackSize) continue
            val taken = handler.extractItem(slot, peek.count, false)
            if (!taken.isEmpty) gathered += stripCarryCap(taken)
        }
        val left = ArrayList<ItemStack>()
        for (stack in gathered) {
            val rest = ItemHandlerHelper.insertItemStacked(handler, stack, false)
            if (!rest.isEmpty) left += rest
        }
        return left
    }

    private fun stripCarryCap(stack: ItemStack): ItemStack {
        if (stack.isEmpty || !stack.has(DataComponents.MAX_STACK_SIZE)) return stack
        val forced = stack.get(DataComponents.MAX_STACK_SIZE) ?: return stack
        val vanilla = stack.item.defaultMaxStackSize
        if (forced > vanilla || stack.count > vanilla) return stack
        val count = stack.count
        stack.remove(DataComponents.MAX_STACK_SIZE)
        stack.count = count
        return stack
    }

    fun extractMatching(level: Level, pos: BlockPos, test: (ItemStack) -> Boolean, count: Int): ItemStack {
        val itemHandler = handler(level, pos) ?: return ItemStack.EMPTY
        var remaining = count
        var taken = ItemStack.EMPTY
        for (slot in 0 until itemHandler.slots) {
            val peek = itemHandler.getStackInSlot(slot)
            if (peek.isEmpty || !test(peek)) continue
            val extracted = itemHandler.extractItem(slot, remaining, false)
            if (extracted.isEmpty) continue
            if (taken.isEmpty) {
                taken = extracted
            } else if (ItemStack.isSameItemSameComponents(taken, extracted)) {
                taken.grow(extracted.count)
            }
            remaining -= extracted.count
            if (remaining <= 0) break
        }
        return taken
    }

    fun openBriefly(level: ServerLevel, pos: BlockPos, actor: Player? = null) {
        val be = level.getBlockEntity(pos) ?: return
        if (be !is Container) return
        val existing = openLids.firstOrNull { it.dimension == level.dimension() && it.pos == pos }
        if (existing != null) {
            existing.until = level.server.tickCount + ModConfig.CONTAINER_OPEN_TICKS
            return
        }
        val player = actor ?: FakePlayerFactory.getMinecraft(level)
        be.startOpen(player)
        openLids += OpenLid(level.dimension(), pos, level.server.tickCount + ModConfig.CONTAINER_OPEN_TICKS, player)
    }

    fun tick(server: MinecraftServer) {
        if (openLids.isEmpty()) return
        val now = server.tickCount
        val due = openLids.filter { it.until <= now }
        openLids.removeAll(due.toSet())
        for (lid in due) {
            val level = server.getLevel(lid.dimension) ?: continue
            val be = level.getBlockEntity(lid.pos)
            if (be is Container) be.stopOpen(lid.player)
            if (!playerIsUsing(level, lid.pos)) {
                zeroOpenCount(be)
                val state = level.getBlockState(lid.pos)
                if (state.block is BarrelBlock && state.getValue(BarrelBlock.OPEN)) {
                    level.setBlock(lid.pos, state.setValue(BarrelBlock.OPEN, false), 3)
                }
                level.blockEvent(lid.pos, state.block, 1, 0)
            }
        }
    }

    private fun playerIsUsing(level: ServerLevel, pos: BlockPos): Boolean {
        val center = Vec3.atCenterOf(pos)
        val box = AABB(center, center).inflate(8.0)
        return level.getEntitiesOfClass(Player::class.java, box).any { player ->
            player.containerMenu !is InventoryMenu
        }
    }

    private fun zeroOpenCount(be: BlockEntity?) {
        if (be == null) return
        val counterField = be.javaClass.declaredFields.firstOrNull { field ->
            ContainerOpenersCounter::class.java.isAssignableFrom(field.type)
        } ?: return
        counterField.isAccessible = true
        val counter = counterField.get(be) ?: return
        var type: Class<*>? = counter.javaClass
        while (type != null) {
            val count = type.declaredFields.firstOrNull { field ->
                field.type == Int::class.javaPrimitiveType && !java.lang.reflect.Modifier.isStatic(field.modifiers)
            }
            if (count != null) {
                count.isAccessible = true
                count.setInt(counter, 0)
                return
            }
            type = type.superclass
        }
    }

    fun looksLikeContainerId(block: net.minecraft.world.level.block.Block): Boolean {
        val path = BuiltInRegistries.BLOCK.getKey(block).path
        return path.contains("chest") || path.contains("barrel") || path.contains("shulker") ||
            path.contains("crate") || path.contains("cabinet") || path.contains("basket")
    }
}
