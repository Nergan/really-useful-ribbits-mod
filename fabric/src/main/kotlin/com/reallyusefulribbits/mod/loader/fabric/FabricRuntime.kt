package com.reallyusefulribbits.mod.loader.fabric

import com.reallyusefulribbits.mod.attach.VisualCarrier
import com.reallyusefulribbits.mod.attach.WorkCarrier
import com.reallyusefulribbits.mod.client.ClientVisuals
import com.reallyusefulribbits.mod.loader.ItemSlots
import com.reallyusefulribbits.mod.loader.RuntimeHooks
import com.reallyusefulribbits.mod.network.PlayerVisualPayload
import net.fabricmc.fabric.api.entity.FakePlayer
import net.fabricmc.fabric.api.networking.v1.PlayerLookup
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.Container
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import java.io.File

object FabricRuntime {
    fun install() {
        RuntimeHooks.work = { ribbit -> (ribbit as WorkCarrier).`reallyusefulribbits$work`() }
        RuntimeHooks.visual = { player -> (player as VisualCarrier).`reallyusefulribbits$visual`() }
        RuntimeHooks.sendTrackingAndSelf = { player, payload -> sendTrackingAndSelf(player, payload) }
        RuntimeHooks.sendToPlayer = { player, payload -> ServerPlayNetworking.send(player, payload) }
        RuntimeHooks.itemSlots = { level, pos -> slots(level, pos) }
        RuntimeHooks.containerOpener = { level -> FakePlayer.get(level) }
        RuntimeHooks.patchouliLoaded = { FabricLoader.getInstance().isModLoaded("patchouli") }
        RuntimeHooks.guideReceived = { player -> RuntimeHooks.visual(player).receivedGuide }
        RuntimeHooks.markGuideReceived = { player -> RuntimeHooks.visual(player).receivedGuide = true }
        loadConfig()
    }

    private fun sendTrackingAndSelf(player: ServerPlayer, payload: PlayerVisualPayload) {
        ServerPlayNetworking.send(player, payload)
        for (watcher in PlayerLookup.tracking(player)) {
            ServerPlayNetworking.send(watcher, payload)
        }
    }

    private fun loadConfig() {
        val file = File(FabricLoader.getInstance().configDir.toFile(), "reallyusefulribbits-server.properties")
        var radius = com.reallyusefulribbits.mod.logic.ScanRadius.DEFAULT
        var chaos = 25
        if (file.isFile) {
            file.readLines().forEach { line ->
                val parts = line.split("=", limit = 2)
                if (parts.size != 2) return@forEach
                when (parts[0].trim()) {
                    "scan_radius" -> radius = parts[1].trim().toIntOrNull() ?: radius
                    "chaos_level" -> chaos = parts[1].trim().toIntOrNull() ?: chaos
                }
            }
        } else {
            file.parentFile.mkdirs()
            file.writeText("scan_radius=$radius\nchaos_level=$chaos\n")
        }
        val savedRadius = radius
        val savedChaos = chaos
        RuntimeHooks.scanRadius = { com.reallyusefulribbits.mod.logic.ScanRadius.clamp(savedRadius) }
        RuntimeHooks.chaosLevel = { savedChaos.coerceIn(0, 100) }
    }

    private fun slots(level: Level, pos: BlockPos): ItemSlots? {
        val blockEntity = level.getBlockEntity(pos)
        val container = blockEntity as? Container ?: return null
        return ItemSlots(
            slots = container.containerSize,
            getStack = { slot -> container.getItem(slot) },
            extract = { slot, amount -> container.removeItem(slot, amount) },
            insertStacked = { stack -> insertStacked(container, stack) },
        )
    }

    private fun insertStacked(container: Container, stack: ItemStack): ItemStack {
        if (stack.isEmpty) return ItemStack.EMPTY
        val remaining = stack.copy()
        for (slot in 0 until container.containerSize) {
            if (remaining.isEmpty) break
            val existing = container.getItem(slot)
            if (existing.isEmpty || !ItemStack.isSameItemSameComponents(existing, remaining)) continue
            val room = existing.maxStackSize - existing.count
            if (room <= 0) continue
            val moved = minOf(room, remaining.count)
            existing.grow(moved)
            remaining.shrink(moved)
            container.setItem(slot, existing)
        }
        for (slot in 0 until container.containerSize) {
            if (remaining.isEmpty) break
            if (!container.getItem(slot).isEmpty) continue
            val moved = remaining.copy()
            if (moved.count > moved.maxStackSize) moved.count = moved.maxStackSize
            container.setItem(slot, moved)
            remaining.shrink(moved.count)
        }
        return remaining
    }

    fun onClientPayload(payload: PlayerVisualPayload) {
        ClientVisuals.set(payload.playerId, payload.upsideDown, payload.morphId)
    }
}
