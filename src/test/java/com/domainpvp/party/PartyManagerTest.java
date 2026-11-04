package com.domainpvp.party;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class PartyManagerTest {

    private PartyManager manager;
    private UUID leaderUuid;
    private UUID opponentUuid;

    @BeforeEach
    public void setup() {
        manager = PartyManager.getInstance();
        manager.clear();
        leaderUuid = UUID.randomUUID();
        opponentUuid = UUID.randomUUID();
    }

    @Test
    public void testCreateParty() {
        Party party = manager.createParty(leaderUuid, "LeaderPlayer");
        assertNotNull(party);
        assertEquals(leaderUuid, party.getLeaderUuid());
        assertEquals(1, party.getMemberCount());
        assertEquals(party, manager.getPartyByPlayer(leaderUuid));
    }

    @Test
    public void testCannotCreateMultipleParties() {
        Party party1 = manager.createParty(leaderUuid, "LeaderPlayer");
        assertNotNull(party1);
        Party party2 = manager.createParty(leaderUuid, "LeaderPlayer");
        assertNull(party2, "Player already in party should not be able to create another");
    }

    @Test
    public void testInviteAndAcceptFlow() {
        Party party = manager.createParty(leaderUuid, "LeaderPlayer");
        assertNotNull(party);

        // Leader invites opponent
        PartyInvite invite = manager.invitePlayer(leaderUuid, opponentUuid, "OpponentPlayer");
        assertNotNull(invite);
        assertEquals(opponentUuid, invite.targetPlayerUuid());

        // Opponent accepts
        boolean accepted = manager.acceptInvite(opponentUuid, "OpponentPlayer", party.getId());
        assertTrue(accepted);
        assertEquals(2, party.getMemberCount());
        assertTrue(party.isFull());
        assertEquals(party, manager.getPartyByPlayer(opponentUuid));
    }

    @Test
    public void testCannotInviteSelf() {
        manager.createParty(leaderUuid, "LeaderPlayer");
        PartyInvite invite = manager.invitePlayer(leaderUuid, leaderUuid, "LeaderPlayer");
        assertNull(invite, "Cannot invite self");
    }

    @Test
    public void testCannotJoinFullParty() {
        Party party = manager.createParty(leaderUuid, "LeaderPlayer");
        manager.invitePlayer(leaderUuid, opponentUuid, "OpponentPlayer");
        manager.acceptInvite(opponentUuid, "OpponentPlayer", party.getId());
        assertTrue(party.isFull());

        UUID thirdPlayer = UUID.randomUUID();
        PartyInvite invite3 = manager.invitePlayer(leaderUuid, thirdPlayer, "ThirdPlayer");
        assertNull(invite3, "Cannot invite to full party");
    }

    @Test
    public void testReadinessCheck() {
        Party party = manager.createParty(leaderUuid, "LeaderPlayer");
        manager.invitePlayer(leaderUuid, opponentUuid, "OpponentPlayer");
        manager.acceptInvite(opponentUuid, "OpponentPlayer", party.getId());

        assertFalse(party.areAllMembersReady());

        manager.setPlayerReady(leaderUuid, true);
        assertFalse(party.areAllMembersReady());

        manager.setPlayerReady(opponentUuid, true);
        assertTrue(party.areAllMembersReady());

        // Test unready
        manager.setPlayerReady(leaderUuid, false);
        assertFalse(party.areAllMembersReady(), "Party should not be ready if one member unreadies");
    }

    @Test
    public void testGetInviteForPlayer() {
        Party party = manager.createParty(leaderUuid, "LeaderPlayer");
        manager.invitePlayer(leaderUuid, opponentUuid, "OpponentPlayer");

        PartyInvite found = manager.getInviteForPlayer(opponentUuid);
        assertNotNull(found);
        assertEquals(party.getId(), found.partyId());
        assertEquals(leaderUuid, found.leaderUuid());
    }

    @Test
    public void testLeaveParty() {
        Party party = manager.createParty(leaderUuid, "LeaderPlayer");
        manager.invitePlayer(leaderUuid, opponentUuid, "OpponentPlayer");
        manager.acceptInvite(opponentUuid, "OpponentPlayer", party.getId());

        manager.leaveParty(opponentUuid);
        assertEquals(1, party.getMemberCount());
        assertFalse(party.isFull());
        assertNull(manager.getPartyByPlayer(opponentUuid));
    }
}

