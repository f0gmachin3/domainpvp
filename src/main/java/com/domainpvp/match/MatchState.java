package com.domainpvp.match;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Strict 11-state match lifecycle enumeration.
 */
public enum MatchState {
    IDLE,
    PARTY_CREATED,
    WAITING_FOR_PLAYER,
    LOADOUT_SELECTION,
    READY_CHECK,
    ARENA_BUILDING,
    COUNTDOWN,
    FIGHTING,
    MATCH_FINISHED,
    REMATCH,
    CLEANUP;

    private static final Map<MatchState, Set<MatchState>> VALID_TRANSITIONS = new EnumMap<>(MatchState.class);

    static {
        VALID_TRANSITIONS.put(IDLE, EnumSet.of(PARTY_CREATED, LOADOUT_SELECTION, ARENA_BUILDING));
        VALID_TRANSITIONS.put(PARTY_CREATED, EnumSet.of(WAITING_FOR_PLAYER, LOADOUT_SELECTION, ARENA_BUILDING, CLEANUP, IDLE));
        VALID_TRANSITIONS.put(WAITING_FOR_PLAYER, EnumSet.of(LOADOUT_SELECTION, ARENA_BUILDING, PARTY_CREATED, CLEANUP, IDLE));
        VALID_TRANSITIONS.put(LOADOUT_SELECTION, EnumSet.of(READY_CHECK, ARENA_BUILDING, CLEANUP, IDLE));
        VALID_TRANSITIONS.put(READY_CHECK, EnumSet.of(ARENA_BUILDING, LOADOUT_SELECTION, CLEANUP, IDLE));
        VALID_TRANSITIONS.put(ARENA_BUILDING, EnumSet.of(COUNTDOWN, CLEANUP));
        VALID_TRANSITIONS.put(COUNTDOWN, EnumSet.of(FIGHTING, CLEANUP));
        VALID_TRANSITIONS.put(FIGHTING, EnumSet.of(MATCH_FINISHED, CLEANUP));
        VALID_TRANSITIONS.put(MATCH_FINISHED, EnumSet.of(REMATCH, CLEANUP, IDLE));
        VALID_TRANSITIONS.put(REMATCH, EnumSet.of(COUNTDOWN, LOADOUT_SELECTION, CLEANUP));
        VALID_TRANSITIONS.put(CLEANUP, EnumSet.of(IDLE));
    }

    public boolean canTransitionTo(MatchState next) {
        if (next == this) return true;
        // Emergency cleanup is always allowed from any state
        if (next == CLEANUP) return true;
        Set<MatchState> validNext = VALID_TRANSITIONS.get(this);
        return validNext != null && validNext.contains(next);
    }
}
