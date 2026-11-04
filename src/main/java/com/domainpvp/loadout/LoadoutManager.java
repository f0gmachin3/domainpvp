package com.domainpvp.loadout;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-authoritative manager and validator for PvP Domain loadouts.
 */
public class LoadoutManager {
    private static final LoadoutManager INSTANCE = new LoadoutManager();

    private final Map<String, Loadout> registeredPresets = new HashMap<>();
    private final Map<UUID, Map<String, Loadout>> playerCustomLoadouts = new ConcurrentHashMap<>();

    private static final Set<String> BANNED_ITEMS = Set.of(
            "minecraft:bedrock",
            "minecraft:barrier",
            "minecraft:command_block",
            "minecraft:chain_command_block",
            "minecraft:repeating_command_block",
            "minecraft:structure_block",
            "minecraft:jigsaw",
            "minecraft:structure_void",
            "minecraft:end_portal_frame",
            "minecraft:spawner"
    );

    private static final Map<String, Integer> MAX_ENCHANTMENT_LEVELS = new HashMap<>();

    static {
        MAX_ENCHANTMENT_LEVELS.put("minecraft:sharpness", 5);
        MAX_ENCHANTMENT_LEVELS.put("minecraft:protection", 4);
        MAX_ENCHANTMENT_LEVELS.put("minecraft:fire_protection", 4);
        MAX_ENCHANTMENT_LEVELS.put("minecraft:blast_protection", 4);
        MAX_ENCHANTMENT_LEVELS.put("minecraft:projectile_protection", 4);
        MAX_ENCHANTMENT_LEVELS.put("minecraft:feather_falling", 4);
        MAX_ENCHANTMENT_LEVELS.put("minecraft:respiration", 3);
        MAX_ENCHANTMENT_LEVELS.put("minecraft:aqua_affinity", 1);
        MAX_ENCHANTMENT_LEVELS.put("minecraft:thorns", 3);
        MAX_ENCHANTMENT_LEVELS.put("minecraft:depth_strider", 3);
        MAX_ENCHANTMENT_LEVELS.put("minecraft:frost_walker", 2);
        MAX_ENCHANTMENT_LEVELS.put("minecraft:soul_speed", 3);
        MAX_ENCHANTMENT_LEVELS.put("minecraft:swift_sneak", 3);
        MAX_ENCHANTMENT_LEVELS.put("minecraft:unbreaking", 3);
        MAX_ENCHANTMENT_LEVELS.put("minecraft:efficiency", 5);
        MAX_ENCHANTMENT_LEVELS.put("minecraft:silk_touch", 1);
        MAX_ENCHANTMENT_LEVELS.put("minecraft:fortune", 3);
        MAX_ENCHANTMENT_LEVELS.put("minecraft:power", 5);
        MAX_ENCHANTMENT_LEVELS.put("minecraft:punch", 2);
        MAX_ENCHANTMENT_LEVELS.put("minecraft:flame", 1);
        MAX_ENCHANTMENT_LEVELS.put("minecraft:infinity", 1);
        MAX_ENCHANTMENT_LEVELS.put("minecraft:luck_of_the_sea", 3);
        MAX_ENCHANTMENT_LEVELS.put("minecraft:lure", 3);
        MAX_ENCHANTMENT_LEVELS.put("minecraft:loyalty", 3);
        MAX_ENCHANTMENT_LEVELS.put("minecraft:impaling", 5);
        MAX_ENCHANTMENT_LEVELS.put("minecraft:riptide", 3);
        MAX_ENCHANTMENT_LEVELS.put("minecraft:channeling", 1);
        MAX_ENCHANTMENT_LEVELS.put("minecraft:multishot", 1);
        MAX_ENCHANTMENT_LEVELS.put("minecraft:quick_charge", 3);
        MAX_ENCHANTMENT_LEVELS.put("minecraft:piercing", 4);
        MAX_ENCHANTMENT_LEVELS.put("minecraft:mending", 1);
        MAX_ENCHANTMENT_LEVELS.put("minecraft:vanishing_curse", 1);
        MAX_ENCHANTMENT_LEVELS.put("minecraft:binding_curse", 1);
    }

    private LoadoutManager() {
        for (LoadoutPreset preset : LoadoutPreset.values()) {
            registeredPresets.put(preset.name(), LoadoutPreset.createDefault(preset));
        }
    }

