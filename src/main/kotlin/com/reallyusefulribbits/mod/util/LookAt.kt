package com.reallyusefulribbits.mod.util

import net.minecraft.core.BlockPos
import net.minecraft.world.entity.Mob
import kotlin.math.atan2

object LookAt {
    fun block(mob: Mob, pos: BlockPos, yOffset: Double = 0.5) {
        val tx = pos.x + 0.5
        val ty = pos.y + yOffset
        val tz = pos.z + 0.5
        val dx = tx - mob.x
        val dz = tz - mob.z
        val yaw = Math.toDegrees(atan2(-dx, dz)).toFloat()
        mob.yRot = yaw
        mob.yHeadRot = yaw
        mob.yBodyRot = yaw
        mob.lookControl.setLookAt(tx, ty, tz, 180f, 180f)
    }
}
