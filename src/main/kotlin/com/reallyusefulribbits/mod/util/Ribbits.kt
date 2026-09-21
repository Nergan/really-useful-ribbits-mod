package com.reallyusefulribbits.mod.util

import com.reallyusefulribbits.mod.attach.ModAttachments
import com.reallyusefulribbits.mod.attach.RibbitWorkData
import com.reallyusefulribbits.mod.logic.ProfessionKind
import com.yungnickyoung.minecraft.ribbits.entity.RibbitEntity
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundSource
import net.minecraft.world.entity.Entity
import net.minecraft.world.phys.Vec3

fun RibbitEntity.work(): RibbitWorkData = getData(ModAttachments.WORK.get())

fun RibbitEntity.professionKind(): ProfessionKind =
    ProfessionKind.fromId(ribbitData.profession.id.path)

fun ServerLevel.playOnce(pos: BlockPos, sound: SoundEvent, source: SoundSource = SoundSource.NEUTRAL, volume: Float = 1f, pitch: Float = 1f) {
    playSound(null, pos, sound, source, volume, pitch)
}

fun ServerLevel.playOnce(at: Entity, sound: SoundEvent, source: SoundSource = SoundSource.NEUTRAL, volume: Float = 1f, pitch: Float = 1f) {
    playSound(null, at.blockPosition(), sound, source, volume, pitch)
}

fun BlockPos.center(): Vec3 = Vec3(x + 0.5, y.toDouble(), z + 0.5)
