package com.domainpvp.loadout;

import java.util.HashMap;
import java.util.Map;

/**
 * Predefined competitive PvP presets.
 */
public enum LoadoutPreset {
    CLASSIC("Classic PvP", "Standard 1.8/1.9 balanced diamond gear with golden apples"),
    SWORD("Sword PvP", "Netherite sword dual with shield and splash potions"),
    AXE("Axe PvP", "Shield-disabling axe combat with high burst damage"),
    ARCHER("Archer PvP", "Ranged bow duel with mobility enhancement"),
    UHC("UHC PvP", "Ultra Hardcore equipment: water buckets, rods, and golden apples"),
    CUSTOM("Custom Loadout", "Player-tailored combat kit validated server-side");

    private final String displayName;
    private final String description;

    LoadoutPreset(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public static Loadout createDefault(LoadoutPreset preset) {
        Loadout loadout = new Loadout(preset.name());
        switch (preset) {
            case CLASSIC -> {
                loadout.setHelmet(LoadoutItem.enchanted("minecraft:diamond_helmet", 1, "minecraft:protection", 2));
                loadout.setChestplate(LoadoutItem.enchanted("minecraft:diamond_chestplate", 1, "minecraft:protection", 2));
                loadout.setLeggings(LoadoutItem.enchanted("minecraft:diamond_leggings", 1, "minecraft:protection", 2));
                loadout.setBoots(LoadoutItem.enchanted("minecraft:diamond_boots", 1, "minecraft:protection", 2));
                loadout.setOffhand(LoadoutItem.simple("minecraft:shield", 1));

                loadout.setHotbarSlot(0, LoadoutItem.enchanted("minecraft:diamond_sword", 1, "minecraft:sharpness", 3));
                loadout.setHotbarSlot(1, LoadoutItem.simple("minecraft:golden_apple", 16));
                loadout.setHotbarSlot(2, LoadoutItem.enchanted("minecraft:bow", 1, "minecraft:power", 3));
                loadout.setHotbarSlot(3, LoadoutItem.simple("minecraft:cooked_beef", 64));
                loadout.setHotbarSlot(4, LoadoutItem.simple("minecraft:water_bucket", 1));
                loadout.setInventorySlot(0, LoadoutItem.simple("minecraft:arrow", 64));
            }
            case SWORD -> {
                loadout.setHelmet(LoadoutItem.enchanted("minecraft:netherite_helmet", 1, "minecraft:protection", 3));
                loadout.setChestplate(LoadoutItem.enchanted("minecraft:netherite_chestplate", 1, "minecraft:protection", 3));
                loadout.setLeggings(LoadoutItem.enchanted("minecraft:netherite_leggings", 1, "minecraft:protection", 3));
                loadout.setBoots(LoadoutItem.enchanted("minecraft:netherite_boots", 1, "minecraft:protection", 3));
                loadout.setOffhand(LoadoutItem.simple("minecraft:shield", 1));

                loadout.setHotbarSlot(0, LoadoutItem.enchanted("minecraft:netherite_sword", 1, "minecraft:sharpness", 4));
                loadout.setHotbarSlot(1, LoadoutItem.simple("minecraft:golden_apple", 8));
                loadout.setHotbarSlot(2, LoadoutItem.simple("minecraft:splash_potion", 2)); // Healing II
                loadout.setHotbarSlot(3, LoadoutItem.simple("minecraft:potion", 1)); // Speed II
                loadout.setHotbarSlot(4, LoadoutItem.simple("minecraft:cooked_beef", 32));
            }
            case AXE -> {
                loadout.setHelmet(LoadoutItem.enchanted("minecraft:diamond_helmet", 1, "minecraft:protection", 2));
                loadout.setChestplate(LoadoutItem.enchanted("minecraft:diamond_chestplate", 1, "minecraft:protection", 2));
                loadout.setLeggings(LoadoutItem.enchanted("minecraft:diamond_leggings", 1, "minecraft:protection", 2));
                loadout.setBoots(LoadoutItem.enchanted("minecraft:diamond_boots", 1, "minecraft:protection", 2));
                loadout.setOffhand(LoadoutItem.enchanted("minecraft:shield", 1, "minecraft:unbreaking", 3));

                loadout.setHotbarSlot(0, LoadoutItem.enchanted("minecraft:diamond_axe", 1, "minecraft:sharpness", 4));
                loadout.setHotbarSlot(1, LoadoutItem.simple("minecraft:crossbow", 1));
                loadout.setHotbarSlot(2, LoadoutItem.simple("minecraft:cooked_porkchop", 32));
                loadout.setHotbarSlot(3, LoadoutItem.simple("minecraft:golden_apple", 4));
                loadout.setInventorySlot(0, LoadoutItem.simple("minecraft:arrow", 32));
            }
            case ARCHER -> {
                loadout.setHelmet(LoadoutItem.enchanted("minecraft:chainmail_helmet", 1, "minecraft:protection", 1));
                loadout.setChestplate(LoadoutItem.enchanted("minecraft:chainmail_chestplate", 1, "minecraft:protection", 1));
                loadout.setLeggings(LoadoutItem.enchanted("minecraft:chainmail_leggings", 1, "minecraft:protection", 1));
                loadout.setBoots(LoadoutItem.enchanted("minecraft:chainmail_boots", 1, "minecraft:protection", 1));

                Map<String, Integer> bowEnchs = new HashMap<>();
                bowEnchs.put("minecraft:power", 4);
                bowEnchs.put("minecraft:flame", 1);
                bowEnchs.put("minecraft:infinity", 1);

                loadout.setHotbarSlot(0, new LoadoutItem("minecraft:bow", 1, bowEnchs));
                loadout.setHotbarSlot(1, LoadoutItem.simple("minecraft:iron_sword", 1));
                loadout.setHotbarSlot(2, LoadoutItem.simple("minecraft:cooked_beef", 32));
                loadout.setHotbarSlot(3, LoadoutItem.simple("minecraft:golden_apple", 4));
                loadout.setInventorySlot(0, LoadoutItem.simple("minecraft:arrow", 1));
            }
            case UHC -> {
                loadout.setHelmet(LoadoutItem.enchanted("minecraft:iron_helmet", 1, "minecraft:protection", 2));
                loadout.setChestplate(LoadoutItem.enchanted("minecraft:iron_chestplate", 1, "minecraft:protection", 2));
                loadout.setLeggings(LoadoutItem.enchanted("minecraft:iron_leggings", 1, "minecraft:protection", 2));
                loadout.setBoots(LoadoutItem.enchanted("minecraft:iron_boots", 1, "minecraft:protection", 2));
                loadout.setOffhand(LoadoutItem.simple("minecraft:shield", 1));

                loadout.setHotbarSlot(0, LoadoutItem.enchanted("minecraft:iron_sword", 1, "minecraft:sharpness", 2));
                loadout.setHotbarSlot(1, LoadoutItem.simple("minecraft:iron_axe", 1));
                loadout.setHotbarSlot(2, LoadoutItem.simple("minecraft:bow", 1));
                loadout.setHotbarSlot(3, LoadoutItem.simple("minecraft:water_bucket", 1));
                loadout.setHotbarSlot(4, LoadoutItem.simple("minecraft:lava_bucket", 1));
                loadout.setHotbarSlot(5, LoadoutItem.simple("minecraft:golden_apple", 6));
                loadout.setHotbarSlot(6, LoadoutItem.simple("minecraft:fishing_rod", 1));
                loadout.setHotbarSlot(7, LoadoutItem.simple("minecraft:cobblestone", 64));
                loadout.setHotbarSlot(8, LoadoutItem.simple("minecraft:cooked_beef", 32));
                loadout.setInventorySlot(0, LoadoutItem.simple("minecraft:arrow", 32));
            }
            case CUSTOM -> {
                // Starts empty or copy of Classic
                return createDefault(CLASSIC);
            }
        }
        return loadout;
    }
}

