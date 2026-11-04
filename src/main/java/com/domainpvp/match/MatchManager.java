package com.domainpvp.match;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-authoritative manager responsible for duel matches and coordination.
 */
public class MatchManager {
    private static final MatchManager INSTANCE = new MatchManager();

    private final Map<UUID, MatchSession> matchesById = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> playerToMatchId = new ConcurrentHashMap<>();

    private MatchManager() {}

    public static MatchManager getInstance() {
        return INSTANCE;
    }

    public synchronized MatchSession createMatch(UUID partyId, UUID player1Uuid, UUID player2Uuid) {
        if (playerToMatchId.containsKey(player1Uuid) || playerToMatchId.containsKey(player2Uuid)) {
            return null; // A player is already in an active match
        }

        UUID matchId = UUID.randomUUID();
        MatchSession session = new MatchSession(matchId, partyId, player1Uuid, player2Uuid);
        matchesById.put(matchId, session);
        playerToMatchId.put(player1Uuid, matchId);
        playerToMatchId.put(player2Uuid, matchId);

        return session;
    }

    public MatchSession getMatchByPlayer(UUID playerUuid) {
        UUID matchId = playerToMatchId.get(playerUuid);
        if (matchId == null) {
            return null;
        }
        return matchesById.get(matchId);
    }

    public MatchSession getMatchById(UUID matchId) {
        return matchesById.get(matchId);
    }

    public synchronized void finishMatch(UUID matchId, UUID winnerUuid, UUID loserUuid) {
        MatchSession session = matchesById.get(matchId);
        if (session == null) {
            return;
        }

        session.setFinishTime(Instant.now());
        session.getStats().setWinnerUuid(winnerUuid);
        session.getStats().setLoserUuid(loserUuid);

        session.getStateMachine().transitionTo(MatchState.MATCH_FINISHED);
    }

    public synchronized void removeMatch(UUID matchId) {
        MatchSession session = matchesById.remove(matchId);
        if (session != null) {
            playerToMatchId.remove(session.getPlayer1Uuid());
            playerToMatchId.remove(session.getPlayer2Uuid());
        }
    }

    public synchronized void handlePlayerDisconnect(UUID playerUuid) {
        MatchSession session = getMatchByPlayer(playerUuid);
        if (session != null) {
            UUID opponentUuid = session.getOpponent(playerUuid);
            if (session.getState() == MatchState.FIGHTING) {
                // Forfeit victory to remaining player
                finishMatch(session.getMatchId(), opponentUuid, playerUuid);
            } else {
                session.getStateMachine().transitionTo(MatchState.CLEANUP);
            }
        }
    }

    public Collection<MatchSession> getActiveMatches() {
        return Collections.unmodifiableCollection(matchesById.values());
    }

    public synchronized void clear() {
        matchesById.clear();
        playerToMatchId.clear();
    }
}
