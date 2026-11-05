package com.domainpvp.player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Server-authoritative snapshot of a player's complete state captured
 * prior to teleportation into the arena domain.
 */
public class PlayerSnapshot {
    private final UUID playerUuid;
    private final String username;

    // Location
    private String dimensionId = "minecraft:overworld";
    private double x;
    private double y;
    private double z;
    private float yaw;
    private float pitch;

    // Attributes & Status
    private String gameMode = "SURVIVAL";
    private float health = 20.0f;
    private float maxHealth = 20.0f;
    private int foodLevel = 20;
    private float saturation = 5.0f;
    private int experienceLevel = 0;
    private float experienceProgress = 0.0f;
    private int selectedSlot = 0;

    // Inventories (Serialized item NBT strings or compact representations)
    private final List<String> mainInventory = new ArrayList<>();
    private final List<String> armorInventory = new ArrayList<>();
    private String offhandItem = "";

    // Potion effects
    private final List<PotionEffectSnapshot> effects = new ArrayList<>();

    public PlayerSnapshot(UUID playerUuid, String username) {
        this.playerUuid = playerUuid;
        this.username = username;
    }

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public String getUsername() {
        return username;
    }

    public String getDimensionId() {
        return dimensionId;
    }

    public void setDimensionId(String dimensionId) {
        this.dimensionId = dimensionId;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public double getZ() {
        return z;
    }

    public void setZ(double z) {
        this.z = z;
    }

    public float getYaw() {
        return yaw;
    }

    public void setYaw(float yaw) {
        this.yaw = yaw;
    }

    public float getPitch() {
        return pitch;
    }

    public void setPitch(float pitch) {
        this.pitch = pitch;
    }

    public String getGameMode() {
        return gameMode;
    }

    public void setGameMode(String gameMode) {
        this.gameMode = gameMode;
    }

    public float getHealth() {
        return health;
    }

    public void setHealth(float health) {
        this.health = health;
    }

    public float getMaxHealth() {
        return maxHealth;
    }

    public void setMaxHealth(float maxHealth) {
        this.maxHealth = maxHealth;
    }

    public int getFoodLevel() {
        return foodLevel;
    }

    public void setFoodLevel(int foodLevel) {
        this.foodLevel = foodLevel;
    }

    public float getSaturation() {
        return saturation;
    }

    public void setSaturation(float saturation) {
        this.saturation = saturation;
    }

    public int getExperienceLevel() {
        return experienceLevel;
    }

    public void setExperienceLevel(int experienceLevel) {
        this.experienceLevel = experienceLevel;
    }

    public float getExperienceProgress() {
        return experienceProgress;
    }

    public void setExperienceProgress(float experienceProgress) {
        this.experienceProgress = experienceProgress;
    }

    public int getSelectedSlot() {
        return selectedSlot;
    }

    public void setSelectedSlot(int selectedSlot) {
        this.selectedSlot = selectedSlot;
    }

    public List<String> getMainInventory() {
        return mainInventory;
    }

    public List<String> getArmorInventory() {
        return armorInventory;
    }

    public String getOffhandItem() {
        return offhandItem;
    }

    public void setOffhandItem(String offhandItem) {
        this.offhandItem = offhandItem;
    }

    public List<PotionEffectSnapshot> getEffects() {
        return effects;
    }

    // Exact in-memory ItemStack copies for lossless pre-match snapshotting
    private transient final List<net.minecraft.item.ItemStack> savedMain = new ArrayList<>();
    private transient final List<net.minecraft.item.ItemStack> savedArmor = new ArrayList<>();
    private transient net.minecraft.item.ItemStack savedOffhand = null;

    public List<net.minecraft.item.ItemStack> getSavedMain() {
        return savedMain;
    }

    public List<net.minecraft.item.ItemStack> getSavedArmor() {
        return savedArmor;
    }

    public net.minecraft.item.ItemStack getSavedOffhand() {
        return savedOffhand;
    }

    public void setSavedOffhand(net.minecraft.item.ItemStack savedOffhand) {
        this.savedOffhand = savedOffhand;
    }
}

