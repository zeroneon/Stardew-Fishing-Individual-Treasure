package com.zeroneon.stardewfishingindividualtreasure;

import com.mojang.logging.LogUtils;
import com.zeroneon.stardewfishingindividualtreasure.mixin.FishingHookLuckAccessor;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

public final class TreasureLootGenerator {
    private static final Set<ResourceLocation> WARNED = ConcurrentHashMap.newKeySet();

    private TreasureLootGenerator() {}

    public static void clearWarnings() {
        WARNED.clear();
    }

    public static List<ItemStack> generate(ServerLevel level, ItemStack caught, boolean treasureIsGolden) {
        return generate(level, caught, treasureIsGolden, null, null, ItemStack.EMPTY);
    }

    public static List<ItemStack> generate(ServerLevel level, ItemStack caught, boolean treasureIsGolden, ServerPlayer player, FishingHook hook, ItemStack rod) {
        var tables = FishTreasureMappings.find(caught);
        ResourceLocation selectedLootTable;
        ResourceLocation fallbackLootTable = null;
        if (tables != null) {
            if (treasureIsGolden)
            {
                selectedLootTable = tables.golden();
                fallbackLootTable = tables.normal();
            } else {
                selectedLootTable = tables.normal();
            }
        } else {
            selectedLootTable = ResourceLocation.fromNamespaceAndPath("stardew_fishing",
                    level.dimension() == Level.NETHER ? "treasure_chest_nether" : "treasure_chest");
        }

        LootTable lootTable = lookup(level, selectedLootTable);
        if (lootTable == null && fallbackLootTable != null) {
            lootTable = lookup(level, fallbackLootTable);
        }
        if (lootTable == null) {
            return List.of();
        }
        return lootTable.getRandomItems(createLootParams(level, player, hook, rod));
    }

    private static LootParams createLootParams(ServerLevel level, ServerPlayer player, FishingHook hook, ItemStack rod) {
        if (hook == null) {
            return new LootParams.Builder(level).create(LootContextParamSets.EMPTY);
        }

        return new LootParams.Builder(level)
                .withParameter(LootContextParams.ORIGIN, hook.position())
                .withParameter(LootContextParams.THIS_ENTITY, hook)
                .withParameter(LootContextParams.TOOL, rod)
                .withOptionalParameter(LootContextParams.ATTACKING_ENTITY, player)
                .withLuck(((FishingHookLuckAccessor) hook).getFishingLuck() + (player == null ? 0.0F : player.getLuck()))
                .create(LootContextParamSets.FISHING);
    }

    private static LootTable lookup(ServerLevel level, ResourceLocation id) {
        var key = ResourceKey.create(Registries.LOOT_TABLE, id);
        var result = level.getServer().reloadableRegistries().lookup().lookupOrThrow(Registries.LOOT_TABLE).get(key);
        if (result.isPresent()) {
            return result.get().value();
        }
        if (WARNED.add(id)) {
            LogUtils.getLogger().warn("Missing treasure loot table {}", id);
        }
        return null;
    }
}
