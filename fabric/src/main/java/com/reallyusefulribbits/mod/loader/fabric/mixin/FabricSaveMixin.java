package com.reallyusefulribbits.mod.loader.fabric.mixin;

import com.reallyusefulribbits.mod.attach.PlayerVisualData;
import com.reallyusefulribbits.mod.attach.RibbitWorkData;
import com.reallyusefulribbits.mod.attach.VisualCarrier;
import com.reallyusefulribbits.mod.attach.WorkCarrier;
import com.yungnickyoung.minecraft.ribbits.entity.RibbitEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class FabricSaveMixin {
    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void reallyusefulribbits$save(CompoundTag tag, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self instanceof RibbitEntity) {
            RibbitWorkData work = ((WorkCarrier) self).reallyusefulribbits$work();
            tag.put("ReallyUsefulRibbitsWork", work.save(self.registryAccess()));
        }
        if (self instanceof Player) {
            PlayerVisualData visual = ((VisualCarrier) self).reallyusefulribbits$visual();
            tag.put("ReallyUsefulRibbitsVisual", visual.write());
        }
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void reallyusefulribbits$load(CompoundTag tag, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self instanceof RibbitEntity && tag.contains("ReallyUsefulRibbitsWork")) {
            ((FabricCarrierMixin) (Object) self).reallyusefulribbits$work = RibbitWorkData.load(
                tag.getCompound("ReallyUsefulRibbitsWork"),
                self.registryAccess()
            );
        }
        if (self instanceof Player && tag.contains("ReallyUsefulRibbitsVisual")) {
            ((FabricCarrierMixin) (Object) self).reallyusefulribbits$visual = PlayerVisualData.load(
                tag.getCompound("ReallyUsefulRibbitsVisual")
            );
        }
    }
}
