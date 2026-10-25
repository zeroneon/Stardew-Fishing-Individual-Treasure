package com.zeroneon.stardewfishingindividualtreasure;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;

public final class FishTreasureMappings extends SimpleJsonResourceReloadListener {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static volatile Map<Item, TreasureTables> mappings = Map.of();

    public record TreasureTables(ResourceLocation normal, ResourceLocation golden) {}

    public FishTreasureMappings() {
        super(new Gson(), "treasure_categories");
    }

    public static TreasureTables find(ItemStack caught) {
        return mappings.get(caught.getItem());
    }

    public static int size() {
        return mappings.size();
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager resources, ProfilerFiller profiler) {
        Map<Item, Set<TreasureTables>> candidates = new HashMap<>();
        files.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
            ResourceLocation id = entry.getKey();
            String path = id.getPath();
            if (!path.endsWith("_list")) {
                return;
            }

            String group = path.substring(0, path.length() - "_list".length());
            if (group.isEmpty() || group.endsWith("/")) {
                LOGGER.error("Empty treasure group in {}", id);
                return;
            }

            JsonElement root = entry.getValue();
            if (!root.isJsonArray()) {
                LOGGER.error("Treasure list {} must be a JSON array; ignoring file", id);
                return;
            }

            var tables = new TreasureTables(
                    ResourceLocation.fromNamespaceAndPath(id.getNamespace(), group + "_treasure"),
                    ResourceLocation.fromNamespaceAndPath(id.getNamespace(), group + "_golden_treasure"));
            for (JsonElement element : root.getAsJsonArray()) {
                if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
                    LOGGER.error("Non-string item in treasure list {}: {}; ignoring entry", id, element);
                    continue;
                }
                String value = element.getAsString();
                ResourceLocation itemId = ResourceLocation.tryParse(value);
                if (itemId == null) {
                    LOGGER.error("Invalid item ID {} in {}; ignoring entry", value, id);
                    continue;
                }

                var item = BuiltInRegistries.ITEM.getOptional(itemId);
                if (item.isEmpty()) {
                    LOGGER.warn("Ignoring unknown item {} in treasure list {}", itemId, id);
                    continue;
                }
                candidates.computeIfAbsent(item.get(), ignored -> new HashSet<>()).add(tables);
            }
        });

        Map<Item, TreasureTables> resolved = new HashMap<>();
        candidates.forEach((item, tables) -> {
            if (tables.size() == 1) {
                resolved.put(item, tables.iterator().next());
            } else {
                LOGGER.error("Item {} appears in multiple treasure groups {}; using original Stardew table",
                        BuiltInRegistries.ITEM.getKey(item), tables);
            }
        });

        mappings = Map.copyOf(resolved);
        TreasureLootGenerator.clearWarnings();
        LOGGER.info("Stardew Fishing: Individual Treasure loaded {} item mappings", mappings.size());
    }
}
