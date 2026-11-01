package com.reallyusefulribbits.mod.loader.fabric.mixin

import com.reallyusefulribbits.mod.attach.PlayerVisualData
import com.reallyusefulribbits.mod.attach.RibbitWorkData
import com.reallyusefulribbits.mod.attach.VisualCarrier
import com.reallyusefulribbits.mod.attach.WorkCarrier
import com.yungnickyoung.minecraft.ribbits.entity.RibbitEntity
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@Mixin(LivingEntity::class)
abstract class FabricSaveMixin {
    @Inject(method = ["addAdditionalSaveData"], at = [At("TAIL")])
    private fun `reallyusefulribbits$save`(tag: CompoundTag, ci: CallbackInfo) {
        val self = (this as Any) as LivingEntity
        if (self is RibbitEntity) {
            val work = (self as WorkCarrier).`reallyusefulribbits$work`()
            tag.put("ReallyUsefulRibbitsWork", work.save(self.registryAccess()))
        }
        if (self is Player) {
            val visual = (self as VisualCarrier).`reallyusefulribbits$visual`()
            tag.put("ReallyUsefulRibbitsVisual", visual.write())
        }
    }

    @Inject(method = ["readAdditionalSaveData"], at = [At("TAIL")])
    private fun `reallyusefulribbits$load`(tag: CompoundTag, ci: CallbackInfo) {
        val self = (this as Any) as LivingEntity
        if (self is RibbitEntity && tag.contains("ReallyUsefulRibbitsWork")) {
            (self as WorkCarrier).`reallyusefulribbits$setWork`(
                RibbitWorkData.load(tag.getCompound("ReallyUsefulRibbitsWork"), self.registryAccess()),
            )
        }
        if (self is Player && tag.contains("ReallyUsefulRibbitsVisual")) {
            (self as VisualCarrier).`reallyusefulribbits$setVisual`(
                PlayerVisualData.load(tag.getCompound("ReallyUsefulRibbitsVisual")),
            )
        }
    }
}
