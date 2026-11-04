package com.domainpvp.match;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class MatchStateMachineTest {

    @Test
    public void testInitialState() {
        MatchStateMachine sm = new MatchStateMachine();
        assertEquals(MatchState.IDLE, sm.getCurrentState());
    }

    @Test
    public void testValidProgressionFlow() {
        MatchStateMachine sm = new MatchStateMachine();

        assertTrue(sm.transitionTo(MatchState.PARTY_CREATED));
        assertTrue(sm.transitionTo(MatchState.WAITING_FOR_PLAYER));
        assertTrue(sm.transitionTo(MatchState.LOADOUT_SELECTION));
        assertTrue(sm.transitionTo(MatchState.READY_CHECK));
        assertTrue(sm.transitionTo(MatchState.ARENA_BUILDING));
        assertTrue(sm.transitionTo(MatchState.COUNTDOWN));
        assertTrue(sm.transitionTo(MatchState.FIGHTING));
        assertTrue(sm.transitionTo(MatchState.MATCH_FINISHED));
        assertTrue(sm.transitionTo(MatchState.REMATCH));
        assertTrue(sm.transitionTo(MatchState.COUNTDOWN));
        assertTrue(sm.transitionTo(MatchState.FIGHTING));
        assertTrue(sm.transitionTo(MatchState.MATCH_FINISHED));
        assertTrue(sm.transitionTo(MatchState.CLEANUP));
        assertTrue(sm.transitionTo(MatchState.IDLE));
    }

    @Test
    public void testInvalidTransitionsBlocked() {
        MatchStateMachine sm = new MatchStateMachine();
        // IDLE cannot jump directly to FIGHTING
        assertFalse(sm.transitionTo(MatchState.FIGHTING));
        assertEquals(MatchState.IDLE, sm.getCurrentState());

        // IDLE cannot jump directly to COUNTDOWN
        assertFalse(sm.transitionTo(MatchState.COUNTDOWN));
        assertEquals(MatchState.IDLE, sm.getCurrentState());
    }

    @Test
    public void testEmergencyCleanupAllowedFromAnyState() {
        MatchStateMachine sm = new MatchStateMachine();
        sm.transitionTo(MatchState.PARTY_CREATED);
        sm.transitionTo(MatchState.WAITING_FOR_PLAYER);
        sm.transitionTo(MatchState.LOADOUT_SELECTION);
        sm.transitionTo(MatchState.READY_CHECK);
        sm.transitionTo(MatchState.ARENA_BUILDING);

        // Emergency cleanup during construction
        assertTrue(sm.transitionTo(MatchState.CLEANUP));
        assertEquals(MatchState.CLEANUP, sm.getCurrentState());
    }

    @Test
    public void testStateChangeListener() {
        MatchStateMachine sm = new MatchStateMachine();
        List<String> transitions = new ArrayList<>();

        sm.addListener((oldState, newState) -> transitions.add(oldState + "->" + newState));

        sm.transitionTo(MatchState.PARTY_CREATED);
        sm.transitionTo(MatchState.WAITING_FOR_PLAYER);

        assertEquals(2, transitions.size());
        assertEquals("IDLE->PARTY_CREATED", transitions.get(0));
        assertEquals("PARTY_CREATED->WAITING_FOR_PLAYER", transitions.get(1));
    }
}

