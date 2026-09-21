package com.reallyusefulribbits.mod.util

import net.minecraft.server.MinecraftServer

object DelayedTasks {
    private data class Job(val atTick: Int, val action: () -> Unit)

    private val jobs = ArrayList<Job>()

    fun later(server: MinecraftServer, ticks: Int, action: () -> Unit) {
        jobs += Job(server.tickCount + ticks.coerceAtLeast(1), action)
    }

    fun tick(server: MinecraftServer) {
        if (jobs.isEmpty()) return
        val now = server.tickCount
        val due = jobs.filter { it.atTick <= now }
        jobs.removeAll(due.toSet())
        for (job in due) {
            runCatching { job.action() }
        }
    }
}
