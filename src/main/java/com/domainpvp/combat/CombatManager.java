package com.domainpvp.combat;

import com.domainpvp.arena.ArenaInstance;
import com.domainpvp.match.MatchManager;
import com.domainpvp.match.MatchSession;
import com.domainpvp.match.MatchState;

import java.util.UUID;

/**
 * Server-authoritative manager enforcing combat isolation, boundary confinement,
 * and rule constraints for domain duels.
 */
public class CombatManager {
    private static final CombatManager INSTANCE = new CombatManager();

    private CombatManager() {}

    public static CombatManager getInstance() {
        return INSTANCE;
    }

    /**
     * Determines whether one player can inflict damage upon another entity.
     */
    public boolean canInflictDamage(UUID attackerUuid, UUID victimUuid, float damageAmount) {
        MatchManager mm = MatchManager.getInstance();
        MatchSession victimMatch = mm.getMatchByPlayer(victimUuid);
        MatchSession attackerMatch = attackerUuid != null ? mm.getMatchByPlayer(attackerUuid) : null;

        // If victim is not in any match, standard rules apply unless attacker is in a match
        if (victimMatch == null) {
            return attackerMatch == null;
        }

        // Both players are in the same match: combat must be active
        if (victimMatch.getState() != MatchState.FIGHTING) {
            return false; // Pre-fight or post-fight immunity
        }

        // Environmental or fall damage (no attacker) is allowed during active fight
        if (attackerUuid == null) {
            return true;
        }

        // If victim is in a match, attacker MUST be their designated opponent
        if (attackerMatch == null || !victimMatch.getMatchId().equals(attackerMatch.getMatchId())) {
            return false; // Third-party interference blocked
        }

        // Record combat statistics
        if (attackerUuid.equals(victimMatch.getPlayer1Uuid())) {
            victimMatch.getStats().addPlayer1Damage(damageAmount);
            victimMatch.getStats().incrementPlayer1Hits();
        } else {
            victimMatch.getStats().addPlayer2Damage(damageAmount);
            victimMatch.getStats().incrementPlayer2Hits();
        }

        return true;
    }

    /**
     * Determines whether a player is authorized to break blocks.
     */
    public boolean canBreakBlock(UUID playerUuid) {
        MatchSession match = MatchManager.getInstance().getMatchByPlayer(playerUuid);
        // Participants are forbidden from modifying arena blocks
        return match == null;
    }

    /**
     * Determines whether a player is authorized to place blocks or fluids.
     * During duels, placement is allowed inside the combat zone (for cobwebs, planks, water)
     * but strictly disallowed on outer upper walls to prevent escape.
     */
    public boolean canPlaceBlock(UUID playerUuid) {
        MatchSession match = MatchManager.getInstance().getMatchByPlayer(playerUuid);
        if (match == null) return true;
        return match.getState() == MatchState.FIGHTING;
    }

    public boolean canPlaceBlockAt(UUID playerUuid, ArenaInstance arena, int x, int y, int z) {
        MatchSession match = MatchManager.getInstance().getMatchByPlayer(playerUuid);
        if (match == null) return true;
        if (match.getState() != MatchState.FIGHTING) return false;
        if (arena == null) return false;

        // Disallow placing blocks or fluids near the top rim or outside the arena
        double dx = x - arena.getOriginX();
        double dz = z - arena.getOriginZ();
        double dist = Math.sqrt(dx * dx + dz * dz);
        if (dist > arena.getTemplate().getBoundaryRadius() - 2.0) return false;
        if (y >= arena.getOriginY() + 8) return false;

        return true;
    }

    /**
     * Handles death of a player in a duel, automatically assigning victory to the opponent.
     */
    public void onPlayerDeath(UUID victimUuid) {
        MatchManager mm = MatchManager.getInstance();
        MatchSession match = mm.getMatchByPlayer(victimUuid);
        if (match != null && match.getState() == MatchState.FIGHTING) {
            UUID winnerUuid = match.getOpponent(victimUuid);
            mm.finishMatch(match.getMatchId(), winnerUuid, victimUuid);
        }
    }

    /**
     * Calculates repositioning vector if a player tries to breach arena perimeter or wall ceiling.
     */
    public record ClampedPosition(boolean outside, double x, double y, double z) {}

    public ClampedPosition clampToBoundary(ArenaInstance arena, double x, double y, double z) {
        if (arena == null) {
            return new ClampedPosition(false, x, y, z);
        }

        double originCenterX = arena.getOriginX() + 0.5;
        double originCenterZ = arena.getOriginZ() + 0.5;
        double dx = x - originCenterX;
        double dz = z - originCenterZ;
        double dist = Math.sqrt(dx * dx + dz * dz);
        double maxRadius = arena.getTemplate().getBoundaryRadius() - 1.0;
        double maxY = arena.getOriginY() + arena.getTemplate().getBoundaryHeight() - 1.0;

        boolean outside = false;
        double clampedX = x;
        double clampedZ = z;
        double clampedY = y;

        // Radial perimeter clamping
        if (dist > maxRadius && dist > 0.001) {
            clampedX = originCenterX + (dx / dist) * maxRadius;
            clampedZ = originCenterZ + (dz / dist) * maxRadius;
            outside = true;
        }

        // Vertical height ceiling clamping (prevents water-climbing, tower-jumping, pearl breaches)
        if (y > maxY) {
            clampedY = maxY - 1.5;
            outside = true;
        }

        return new ClampedPosition(outside, clampedX, clampedY, clampedZ);
    }
}

