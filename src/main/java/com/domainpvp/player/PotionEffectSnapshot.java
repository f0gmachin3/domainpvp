package com.domainpvp.player;

/**
 * Snapshot of an active status/potion effect.
 */
public record PotionEffectSnapshot(
        String effectId,
        int duration,
        int amplifier,
        boolean ambient,
        boolean visible
) {
}

