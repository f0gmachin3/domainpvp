package com.domainpvp.loadout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Complete equipment and inventory layout for an arena duel.
 */
public class Loadout {
    private String name;
    private LoadoutItem helmet = LoadoutItem.EMPTY;
    private LoadoutItem chestplate = LoadoutItem.EMPTY;
    private LoadoutItem leggings = LoadoutItem.EMPTY;
    private LoadoutItem boots = LoadoutItem.EMPTY;
    private LoadoutItem offhand = LoadoutItem.EMPTY;
    private final List<LoadoutItem> hotbar = new ArrayList<>(Collections.nCopies(9, LoadoutItem.EMPTY));
    private final List<LoadoutItem> inventory = new ArrayList<>(Collections.nCopies(27, LoadoutItem.EMPTY));

    public Loadout() {
        this.name = "CUSTOM";
    }

    public Loadout(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LoadoutItem getHelmet() {
        return helmet;
    }

    public void setHelmet(LoadoutItem helmet) {
        this.helmet = helmet != null ? helmet : LoadoutItem.EMPTY;
    }

    public LoadoutItem getChestplate() {
        return chestplate;
    }

    public void setChestplate(LoadoutItem chestplate) {
        this.chestplate = chestplate != null ? chestplate : LoadoutItem.EMPTY;
    }

    public LoadoutItem getLeggings() {
        return leggings;
    }

    public void setLeggings(LoadoutItem leggings) {
        this.leggings = leggings != null ? leggings : LoadoutItem.EMPTY;
    }

    public LoadoutItem getBoots() {
        return boots;
    }

    public void setBoots(LoadoutItem boots) {
        this.boots = boots != null ? boots : LoadoutItem.EMPTY;
    }

    public LoadoutItem getOffhand() {
        return offhand;
    }

    public void setOffhand(LoadoutItem offhand) {
        this.offhand = offhand != null ? offhand : LoadoutItem.EMPTY;
    }

    public List<LoadoutItem> getHotbar() {
        return hotbar;
    }

    public void setHotbarSlot(int index, LoadoutItem item) {
        if (index >= 0 && index < 9) {
            hotbar.set(index, item != null ? item : LoadoutItem.EMPTY);
        }
    }

    public List<LoadoutItem> getInventory() {
        return inventory;
    }

    public void setInventorySlot(int index, LoadoutItem item) {
        if (index >= 0 && index < 27) {
            inventory.set(index, item != null ? item : LoadoutItem.EMPTY);
        }
    }
}

