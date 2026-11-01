package com.reallyusefulribbits.mod.mixin

import net.minecraft.world.entity.Mob
import net.minecraft.world.entity.ai.navigation.PathNavigation
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.gen.Accessor

@Mixin(PathNavigation::class)
interface PathNavigationAccessor {
    @Accessor("mob")
    fun rurMob(): Mob
}
