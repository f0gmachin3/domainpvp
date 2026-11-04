package com.domainpvp.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

/**
 * Configuration for PvP Domain mod.
 * Loaded from config/domainpvp.json
 */
public class ModConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static ModConfig INSTANCE = new ModConfig();

    public ArenaConfig arena = new ArenaConfig();
    public MatchConfig match = new MatchConfig();
    public LoadoutConfig loadouts = new LoadoutConfig();
    public EffectConfig effects = new EffectConfig();

    public static class ArenaConfig {
        public int constructionSpeedBlocksPerTick = 25;
        public int maxActiveArenas = 4;
        public String defaultArenaTemplate = "domainpvp:colosseum";
        public String arenaDimension = "minecraft:overworld";
        public int arenaOriginX = 5000;
        public int arenaOriginY = 150;
        public int arenaOriginZ = 5000;
        public int arenaSpacing = 500;
        public double boundaryRadius = 22.0;
        public double boundaryHeight = 18.0;
    }

    public static class MatchConfig {
        public int countdownDurationSeconds = 5;
        public int matchTimeoutSeconds = 600;
        public int rematchVoteTimeoutSeconds = 30;
        public boolean preventThirdPartyInterference = true;
        public boolean disableBlockBreaking = true;
        public boolean disableBlockPlacing = true;
        public boolean disableUnauthorizedCommands = true;
        public boolean freezePlayersDuringCountdown = true;
    }

    public static class LoadoutConfig {
        public boolean allowCustomLoadouts = true;
        public int maxCustomLoadoutsPerPlayer = 5;
        public boolean enforceStrictValidation = true;
        public List<String> allowedPresets = List.of(
                "CLASSIC",
                "SWORD",
                "AXE",
                "ARCHER",
                "UHC",
                "CUSTOM"
        );
    }

    public static class EffectConfig {
        public boolean circularBoundaryParticles = true;
        public boolean constructionParticles = true;
        public boolean countdownSounds = true;
        public boolean domainCollapseAnimation = true;
        public double particleDensity = 1.0;
    }

    public static ModConfig get() {
        return INSTANCE;
    }

    public static void load(File configDir) {
        if (!configDir.exists()) {
            configDir.mkdirs();
        }
        File configFile = new File(configDir, "domainpvp.json");
        if (configFile.exists()) {
            try (FileReader reader = new FileReader(configFile)) {
                INSTANCE = GSON.fromJson(reader, ModConfig.class);
                if (INSTANCE == null) {
                    INSTANCE = new ModConfig();
                }
            } catch (Exception e) {
                System.err.println("[PvP Domain] Failed to load config, using defaults: " + e.getMessage());
                INSTANCE = new ModConfig();
            }
        } else {
            save(configDir);
        }
    }

    public static void save(File configDir) {
        if (!configDir.exists()) {
            configDir.mkdirs();
        }
        File configFile = new File(configDir, "domainpvp.json");
        try (FileWriter writer = new FileWriter(configFile)) {
            GSON.toJson(INSTANCE, writer);
        } catch (IOException e) {
            System.err.println("[PvP Domain] Failed to save default config: " + e.getMessage());
        }
    }
}

