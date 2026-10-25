package com.zeroneon.stardewfishingindividualtreasure.mixin;

import com.bonker.stardewfishing.server.data.FishingHookAttachment;
import com.zeroneon.stardewfishingindividualtreasure.TreasureLootGenerator;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "com.bonker.stardewfishing.common.FishingHookLogic", remap = false)
abstract class FishingHookLogicMixin {
    @Redirect(
            method = "giveRewards(Lnet/minecraft/server/level/ServerPlayer;DZLnet/minecraft/world/item/ItemStack;)V",
            at = @At(value = "INVOKE", target = "Lcom/bonker/stardewfishing/common/FishingHookLogic;getTreasureChestLoot(Lnet/minecraft/server/level/ServerLevel;Z)Ljava/util/List;"))
    private static List<ItemStack> stardewTreasure$generate(ServerLevel level, boolean golden,
            ServerPlayer player, double quality, boolean collectedChest, ItemStack rod) {
        FishingHook hook = player == null ? null : player.fishing;
        var event = hook == null ? null : FishingHookAttachment.get(hook).getEvent();
        ItemStack caught = event == null ? ItemStack.EMPTY : event.getFish();
        return TreasureLootGenerator.generate(level, caught, golden, player, hook, rod);
    }
}
