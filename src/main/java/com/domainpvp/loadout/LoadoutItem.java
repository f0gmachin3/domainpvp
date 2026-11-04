package com.domainpvp.loadout;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Validated representation of an item in a PvP Domain loadout.
 */
public record LoadoutItem(
        String itemId,
        int count,
        Map<String, Integer> enchantments
) {
    public static final LoadoutItem EMPTY = new LoadoutItem("minecraft:air", 0, Collections.emptyMap());

    public LoadoutItem(String itemId, int count) {
        this(itemId, count, Collections.emptyMap());
    }

    public boolean isEmpty() {
        return itemId == null || itemId.isEmpty() || itemId.equals("minecraft:air") || count <= 0;
    }

    public static LoadoutItem simple(String itemId, int count) {
        return new LoadoutItem(itemId, count, Collections.emptyMap());
    }

    public static LoadoutItem enchanted(String itemId, int count, String enchId, int level) {
        Map<String, Integer> map = new HashMap<>();
        map.put(enchId, level);
        return new LoadoutItem(itemId, count, map);
    }
}

