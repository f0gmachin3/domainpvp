package com.domainpvp.party;

import java.util.*;

/**
 * Represents a 2-player PvP Domain party.
 */
public class Party {
    public static final int MAX_MEMBERS = 2;

    private final UUID id;
    private UUID leaderUuid;
    private final Map<UUID, PartyMember> members = new LinkedHashMap<>();
    private final Map<UUID, PartyInvite> pendingInvites = new HashMap<>();

    public Party(UUID id, UUID leaderUuid, String leaderUsername) {
        this.id = id;
        this.leaderUuid = leaderUuid;
        this.members.put(leaderUuid, new PartyMember(leaderUuid, leaderUsername));
    }

    public UUID getId() {
        return id;
    }

    public UUID getLeaderUuid() {
        return leaderUuid;
    }

    public void setLeaderUuid(UUID leaderUuid) {
        if (members.containsKey(leaderUuid)) {
            this.leaderUuid = leaderUuid;
        }
    }

    public Collection<PartyMember> getMembers() {
        return Collections.unmodifiableCollection(members.values());
    }

    public PartyMember getMember(UUID uuid) {
        return members.get(uuid);
    }

    public boolean hasMember(UUID uuid) {
        return members.containsKey(uuid);
    }

    public boolean isFull() {
        return members.size() >= MAX_MEMBERS;
    }

    public boolean addMember(PartyMember member) {
        if (isFull()) {
            return false;
        }
        members.put(member.getUuid(), member);
        pendingInvites.remove(member.getUuid());
        return true;
    }

    public boolean removeMember(UUID uuid) {
        PartyMember removed = members.remove(uuid);
        if (removed != null && uuid.equals(leaderUuid) && !members.isEmpty()) {
            // Reassign leadership to remaining member
            this.leaderUuid = members.keySet().iterator().next();
        }
        return removed != null;
    }

    public int getMemberCount() {
        return members.size();
    }

    public boolean areAllMembersReady() {
        if (members.size() != MAX_MEMBERS) {
            return false;
        }
        for (PartyMember member : members.values()) {
            if (!member.isReady()) {
                return false;
            }
        }
        return true;
    }

    public void addInvite(PartyInvite invite) {
        pendingInvites.put(invite.targetPlayerUuid(), invite);
    }

    public PartyInvite getInvite(UUID targetUuid) {
        PartyInvite invite = pendingInvites.get(targetUuid);
        if (invite != null && invite.isExpired()) {
            pendingInvites.remove(targetUuid);
            return null;
        }
        return invite;
    }

    public void removeInvite(UUID targetUuid) {
        pendingInvites.remove(targetUuid);
    }

    public Map<UUID, PartyInvite> getPendingInvites() {
        // Clean expired
        pendingInvites.values().removeIf(PartyInvite::isExpired);
        return Collections.unmodifiableMap(pendingInvites);
    }

    public void resetReadyStatus() {
        for (PartyMember member : members.values()) {
            member.setReady(false);
        }
    }
}

