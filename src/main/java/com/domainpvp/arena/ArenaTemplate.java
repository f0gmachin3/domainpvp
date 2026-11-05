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
     * Features authentic Roman tiered spectator seating, grand archways,
     * high fortified walls, and an anti-climb overhanging rim.
     */
    public static ArenaTemplate createColosseumTemplate() {
        List<ArenaBlockEntry> list = new ArrayList<>();
        int outerRadius = 24;
        int floorRadius = 14;

        // Layer 0: Foundation subfloor ring
        for (int x = -outerRadius; x <= outerRadius; x++) {
            for (int z = -outerRadius; z <= outerRadius; z++) {
                double dist = Math.sqrt(x * x + z * z);
                if (dist <= outerRadius) {
                    list.add(new ArenaBlockEntry(x, 0, z, "minecraft:stone_bricks", 0));
                }
            }
        }

        // Layer 1: Combat floor (smooth stone with concentric chiseled patterns and gold medallion)
        for (int x = -floorRadius; x <= floorRadius; x++) {
            for (int z = -floorRadius; z <= floorRadius; z++) {
                double dist = Math.sqrt(x * x + z * z);
                if (dist <= floorRadius) {
                    if (dist <= 2.2) {
                        list.add(new ArenaBlockEntry(x, 1, z, "minecraft:gold_block", 1));
                    } else if (dist <= 3.2) {
                        list.add(new ArenaBlockEntry(x, 1, z, "minecraft:chiseled_stone_bricks", 1));
                    } else if (Math.abs(dist - 7.5) < 0.6 || Math.abs(dist - 12.0) < 0.6) {
                        list.add(new ArenaBlockEntry(x, 1, z, "minecraft:chiseled_stone_bricks", 1));
                    } else {
                        list.add(new ArenaBlockEntry(x, 1, z, "minecraft:smooth_stone", 1));
                    }
                }
            }
        }

        // Layer 2 to 5: Tiered Roman Spectator Seating around the arena
        for (int tier = 0; tier < 4; tier++) {
            int tierY = 2 + tier;
            int r = floorRadius + 1 + (tier * 2);
            for (int x = -r; x <= r; x++) {
                for (int z = -r; z <= r; z++) {
                    double dist = Math.sqrt(x * x + z * z);
                    if (dist >= r - 0.7 && dist <= r + 0.9) {
                        list.add(new ArenaBlockEntry(x, tierY, z, "minecraft:stone_brick_stairs", 2));
                    }
                }
            }
        }

        // Upper Promenade Walkway (Y = 6)
        for (int x = -outerRadius; x <= outerRadius; x++) {
            for (int z = -outerRadius; z <= outerRadius; z++) {
                double dist = Math.sqrt(x * x + z * z);
                if (dist >= 21.5 && dist <= 23.0) {
                    list.add(new ArenaBlockEntry(x, 6, z, "minecraft:smooth_stone", 2));
                }
            }
        }

        // Layer 2 to 11: Outer Colosseum Fortress Wall & 16 Grand Pillars
        for (int y = 2; y <= 11; y++) {
            for (int x = -outerRadius; x <= outerRadius; x++) {
                for (int z = -outerRadius; z <= outerRadius; z++) {
                    double dist = Math.sqrt(x * x + z * z);
                    if (dist >= outerRadius - 1.2 && dist <= outerRadius) {
                        double angle = Math.atan2(z, x);
                        boolean isPillar = Math.abs(Math.sin(angle * 8)) < 0.22;

                        if (isPillar || y <= 6 || y >= 10) {
                            String block = (y % 3 == 0) ? "minecraft:chiseled_stone_bricks" : "minecraft:stone_bricks";
                            list.add(new ArenaBlockEntry(x, y, z, block, 3));
                        } else {
                            list.add(new ArenaBlockEntry(x, y, z, "minecraft:iron_bars", 3));
                        }
                    }
                }
            }
        }

        // Layer 12: Inward Overhanging Anti-Climb Cornice Rim
        // Physically overhangs into the arena to block any water swimming or tower jumping over the wall!
        for (int x = -outerRadius; x <= outerRadius; x++) {
            for (int z = -outerRadius; z <= outerRadius; z++) {
                double dist = Math.sqrt(x * x + z * z);
                if (dist >= outerRadius - 2.5 && dist <= outerRadius) {
                    list.add(new ArenaBlockEntry(x, 12, z, "minecraft:stone_brick_slab", 4));
                }
            }
        }

        // Perimeter Lanterns along the Colosseum parapet
        for (int i = 0; i < 16; i++) {
            double angle = i * Math.PI / 8.0;
            int lx = (int) Math.round(Math.cos(angle) * (outerRadius - 1));
            int lz = (int) Math.round(Math.sin(angle) * (outerRadius - 1));
            list.add(new ArenaBlockEntry(lx, 12, lz, "minecraft:lantern", 4));
        }

        // Dueling Spawn Points facing center
        List<ArenaSpawnPoint> spawns = List.of(
                new ArenaSpawnPoint(9.5, 2.0, 0.5, 90.0f, 0.0f),
                new ArenaSpawnPoint(-8.5, 2.0, 0.5, -90.0f, 0.0f)
        );

        return new ArenaTemplate("domainpvp:colosseum", "Roman Colosseum", 25.0, 13.0, spawns, list);
    }
}

