package com.reallyusefulribbits.mod.loader.fabric.mixin;

import com.reallyusefulribbits.mod.attach.PlayerVisualData;
import com.reallyusefulribbits.mod.attach.RibbitWorkData;
import com.reallyusefulribbits.mod.attach.VisualCarrier;
import com.reallyusefulribbits.mod.attach.WorkCarrier;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Entity.class)
public abstract class FabricCarrierMixin implements WorkCarrier, VisualCarrier {
    @Unique
    RibbitWorkData reallyusefulribbits$work;

    @Unique
    PlayerVisualData reallyusefulribbits$visual;

    @Override
    public RibbitWorkData reallyusefulribbits$work() {
        if (reallyusefulribbits$work == null) {
            reallyusefulribbits$work = new RibbitWorkData();
        }
        return reallyusefulribbits$work;
    }

    @Override
    public PlayerVisualData reallyusefulribbits$visual() {
        if (reallyusefulribbits$visual == null) {
            reallyusefulribbits$visual = new PlayerVisualData();
        }
        return reallyusefulribbits$visual;
    }
}
