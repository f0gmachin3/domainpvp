package com.domainpvp.match;

import net.minecraft.util.math.BlockPos;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Authoritative session holding state for an individual Best of 3 (Bo3) duel.
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

    // Best of 3 (Bo3) Tracking
    private int currentRound = 1;
    private int player1Score = 0;
    private int player2Score = 0;
    private int roundTransitionTicks = -1; // -1 when round is active, >=0 during delay between rounds

    // Kit Selection & Voting
    private String activeKitName = "SWORD_PVP";
    private final Map<UUID, String> kitVotes = new ConcurrentHashMap<>();
    private int kitVotingTicks = 160; // 8s countdown while manifested

    // Arena cleanup for player-placed blocks (water, lava, cobweb, planks)
    private final List<BlockPos> placedBlocks = Collections.synchronizedList(new ArrayList<>());

    private int countdownSeconds = 3;
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

    // Best of 3 Mechanics
    public int getCurrentRound() {
        return currentRound;
    }

    public int getPlayer1Score() {
        return player1Score;
    }

    public int getPlayer2Score() {
        return player2Score;
    }

    public void recordRoundWin(UUID winnerUuid) {
        if (player1Uuid.equals(winnerUuid)) {
            player1Score++;
        } else if (player2Uuid.equals(winnerUuid)) {
            player2Score++;
        }
    }

    public boolean isMatchDecided() {
        return player1Score >= 2 || player2Score >= 2;
    }

    public UUID getMatchWinner() {
        if (player1Score >= 2) return player1Uuid;
        if (player2Score >= 2) return player2Uuid;
        return null;
    }

    public void nextRound() {
        currentRound++;
    }

    public int getRoundTransitionTicks() {
        return roundTransitionTicks;
    }

    public void setRoundTransitionTicks(int ticks) {
        this.roundTransitionTicks = ticks;
    }

    public int decrementRoundTransitionTicks() {
        return --roundTransitionTicks;
    }

    // Kit Voting
    public void voteKit(UUID player, String kit) {
        if (containsPlayer(player) && kit != null) {
            kitVotes.put(player, kit);
        }
    }

    public String getKitVote(UUID player) {
        return kitVotes.get(player);
    }

    public boolean haveBothVotedKit() {
        return kitVotes.containsKey(player1Uuid) && kitVotes.containsKey(player2Uuid);
    }

    public String resolveKit() {
        String v1 = kitVotes.get(player1Uuid);
        String v2 = kitVotes.get(player2Uuid);

        if (v1 != null && v2 != null) {
            if (v1.equalsIgnoreCase(v2)) {
                return v1;
            } else {
                // If votes differ, randomly pick one of the two voted kits
                return new Random().nextBoolean() ? v1 : v2;
            }
        } else if (v1 != null) {
            return v1;
        } else if (v2 != null) {
            return v2;
        } else {
            return "SWORD_PVP";
        }
    }

    public String getActiveKitName() {
        return activeKitName;
    }

    public void setActiveKitName(String activeKitName) {
        this.activeKitName = activeKitName;
    }

    public int getKitVotingTicks() {
        return kitVotingTicks;
    }

    public void setKitVotingTicks(int kitVotingTicks) {
        this.kitVotingTicks = kitVotingTicks;
    }

    public int decrementKitVotingTicks() {
        return --kitVotingTicks;
    }

    // Player-placed blocks tracking
    public List<BlockPos> getPlacedBlocks() {
        return placedBlocks;
    }

    public void addPlacedBlock(BlockPos pos) {
        if (!placedBlocks.contains(pos)) {
            placedBlocks.add(pos);
        }
    }

    public void clearPlacedBlocks() {
        placedBlocks.clear();
    }

    // Countdown and lifecycle
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
