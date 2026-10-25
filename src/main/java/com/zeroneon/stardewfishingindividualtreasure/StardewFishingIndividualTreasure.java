package com.zeroneon.stardewfishingindividualtreasure;

import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;

@Mod(StardewFishingIndividualTreasure.MOD_ID)
public final class StardewFishingIndividualTreasure {
    public static final String MOD_ID = "stardew_fishing_individual_treasure";

    public StardewFishingIndividualTreasure() {
        NeoForge.EVENT_BUS.addListener(this::reload);
    }

    private void reload(AddReloadListenerEvent event) {
        event.addListener(new FishTreasureMappings());
    }
}
