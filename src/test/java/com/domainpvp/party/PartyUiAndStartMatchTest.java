package com.domainpvp.party;

import com.domainpvp.client.ui.ClientUIManager;
import com.domainpvp.network.s2c.PartyUpdatedPayload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the top-right Party UI state synchronization and /start command readiness checks.
 */
public class PartyUiAndStartMatchTest {

    private PartyManager manager;
    private UUID leaderUuid;
    private UUID memberUuid;

    @BeforeEach
    public void setup() {
        manager = PartyManager.getInstance();
        manager.clear();
        leaderUuid = UUID.randomUUID();
        memberUuid = UUID.randomUUID();
    }

    @Test
    public void testPartyUiStateSync() {
        ClientUIManager ui = ClientUIManager.getInstance();
        ui.reset();
        assertNull(ui.getCurrentParty());

        UUID partyId = UUID.randomUUID();
        PartyUpdatedPayload payload = new PartyUpdatedPayload(
                partyId,
                leaderUuid,
                leaderUuid.toString(),
                "Alice",
                false,
                "CLASSIC",
                memberUuid.toString(),
                "Bob",
                false,
                "SWORD"
        );

        ui.updateParty(payload);
        assertNotNull(ui.getCurrentParty());
        assertEquals("Alice", ui.getCurrentParty().member1Name());
        assertEquals("Bob", ui.getCurrentParty().member2Name());
        assertFalse(ui.getCurrentParty().member1Ready());
        assertFalse(ui.getCurrentParty().member2Ready());

        // Update with Alice ready
        PartyUpdatedPayload payload2 = new PartyUpdatedPayload(
                partyId,
                leaderUuid,
                leaderUuid.toString(),
                "Alice",
                true,
                "CLASSIC",
                memberUuid.toString(),
                "Bob",
                false,
                "SWORD"
        );
        ui.updateParty(payload2);
        assertTrue(ui.getCurrentParty().member1Ready());
        assertFalse(ui.getCurrentParty().member2Ready());

        // Update when party disbanded or left
        PartyUpdatedPayload emptyPayload = new PartyUpdatedPayload(
                new UUID(0, 0),
                new UUID(0, 0),
                "",
                "",
                false,
                "",
                "",
                "",
                false,
                ""
        );
        ui.updateParty(emptyPayload);
        assertNull(ui.getCurrentParty(), "Empty party payload should clear current party in UI");
    }

    @Test
    public void testStartReadinessValidation() {
        Party party = manager.createParty(leaderUuid, "LeaderPlayer");
        assertNotNull(party);

        // Scenario 1: Solo party cannot start
        assertFalse(party.areAllMembersReady(), "Party with 1 member cannot start");

        // Scenario 2: Two members, neither ready
        manager.invitePlayer(leaderUuid, memberUuid, "MemberPlayer");
        manager.acceptInvite(memberUuid, "MemberPlayer", party.getId());
        assertEquals(2, party.getMemberCount());
        assertFalse(party.areAllMembersReady(), "Neither player is ready");

        // Scenario 3: Only leader ready
        manager.setPlayerReady(leaderUuid, true);
        assertFalse(party.areAllMembersReady(), "Only leader is ready, member is unready");

        // Scenario 4: Member readies up
        manager.setPlayerReady(memberUuid, true);
        assertTrue(party.areAllMembersReady(), "Both players are ready - /start can proceed!");

        // Scenario 5: Member unreadies
        manager.setPlayerReady(memberUuid, false);
        assertFalse(party.areAllMembersReady(), "Member unreadied - /start must throw all player not ready!");
    }

    @Test
    public void testOnlyLeaderHasPermissionToStart() {
        Party party = manager.createParty(leaderUuid, "LeaderPlayer");
        manager.invitePlayer(leaderUuid, memberUuid, "MemberPlayer");
        manager.acceptInvite(memberUuid, "MemberPlayer", party.getId());

        manager.setPlayerReady(leaderUuid, true);
        manager.setPlayerReady(memberUuid, true);
        assertTrue(party.areAllMembersReady());

        // Check leader identity
        assertEquals(leaderUuid, party.getLeaderUuid());
        assertNotEquals(memberUuid, party.getLeaderUuid());
    }
}

