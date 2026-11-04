package com.domainpvp.party;

import java.time.Instant;
import java.util.UUID;

/**
 * Represents an invitation from a party leader to a prospective opponent.
 */
public record PartyInvite(
        UUID partyId,
        UUID leaderUuid,
        String leaderUsername,
        UUID targetPlayerUuid,
        String targetUsername,
        Instant expirationTime
) {
    public static final long DEFAULT_EXPIRATION_SECONDS = 60;

    public static PartyInvite create(UUID partyId, UUID leaderUuid, String leaderUsername, UUID targetUuid, String targetUsername) {
        return new PartyInvite(
                partyId,
                leaderUuid,
                leaderUsername,
                targetUuid,
                targetUsername,
                Instant.now().plusSeconds(DEFAULT_EXPIRATION_SECONDS)
        );
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expirationTime);
    }
}

