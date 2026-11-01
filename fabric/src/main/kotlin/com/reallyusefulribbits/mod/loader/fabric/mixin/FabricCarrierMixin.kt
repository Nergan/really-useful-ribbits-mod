package com.reallyusefulribbits.mod.loader.fabric.mixin

import com.reallyusefulribbits.mod.attach.PlayerVisualData
import com.reallyusefulribbits.mod.attach.RibbitWorkData
import com.reallyusefulribbits.mod.attach.VisualCarrier
import com.reallyusefulribbits.mod.attach.WorkCarrier
import net.minecraft.world.entity.Entity
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.Unique

@Mixin(Entity::class)
abstract class FabricCarrierMixin : WorkCarrier, VisualCarrier {
    @Unique
    private var `reallyusefulribbits$workData`: RibbitWorkData? = null

    @Unique
    private var `reallyusefulribbits$visualData`: PlayerVisualData? = null

    override fun `reallyusefulribbits$work`(): RibbitWorkData =
        `reallyusefulribbits$workData` ?: RibbitWorkData().also { `reallyusefulribbits$workData` = it }

    override fun `reallyusefulribbits$setWork`(data: RibbitWorkData) {
        `reallyusefulribbits$workData` = data
    }

    override fun `reallyusefulribbits$visual`(): PlayerVisualData =
        `reallyusefulribbits$visualData` ?: PlayerVisualData().also { `reallyusefulribbits$visualData` = it }

    override fun `reallyusefulribbits$setVisual`(data: PlayerVisualData) {
        `reallyusefulribbits$visualData` = data
    }
}
