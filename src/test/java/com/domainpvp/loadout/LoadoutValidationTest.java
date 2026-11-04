package com.domainpvp.loadout;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class LoadoutValidationTest {

    @Test
    public void testPresetsAreValid() {
        LoadoutManager manager = LoadoutManager.getInstance();

        for (LoadoutPreset preset : LoadoutPreset.values()) {
            Loadout loadout = manager.getPreset(preset.name());
            assertNotNull(loadout, "Preset should exist: " + preset);
            LoadoutManager.ValidationResult result = manager.validate(loadout);
            assertTrue(result.valid(), "Preset " + preset + " failed validation: " + result.message());
        }
    }

    @Test
    public void testBannedItemRejection() {
        LoadoutManager manager = LoadoutManager.getInstance();
        Loadout loadout = LoadoutPreset.createDefault(LoadoutPreset.CLASSIC);

        // Inject bedrock into hotbar slot 0
        loadout.setHotbarSlot(0, LoadoutItem.simple("minecraft:bedrock", 1));

        LoadoutManager.ValidationResult result = manager.validate(loadout);
        assertFalse(result.valid());
        assertTrue(result.message().contains("Banned item"));
    }

    @Test
    public void testIllegalEnchantmentLevelRejection() {
        LoadoutManager manager = LoadoutManager.getInstance();
        Loadout loadout = LoadoutPreset.createDefault(LoadoutPreset.CLASSIC);

        // Inject illegal Sharpness 255
        loadout.setHotbarSlot(0, LoadoutItem.enchanted("minecraft:diamond_sword", 1, "minecraft:sharpness", 255));

        LoadoutManager.ValidationResult result = manager.validate(loadout);
        assertFalse(result.valid());
        assertTrue(result.message().contains("Illegal enchantment level"));
    }

    @Test
    public void testArmorStackSizeMustBeOne() {
        LoadoutManager manager = LoadoutManager.getInstance();
        Loadout loadout = LoadoutPreset.createDefault(LoadoutPreset.CLASSIC);

        // Set helmet stack to 5
        loadout.setHelmet(LoadoutItem.simple("minecraft:diamond_helmet", 5));

        LoadoutManager.ValidationResult result = manager.validate(loadout);
        assertFalse(result.valid());
        assertTrue(result.message().contains("Armor stack size"));
    }
}

