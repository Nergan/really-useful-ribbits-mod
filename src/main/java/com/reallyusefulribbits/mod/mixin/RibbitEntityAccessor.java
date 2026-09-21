package com.reallyusefulribbits.mod.mixin;

import com.yungnickyoung.minecraft.ribbits.entity.RibbitEntity;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(RibbitEntity.class)
public interface RibbitEntityAccessor {
    @Accessor("homePosition")
    void rurSetHomePosition(BlockPos pos);
}