    public static LoadoutManager getInstance() {
        return INSTANCE;
    }

    public Loadout getPreset(String name) {
        if (name == null) {
            return registeredPresets.get("CLASSIC");
        }
        Loadout preset = registeredPresets.get(name.toUpperCase(Locale.ROOT));
        return preset != null ? preset : registeredPresets.get("CLASSIC");
    }

    public record ValidationResult(boolean valid, String message) {
        public static ValidationResult success() {
            return new ValidationResult(true, "Loadout is valid.");
        }
        public static ValidationResult fail(String msg) {
            return new ValidationResult(false, msg);
        }
    }

    public ValidationResult validate(Loadout loadout) {
        if (loadout == null) {
            return ValidationResult.fail("Loadout cannot be null.");
        }

        // Validate Armor
        ValidationResult res = validateSlotItem(loadout.getHelmet(), "helmet", true);
        if (!res.valid()) return res;
        res = validateSlotItem(loadout.getChestplate(), "chestplate", true);
        if (!res.valid()) return res;
        res = validateSlotItem(loadout.getLeggings(), "leggings", true);
        if (!res.valid()) return res;
        res = validateSlotItem(loadout.getBoots(), "boots", true);
        if (!res.valid()) return res;
        res = validateSlotItem(loadout.getOffhand(), "offhand", false);
        if (!res.valid()) return res;

        // Validate Hotbar
        for (int i = 0; i < loadout.getHotbar().size(); i++) {
            res = validateSlotItem(loadout.getHotbar().get(i), "hotbar[" + i + "]", false);
            if (!res.valid()) return res;
        }

        // Validate Inventory
        for (int i = 0; i < loadout.getInventory().size(); i++) {
            res = validateSlotItem(loadout.getInventory().get(i), "inventory[" + i + "]", false);
            if (!res.valid()) return res;
        }

        return ValidationResult.success();
    }

    private ValidationResult validateSlotItem(LoadoutItem item, String slotName, boolean isArmorSlot) {
        if (item == null || item.isEmpty()) {
            return ValidationResult.success();
        }

        String id = item.itemId().toLowerCase(Locale.ROOT);
        if (BANNED_ITEMS.contains(id)) {
            return ValidationResult.fail("Banned item in " + slotName + ": " + id);
        }

        if (item.count() < 1 || item.count() > 64) {
            return ValidationResult.fail("Invalid item stack size in " + slotName + ": " + item.count());
        }

        if (isArmorSlot && item.count() != 1) {
            return ValidationResult.fail("Armor stack size in " + slotName + " must be 1");
        }

        // Check enchantments
        if (item.enchantments() != null) {
            for (Map.Entry<String, Integer> entry : item.enchantments().entrySet()) {
                String enchId = entry.getKey().toLowerCase(Locale.ROOT);
                int level = entry.getValue();

                if (level < 1) {
                    return ValidationResult.fail("Enchantment level < 1 for " + enchId + " in " + slotName);
                }

                Integer maxAllowed = MAX_ENCHANTMENT_LEVELS.get(enchId);
                if (maxAllowed != null && level > maxAllowed) {
                    return ValidationResult.fail("Illegal enchantment level " + level + " for " + enchId + " in " + slotName + " (max: " + maxAllowed + ")");
                }
            }
        }

        return ValidationResult.success();
    }

    public void saveCustomLoadout(UUID playerUuid, Loadout loadout) {
        playerCustomLoadouts.computeIfAbsent(playerUuid, k -> new HashMap<>()).put(loadout.getName(), loadout);
    }

    public Loadout getCustomLoadout(UUID playerUuid, String name) {
        Map<String, Loadout> playerMap = playerCustomLoadouts.get(playerUuid);
        if (playerMap != null) {
            return playerMap.get(name);
        }
        return null;
    }

    public Map<String, Loadout> getPlayerCustomLoadouts(UUID playerUuid) {
        return playerCustomLoadouts.getOrDefault(playerUuid, Collections.emptyMap());
    }

    public void deleteCustomLoadout(UUID playerUuid, String name) {
        Map<String, Loadout> playerMap = playerCustomLoadouts.get(playerUuid);
        if (playerMap != null) {
            playerMap.remove(name);
        }
    }
}
