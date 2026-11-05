package com.domainpvp.match;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Authoritative session holding state for an individual 1v1 duel.
 */
public class MatchSession {
    private final UUID matchId;
    private final UUID partyId;
    private final UUID player1Uuid;
    private final UUID player2Uuid;
    private UUID arenaInstanceId;

    private final MatchStateMachine stateMachine;
    private final MatchStats stats;
    private final Map<UUID, Boolean> rematchVotes = new ConcurrentHashMap<>();

    private int countdownSeconds = 5;
    private int finishCountdownTicks = 100; // 5 seconds post-match celebration
    private Instant startTime;
    private Instant finishTime;

    public MatchSession(UUID matchId, UUID partyId, UUID player1Uuid, UUID player2Uuid) {
        this.matchId = matchId;
        this.partyId = partyId;
        this.player1Uuid = player1Uuid;
        this.player2Uuid = player2Uuid;
        this.stateMachine = new MatchStateMachine(MatchState.LOADOUT_SELECTION);
        this.stats = new MatchStats(matchId);
    }

    public UUID getMatchId() {
        return matchId;
    }

    public UUID getPartyId() {
        return partyId;
    }

    public UUID getPlayer1Uuid() {
        return player1Uuid;
    }

    public UUID getPlayer2Uuid() {
        return player2Uuid;
    }

    public UUID getArenaInstanceId() {
        return arenaInstanceId;
    }

    public void setArenaInstanceId(UUID arenaInstanceId) {
        this.arenaInstanceId = arenaInstanceId;
    }

    public MatchStateMachine getStateMachine() {
        return stateMachine;
    }

    public MatchState getState() {
        return stateMachine.getCurrentState();
    }

    public MatchStats getStats() {
        return stats;
    }

    public boolean containsPlayer(UUID uuid) {
        return player1Uuid.equals(uuid) || player2Uuid.equals(uuid);
    }

    public UUID getOpponent(UUID uuid) {
        if (player1Uuid.equals(uuid)) return player2Uuid;
        if (player2Uuid.equals(uuid)) return player1Uuid;
        return null;
    }

    public int getCountdownSeconds() {
        return countdownSeconds;
    }

    public void setCountdownSeconds(int countdownSeconds) {
        this.countdownSeconds = countdownSeconds;
    }

    public int decrementCountdown() {
        return --countdownSeconds;
    }

    public int getFinishCountdownTicks() {
        return finishCountdownTicks;
    }

    public void setFinishCountdownTicks(int ticks) {
        this.finishCountdownTicks = ticks;
    }

    public int decrementFinishCountdownTicks() {
        return --finishCountdownTicks;
    }

    public void voteRematch(UUID playerUuid, boolean agree) {
        if (containsPlayer(playerUuid)) {
            rematchVotes.put(playerUuid, agree);
        }
    }

    public boolean hasVotedRematch(UUID playerUuid) {
        return rematchVotes.containsKey(playerUuid);
    }

    public boolean getRematchVote(UUID playerUuid) {
        return Boolean.TRUE.equals(rematchVotes.get(playerUuid));
    }

    public boolean haveBothAgreedRematch() {
        return Boolean.TRUE.equals(rematchVotes.get(player1Uuid)) &&
               Boolean.TRUE.equals(rematchVotes.get(player2Uuid));
    }

    public boolean hasAnyDeclinedRematch() {
        return Boolean.FALSE.equals(rematchVotes.get(player1Uuid)) ||
               Boolean.FALSE.equals(rematchVotes.get(player2Uuid));
    }

    public void clearRematchVotes() {
        rematchVotes.clear();
    }

    public Instant getStartTime() {
        return startTime;
    }

    public void setStartTime(Instant startTime) {
        this.startTime = startTime;
    }

    public Instant getFinishTime() {
        return finishTime;
    }

    public void setFinishTime(Instant finishTime) {
        this.finishTime = finishTime;
        if (startTime != null && finishTime != null) {
            stats.setDurationSeconds(finishTime.getEpochSecond() - startTime.getEpochSecond());
        }
    }
}
