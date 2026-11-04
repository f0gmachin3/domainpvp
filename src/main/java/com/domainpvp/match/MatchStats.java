package com.domainpvp.match;

import java.util.UUID;

/**
 * Statistics captured during an active arena duel.
 */
public class MatchStats {
    private final UUID matchId;
    private UUID winnerUuid;
    private UUID loserUuid;
    private long durationSeconds;
    private float player1DamageDealt;
    private float player2DamageDealt;
    private int player1Hits;
    private int player2Hits;

    public MatchStats(UUID matchId) {
        this.matchId = matchId;
    }

    public UUID getMatchId() {
        return matchId;
    }

    public UUID getWinnerUuid() {
        return winnerUuid;
    }

    public void setWinnerUuid(UUID winnerUuid) {
        this.winnerUuid = winnerUuid;
    }

    public UUID getLoserUuid() {
        return loserUuid;
    }

    public void setLoserUuid(UUID loserUuid) {
        this.loserUuid = loserUuid;
    }

    public long getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(long durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public float getPlayer1DamageDealt() {
        return player1DamageDealt;
    }

    public void addPlayer1Damage(float dmg) {
        this.player1DamageDealt += dmg;
    }

    public float getPlayer2DamageDealt() {
        return player2DamageDealt;
    }

    public void addPlayer2Damage(float dmg) {
        this.player2DamageDealt += dmg;
    }

    public int getPlayer1Hits() {
        return player1Hits;
    }

    public void incrementPlayer1Hits() {
        this.player1Hits++;
    }

    public int getPlayer2Hits() {
        return player2Hits;
    }

    public void incrementPlayer2Hits() {
        this.player2Hits++;
    }
}

