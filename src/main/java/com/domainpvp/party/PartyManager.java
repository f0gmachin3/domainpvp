package com.domainpvp.party;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-authoritative manager for 2-player PvP Domain parties.
 */
public class PartyManager {
    private static final PartyManager INSTANCE = new PartyManager();

    private final Map<UUID, Party> partiesById = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> playerToPartyId = new ConcurrentHashMap<>();

    private PartyManager() {}

    public static PartyManager getInstance() {
        return INSTANCE;
    }

    public synchronized Party createParty(UUID leaderUuid, String leaderUsername) {
        if (playerToPartyId.containsKey(leaderUuid)) {
            return null; // Already in a party
        }
        UUID partyId = UUID.randomUUID();
        Party party = new Party(partyId, leaderUuid, leaderUsername);
        partiesById.put(partyId, party);
        playerToPartyId.put(leaderUuid, partyId);
        return party;
    }

    public synchronized PartyInvite invitePlayer(UUID leaderUuid, UUID targetUuid, String targetUsername) {
        if (leaderUuid.equals(targetUuid)) {
            return null; // Cannot invite self
        }
        UUID partyId = playerToPartyId.get(leaderUuid);
        if (partyId == null) {
            return null;
        }
        Party party = partiesById.get(partyId);
        if (party == null || !party.getLeaderUuid().equals(leaderUuid)) {
            return null; // Must be party leader
        }
        if (party.isFull()) {
            return null; // Party full
        }
        if (playerToPartyId.containsKey(targetUuid)) {
            return null; // Target already in a party
        }
        PartyMember leader = party.getMember(leaderUuid);
        String leaderName = leader != null ? leader.getUsername() : "Leader";
        PartyInvite invite = PartyInvite.create(partyId, leaderUuid, leaderName, targetUuid, targetUsername);
        party.addInvite(invite);
        return invite;
    }

    public synchronized PartyInvite getInviteForPlayer(UUID targetUuid) {
        for (Party party : partiesById.values()) {
            PartyInvite invite = party.getInvite(targetUuid);
            if (invite != null && !invite.isExpired()) {
                return invite;
            }
        }
        return null;
    }

    public synchronized boolean acceptInvite(UUID targetUuid, String targetUsername, UUID partyId) {
        if (playerToPartyId.containsKey(targetUuid)) {
            return false; // Already in a party
        }
        Party party = partiesById.get(partyId);
        if (party == null || party.isFull()) {
            return false;
        }
        PartyInvite invite = party.getInvite(targetUuid);
        if (invite == null || invite.isExpired()) {
            return false;
        }
        PartyMember newMember = new PartyMember(targetUuid, targetUsername);
        if (party.addMember(newMember)) {
            playerToPartyId.put(targetUuid, partyId);
            return true;
        }
        return false;
    }

    public synchronized boolean declineInvite(UUID targetUuid, UUID partyId) {
        Party party = partiesById.get(partyId);
        if (party == null) {
            return false;
        }
        party.removeInvite(targetUuid);
        return true;
    }

    public synchronized Party leaveParty(UUID playerUuid) {
        UUID partyId = playerToPartyId.remove(playerUuid);
        if (partyId == null) {
            return null;
        }
        Party party = partiesById.get(partyId);
        if (party == null) {
            return null;
        }
        party.removeMember(playerUuid);
        if (party.getMemberCount() == 0) {
            partiesById.remove(partyId);
            return null;
        }
        return party;
    }

    public synchronized void disbandParty(UUID partyId) {
        Party party = partiesById.remove(partyId);
        if (party != null) {
            for (PartyMember member : party.getMembers()) {
                playerToPartyId.remove(member.getUuid());
            }
        }
    }

    public Party getPartyByPlayer(UUID playerUuid) {
        UUID partyId = playerToPartyId.get(playerUuid);
        if (partyId == null) {
            return null;
        }
        return partiesById.get(partyId);
    }

    public Party getPartyById(UUID partyId) {
        return partiesById.get(partyId);
    }

    public synchronized boolean setPlayerReady(UUID playerUuid, boolean ready) {
        Party party = getPartyByPlayer(playerUuid);
        if (party == null) {
            return false;
        }
        PartyMember member = party.getMember(playerUuid);
        if (member == null) {
            return false;
        }
        member.setReady(ready);
        return true;
    }

    public synchronized boolean setPlayerLoadout(UUID playerUuid, String loadoutName) {
        Party party = getPartyByPlayer(playerUuid);
        if (party == null) {
            return false;
        }
        PartyMember member = party.getMember(playerUuid);
        if (member == null) {
            return false;
        }
        member.setSelectedLoadoutName(loadoutName);
        return true;
    }

    public synchronized void clear() {
        partiesById.clear();
        playerToPartyId.clear();
    }
}

