package com.zeroneon.stardewfishingindividualtreasure.mixin;

import net.minecraft.world.entity.projectile.FishingHook;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(FishingHook.class)
public interface FishingHookLuckAccessor {
    @Accessor("luck")
    int getFishingLuck();
}
