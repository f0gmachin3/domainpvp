package com.domainpvp.loadout;

/**
 * Predefined competitive PvP presets corresponding to the 6 official Domain kits.
 */
public enum LoadoutPreset {
    SWORD_PVP("Sword PvP", "Diamond armor, sword, rod, golden apples, pearls, steak"),
    AXE_PVP("Axe PvP", "Diamond armor, axe, sword, shield, bow, arrows, apples, steak"),
    UHC_PVP("UHC PvP", "Diamond armor, sword, axe, bow, arrows, water, lava, cobwebs, planks, rod, steak"),
    SMP_PVP("SMP / Netherite PvP", "Netherite armor & weapons, shield, totem, pearls, apples, god apples, xp, steak"),
    MACE_PVP("Mace PvP", "Netherite armor, mace, wind charges, pearls, shield, totem, apples, rockets, water"),
    POT_PVP("Pot PvP", "Diamond armor, sword, 36 Splash Healing II potions, pearls, golden carrots");

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
            case SWORD_PVP -> {
                // Armor: Diamond Set
                loadout.setHelmet(LoadoutItem.simple("minecraft:diamond_helmet", 1));
                loadout.setChestplate(LoadoutItem.simple("minecraft:diamond_chestplate", 1));
                loadout.setLeggings(LoadoutItem.simple("minecraft:diamond_leggings", 1));
                loadout.setBoots(LoadoutItem.simple("minecraft:diamond_boots", 1));

                // Inventory: Diamond Sword x1, Fishing Rod x1, Golden Apples x8, Ender Pearls x16, Steak x64
                loadout.setHotbarSlot(0, LoadoutItem.simple("minecraft:diamond_sword", 1));
                loadout.setHotbarSlot(1, LoadoutItem.simple("minecraft:fishing_rod", 1));
                loadout.setHotbarSlot(2, LoadoutItem.simple("minecraft:golden_apple", 8));
                loadout.setHotbarSlot(3, LoadoutItem.simple("minecraft:ender_pearl", 16));
                loadout.setHotbarSlot(4, LoadoutItem.simple("minecraft:cooked_beef", 64));
            }
            case AXE_PVP -> {
                // Armor: Diamond Set
                loadout.setHelmet(LoadoutItem.simple("minecraft:diamond_helmet", 1));
                loadout.setChestplate(LoadoutItem.simple("minecraft:diamond_chestplate", 1));
                loadout.setLeggings(LoadoutItem.simple("minecraft:diamond_leggings", 1));
                loadout.setBoots(LoadoutItem.simple("minecraft:diamond_boots", 1));
                loadout.setOffhand(LoadoutItem.simple("minecraft:shield", 1));

                // Inventory: Diamond Axe x1, Diamond Sword x1, Shield x1, Bow x1, Arrows x32, Golden Apples x8, Steak x32
                loadout.setHotbarSlot(0, LoadoutItem.simple("minecraft:diamond_axe", 1));
                loadout.setHotbarSlot(1, LoadoutItem.simple("minecraft:diamond_sword", 1));
                loadout.setHotbarSlot(2, LoadoutItem.simple("minecraft:bow", 1));
                loadout.setHotbarSlot(3, LoadoutItem.simple("minecraft:golden_apple", 8));
                loadout.setHotbarSlot(4, LoadoutItem.simple("minecraft:cooked_beef", 32));
                loadout.setInventorySlot(0, LoadoutItem.simple("minecraft:arrow", 32));
            }
            case UHC_PVP -> {
                // Armor: Diamond Set
                loadout.setHelmet(LoadoutItem.simple("minecraft:diamond_helmet", 1));
                loadout.setChestplate(LoadoutItem.simple("minecraft:diamond_chestplate", 1));
                loadout.setLeggings(LoadoutItem.simple("minecraft:diamond_leggings", 1));
                loadout.setBoots(LoadoutItem.simple("minecraft:diamond_boots", 1));

                // Inventory: Diamond Sword x1, Diamond Axe x1, Bow x1, Arrows x32, Water Bucket x1, Lava Bucket x1, Golden Apples x8, Cobwebs x8, Oak Planks x64, Fishing Rod x1, Steak x32
                loadout.setHotbarSlot(0, LoadoutItem.simple("minecraft:diamond_sword", 1));
                loadout.setHotbarSlot(1, LoadoutItem.simple("minecraft:diamond_axe", 1));
                loadout.setHotbarSlot(2, LoadoutItem.simple("minecraft:bow", 1));
                loadout.setHotbarSlot(3, LoadoutItem.simple("minecraft:fishing_rod", 1));
                loadout.setHotbarSlot(4, LoadoutItem.simple("minecraft:golden_apple", 8));
                loadout.setHotbarSlot(5, LoadoutItem.simple("minecraft:water_bucket", 1));
                loadout.setHotbarSlot(6, LoadoutItem.simple("minecraft:lava_bucket", 1));
                loadout.setHotbarSlot(7, LoadoutItem.simple("minecraft:cobweb", 8));
                loadout.setHotbarSlot(8, LoadoutItem.simple("minecraft:oak_planks", 64));
                loadout.setInventorySlot(0, LoadoutItem.simple("minecraft:arrow", 32));
                loadout.setInventorySlot(1, LoadoutItem.simple("minecraft:cooked_beef", 32));
            }
            case SMP_PVP -> {
                // Armor: Netherite Set
                loadout.setHelmet(LoadoutItem.simple("minecraft:netherite_helmet", 1));
                loadout.setChestplate(LoadoutItem.simple("minecraft:netherite_chestplate", 1));
                loadout.setLeggings(LoadoutItem.simple("minecraft:netherite_leggings", 1));
                loadout.setBoots(LoadoutItem.simple("minecraft:netherite_boots", 1));
                loadout.setOffhand(LoadoutItem.simple("minecraft:totem_of_undying", 1));

                // Inventory: Netherite Sword x1, Netherite Axe x1, Shield x1, Totem of Undying x1, Ender Pearls x16, Golden Apples x16, Enchanted Golden Apples x4, Experience Bottles x64, Steak x32
                loadout.setHotbarSlot(0, LoadoutItem.simple("minecraft:netherite_sword", 1));
                loadout.setHotbarSlot(1, LoadoutItem.simple("minecraft:netherite_axe", 1));
                loadout.setHotbarSlot(2, LoadoutItem.simple("minecraft:shield", 1));
                loadout.setHotbarSlot(3, LoadoutItem.simple("minecraft:golden_apple", 16));
                loadout.setHotbarSlot(4, LoadoutItem.simple("minecraft:enchanted_golden_apple", 4));
                loadout.setHotbarSlot(5, LoadoutItem.simple("minecraft:ender_pearl", 16));
                loadout.setHotbarSlot(6, LoadoutItem.simple("minecraft:cooked_beef", 32));
                loadout.setHotbarSlot(7, LoadoutItem.simple("minecraft:experience_bottle", 64));
            }
            case MACE_PVP -> {
                // Armor: Netherite Set
                loadout.setHelmet(LoadoutItem.simple("minecraft:netherite_helmet", 1));
                loadout.setChestplate(LoadoutItem.simple("minecraft:netherite_chestplate", 1));
                loadout.setLeggings(LoadoutItem.simple("minecraft:netherite_leggings", 1));
                loadout.setBoots(LoadoutItem.simple("minecraft:netherite_boots", 1));
                loadout.setOffhand(LoadoutItem.simple("minecraft:shield", 1));

                // Inventory: Mace x1, Wind Charges x32, Ender Pearls x16, Shield x1, Totem of Undying x1, Golden Apples x16, Firework Rockets x64, Water Bucket x1
                loadout.setHotbarSlot(0, LoadoutItem.simple("minecraft:mace", 1));
                loadout.setHotbarSlot(1, LoadoutItem.simple("minecraft:wind_charge", 32));
                loadout.setHotbarSlot(2, LoadoutItem.simple("minecraft:ender_pearl", 16));
                loadout.setHotbarSlot(3, LoadoutItem.simple("minecraft:totem_of_undying", 1));
                loadout.setHotbarSlot(4, LoadoutItem.simple("minecraft:golden_apple", 16));
                loadout.setHotbarSlot(5, LoadoutItem.simple("minecraft:firework_rocket", 64));
                loadout.setHotbarSlot(6, LoadoutItem.simple("minecraft:water_bucket", 1));
            }
            case POT_PVP -> {
                // Armor: Diamond Set
                loadout.setHelmet(LoadoutItem.simple("minecraft:diamond_helmet", 1));
                loadout.setChestplate(LoadoutItem.simple("minecraft:diamond_chestplate", 1));
                loadout.setLeggings(LoadoutItem.simple("minecraft:diamond_leggings", 1));
                loadout.setBoots(LoadoutItem.simple("minecraft:diamond_boots", 1));
                loadout.setOffhand(LoadoutItem.simple("minecraft:splash_potion", 1));

                // Inventory: Diamond Sword x1, Splash Potion of Healing II x36, Ender Pearls x16, Golden Carrots x64
                loadout.setHotbarSlot(0, LoadoutItem.simple("minecraft:diamond_sword", 1));
                loadout.setHotbarSlot(1, LoadoutItem.simple("minecraft:ender_pearl", 16));
                loadout.setHotbarSlot(2, LoadoutItem.simple("minecraft:golden_carrot", 64));
                for (int h = 3; h < 9; h++) {
                    loadout.setHotbarSlot(h, LoadoutItem.simple("minecraft:splash_potion", 1));
                }
                for (int inv = 0; inv < 27; inv++) {
                    loadout.setInventorySlot(inv, LoadoutItem.simple("minecraft:splash_potion", 1));
                }
                // (1 offhand + 6 hotbar + 27 main inv + 2 additional in inventory slots 25, 26 = 36 total)
            }
        }
        return loadout;
    }
}
