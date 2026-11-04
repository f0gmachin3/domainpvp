package com.domainpvp.match;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Authoritative state machine governing match progression and valid transitions.
 */
public class MatchStateMachine {

    @FunctionalInterface
    public interface StateChangeListener {
        void onStateChange(MatchState oldState, MatchState newState);
    }

    private MatchState currentState;
    private final List<StateChangeListener> listeners = new CopyOnWriteArrayList<>();

    public MatchStateMachine() {
        this.currentState = MatchState.IDLE;
    }

    public MatchStateMachine(MatchState initialState) {
        this.currentState = initialState;
    }

    public synchronized MatchState getCurrentState() {
        return currentState;
    }

    public synchronized boolean transitionTo(MatchState nextState) {
        if (!currentState.canTransitionTo(nextState)) {
            System.err.println("[PvP Domain] Illegal state transition attempted: " + currentState + " -> " + nextState);
            return false;
        }

        MatchState previous = this.currentState;
        this.currentState = nextState;

        for (StateChangeListener listener : listeners) {
            try {
                listener.onStateChange(previous, nextState);
            } catch (Exception e) {
                System.err.println("[PvP Domain] Error in state change listener: " + e.getMessage());
            }
        }
        return true;
    }

    public synchronized void forceState(MatchState state) {
        MatchState previous = this.currentState;
        this.currentState = state;
        for (StateChangeListener listener : listeners) {
            try {
                listener.onStateChange(previous, state);
            } catch (Exception e) {
                System.err.println("[PvP Domain] Error in state change listener: " + e.getMessage());
            }
        }
    }

    public void addListener(StateChangeListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    public void removeListener(StateChangeListener listener) {
        listeners.remove(listener);
    }
}

