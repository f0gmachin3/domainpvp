package com.domainpvp.arena;

/**
 * Single block definition within an arena template with relative offsets.
 */
public record ArenaBlockEntry(
        int dx,
        int dy,
        int dz,
        String blockState,
        int layer
) {
}

