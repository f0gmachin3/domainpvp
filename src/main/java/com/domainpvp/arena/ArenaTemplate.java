package com.domainpvp.arena;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Representation of an arena architectural blueprint.
 */
public class ArenaTemplate {
    private final String id;
    private final String name;
    private final double boundaryRadius;
    private final double boundaryHeight;
    private final List<ArenaSpawnPoint> spawnPoints;
    private final List<ArenaBlockEntry> blocks;

    public ArenaTemplate(String id, String name, double boundaryRadius, double boundaryHeight,
                         List<ArenaSpawnPoint> spawnPoints, List<ArenaBlockEntry> blocks) {
        this.id = id;
        this.name = name;
        this.boundaryRadius = boundaryRadius;
        this.boundaryHeight = boundaryHeight;
        this.spawnPoints = Collections.unmodifiableList(spawnPoints);
        this.blocks = Collections.unmodifiableList(blocks);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public double getBoundaryRadius() {
        return boundaryRadius;
    }

    public double getBoundaryHeight() {
        return boundaryHeight;
    }

    public List<ArenaSpawnPoint> getSpawnPoints() {
        return spawnPoints;
    }

    public List<ArenaBlockEntry> getBlocks() {
        return blocks;
    }

    /**
     * Programmatic architectural generator for the Roman Colosseum arena.
     */
    public static ArenaTemplate createColosseumTemplate() {
        List<ArenaBlockEntry> list = new ArrayList<>();
        int radius = 16;

        // Layer 0: Foundation (subfloor ring)
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                double dist = Math.sqrt(x * x + z * z);
                if (dist <= radius) {
                    list.add(new ArenaBlockEntry(x, 0, z, "minecraft:stone_bricks", 0));
                }
            }
        }

        // Layer 1: Combat floor (smooth stone with concentric chiseled patterns)
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                double dist = Math.sqrt(x * x + z * z);
                if (dist < radius - 1) {
                    if (Math.abs(dist - 6.5) < 0.6 || Math.abs(dist - 11.5) < 0.6) {
                        list.add(new ArenaBlockEntry(x, 1, z, "minecraft:chiseled_stone_bricks", 1));
                    } else if (dist <= 2.2) {
                        list.add(new ArenaBlockEntry(x, 1, z, "minecraft:gold_block", 1));
                    } else {
                        list.add(new ArenaBlockEntry(x, 1, z, "minecraft:smooth_stone", 1));
                    }
                }
            }
        }

        // Layer 2: Outer perimeter walls and arches (height 2 to 5)
        for (int y = 2; y <= 5; y++) {
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    double dist = Math.sqrt(x * x + z * z);
                    if (dist >= radius - 1 && dist <= radius) {
                        // Pillars every 45 degrees, arched openings between
                        double angle = Math.atan2(z, x);
                        boolean isPillar = Math.abs(Math.sin(angle * 4)) < 0.25;

                        if (isPillar || y == 2 || y == 5) {
                            String block = (y % 2 == 0) ? "minecraft:stone_bricks" : "minecraft:mossy_stone_bricks";
                            list.add(new ArenaBlockEntry(x, y, z, block, 2));
                        } else {
                            list.add(new ArenaBlockEntry(x, y, z, "minecraft:iron_bars", 2));
                        }
                    }
                }
            }
        }

        // Layer 3: Tiered spectator seating
        for (int tier = 0; tier < 2; tier++) {
            int tierY = 2 + tier;
            int r = radius - 2 + tier;
            for (int x = -r; x <= r; x++) {
                for (int z = -r; z <= r; z++) {
                    double dist = Math.sqrt(x * x + z * z);
                    if (Math.abs(dist - r) < 0.6) {
                        list.add(new ArenaBlockEntry(x, tierY, z, "minecraft:stone_brick_stairs", 3));
                    }
                }
            }
        }

        // Layer 4: Torches & boundary lanterns
        for (int i = 0; i < 8; i++) {
            double angle = i * Math.PI / 4.0;
            int lx = (int) (Math.cos(angle) * (radius - 1));
            int lz = (int) (Math.sin(angle) * (radius - 1));
            list.add(new ArenaBlockEntry(lx, 6, lz, "minecraft:lantern", 4));
        }

        // Spawn points
        List<ArenaSpawnPoint> spawns = List.of(
                new ArenaSpawnPoint(10.5, 2.0, 0.5, 90.0f, 0.0f),
                new ArenaSpawnPoint(-9.5, 2.0, 0.5, -90.0f, 0.0f)
        );

        return new ArenaTemplate("domainpvp:colosseum", "Roman Colosseum", 18.0, 14.0, spawns, list);
    }
}

