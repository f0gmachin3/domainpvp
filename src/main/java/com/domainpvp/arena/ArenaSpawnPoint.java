package com.domainpvp.arena;

/**
 * Relative spawn location and orientation within an arena.
 */
public record ArenaSpawnPoint(
        double x,
        double y,
        double z,
        float yaw,
        float pitch
) {
}

