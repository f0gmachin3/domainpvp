package com.domainpvp.test;

import com.domainpvp.arena.ArenaBlockEntry;
import com.domainpvp.arena.ArenaInstance;
import com.domainpvp.arena.ArenaSpawnPoint;
import com.domainpvp.arena.ArenaTemplate;
import com.domainpvp.combat.CombatManager;
import com.domainpvp.loadout.Loadout;
import com.domainpvp.loadout.LoadoutItem;
import com.domainpvp.loadout.LoadoutManager;
import com.domainpvp.loadout.LoadoutPreset;
import com.domainpvp.match.MatchManager;
import com.domainpvp.match.MatchSession;
import com.domainpvp.match.MatchState;
import com.domainpvp.match.MatchStateMachine;
import com.domainpvp.party.Party;
import com.domainpvp.party.PartyInvite;
import com.domainpvp.party.PartyManager;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Self-contained diagnostic test runner to verify core mod domain logic.
 */
public class StandaloneTestRunner {
    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("Running PvP Domain Diagnostic Verification Suite");
        System.out.println("==================================================");

        testPartyManager();
        testMatchStateMachine();
        testLoadoutValidation();
        testArenaArchitecture();
        testCombatIsolationAndRules();

        System.out.println("--------------------------------------------------");
        System.out.printf("Results: %d PASSED, %d FAILED%n", passed, failed);
        System.out.println("==================================================");

        if (failed > 0) {
            System.exit(1);
        }
    }

    private static void assertTrue(String testName, boolean condition) {
        if (condition) {
            System.out.println("  [PASS] " + testName);
            passed++;
        } else {
            System.err.println("  [FAIL] " + testName);
            failed++;
        }
    }

    private static void testPartyManager() {
        System.out.println("\n[PartyManager Tests]");
        PartyManager pm = PartyManager.getInstance();
        pm.clear();

        UUID p1 = UUID.randomUUID();
        UUID p2 = UUID.randomUUID();
        UUID p3 = UUID.randomUUID();

        Party party = pm.createParty(p1, "PlayerOne");
        assertTrue("Create party establishes leader", party != null && party.getLeaderUuid().equals(p1));
        assertTrue("Player cannot create second party", pm.createParty(p1, "PlayerOne") == null);

        assertTrue("Player cannot invite self", pm.invitePlayer(p1, p1, "PlayerOne") == null);

        PartyInvite invite = pm.invitePlayer(p1, p2, "PlayerTwo");
        assertTrue("Leader can invite target player", invite != null && invite.targetPlayerUuid().equals(p2));

        boolean accepted = pm.acceptInvite(p2, "PlayerTwo", party.getId());
        assertTrue("Target can accept invite", accepted);
        assertTrue("Party reaches full capacity (2 members)", party.isFull() && party.getMemberCount() == 2);

        assertTrue("Third player cannot be invited to full party", pm.invitePlayer(p1, p3, "PlayerThree") == null);

        assertTrue("Both players not ready initially", !party.areAllMembersReady());
        pm.setPlayerReady(p1, true);
        assertTrue("P1 ready, still not all ready", !party.areAllMembersReady());
        pm.setPlayerReady(p2, true);
        assertTrue("Both players ready", party.areAllMembersReady());

        pm.leaveParty(p2);
        assertTrue("After p2 leaves, member count is 1", party.getMemberCount() == 1 && !party.isFull());
    }

    private static void testMatchStateMachine() {
        System.out.println("\n[MatchStateMachine Tests]");
        MatchStateMachine sm = new MatchStateMachine();

        assertTrue("Initial state is IDLE", sm.getCurrentState() == MatchState.IDLE);
        assertTrue("Transition to PARTY_CREATED", sm.transitionTo(MatchState.PARTY_CREATED));
        assertTrue("Transition to WAITING_FOR_PLAYER", sm.transitionTo(MatchState.WAITING_FOR_PLAYER));
        assertTrue("Transition to LOADOUT_SELECTION", sm.transitionTo(MatchState.LOADOUT_SELECTION));
        assertTrue("Transition to READY_CHECK", sm.transitionTo(MatchState.READY_CHECK));
        assertTrue("Transition to ARENA_BUILDING", sm.transitionTo(MatchState.ARENA_BUILDING));
        assertTrue("Transition to COUNTDOWN", sm.transitionTo(MatchState.COUNTDOWN));
        assertTrue("Transition to FIGHTING", sm.transitionTo(MatchState.FIGHTING));
        assertTrue("Transition to MATCH_FINISHED", sm.transitionTo(MatchState.MATCH_FINISHED));
        assertTrue("Transition to REMATCH", sm.transitionTo(MatchState.REMATCH));
        assertTrue("Rematch loops back to COUNTDOWN", sm.transitionTo(MatchState.COUNTDOWN));
        assertTrue("Countdown progresses to FIGHTING", sm.transitionTo(MatchState.FIGHTING));
        assertTrue("Fighting finishes", sm.transitionTo(MatchState.MATCH_FINISHED));
        assertTrue("Transition to CLEANUP", sm.transitionTo(MatchState.CLEANUP));
        assertTrue("Teardown returns to IDLE", sm.transitionTo(MatchState.IDLE));

        // Invalid jump test
        assertTrue("Direct jump from IDLE to FIGHTING blocked", !sm.transitionTo(MatchState.FIGHTING));

        // Emergency cleanup test
        sm.transitionTo(MatchState.PARTY_CREATED);
        assertTrue("Emergency CLEANUP allowed from arbitrary state", sm.transitionTo(MatchState.CLEANUP));
    }

    private static void testLoadoutValidation() {
        System.out.println("\n[LoadoutValidation Tests]");
        LoadoutManager lm = LoadoutManager.getInstance();

        for (LoadoutPreset preset : LoadoutPreset.values()) {
            Loadout loadout = lm.getPreset(preset.name());
            LoadoutManager.ValidationResult res = lm.validate(loadout);
            assertTrue("Default preset valid: " + preset.name(), res.valid());
        }

        // Banned item test
        Loadout hacked = LoadoutPreset.createDefault(LoadoutPreset.CLASSIC);
        hacked.setHotbarSlot(0, LoadoutItem.simple("minecraft:bedrock", 1));
        LoadoutManager.ValidationResult banRes = lm.validate(hacked);
        assertTrue("Banned item (bedrock) rejected", !banRes.valid() && banRes.message().contains("Banned item"));

        // Illegal enchantment test
        Loadout overEnchanted = LoadoutPreset.createDefault(LoadoutPreset.CLASSIC);
        overEnchanted.setHotbarSlot(0, LoadoutItem.enchanted("minecraft:diamond_sword", 1, "minecraft:sharpness", 255));
        LoadoutManager.ValidationResult enchRes = lm.validate(overEnchanted);
        assertTrue("Illegal sharpness 255 rejected", !enchRes.valid() && enchRes.message().contains("Illegal enchantment level"));

        // Armor stack test
        Loadout stackedArmor = LoadoutPreset.createDefault(LoadoutPreset.CLASSIC);
        stackedArmor.setHelmet(LoadoutItem.simple("minecraft:diamond_helmet", 16));
        LoadoutManager.ValidationResult armorRes = lm.validate(stackedArmor);
        assertTrue("Armor stack > 1 rejected", !armorRes.valid() && armorRes.message().contains("Armor stack size"));
    }

    private static void testArenaArchitecture() {
        System.out.println("\n[ArenaArchitecture Tests]");
        ArenaTemplate colosseum = ArenaTemplate.createColosseumTemplate();
        assertTrue("Colosseum template generated", colosseum != null);
        assertTrue("Colosseum has substantial block entries", colosseum.getBlocks().size() > 500);
        assertTrue("Opposing spawn positions present", colosseum.getSpawnPoints().size() == 2);

        ArenaInstance instance = new ArenaInstance(
                UUID.randomUUID(),
                colosseum,
                "minecraft:overworld",
                5000, 150, 5000
        );

        ArenaSpawnPoint sp1 = instance.getSpawn1World();
        ArenaSpawnPoint sp2 = instance.getSpawn2World();
        assertTrue("Spawn 1 is inside boundary", instance.isInsideBoundary(sp1.x(), sp1.y(), sp1.z()));
        assertTrue("Spawn 2 is inside boundary", instance.isInsideBoundary(sp2.x(), sp2.y(), sp2.z()));
        assertTrue("Far exterior coordinate rejected", !instance.isInsideBoundary(6000, 150, 6000));
    }

    private static void testCombatIsolationAndRules() {
        System.out.println("\n[CombatIsolation Tests]");
        MatchManager mm = MatchManager.getInstance();
        mm.clear();
        CombatManager cm = CombatManager.getInstance();

        UUID p1 = UUID.randomUUID();
        UUID p2 = UUID.randomUUID();
        UUID outsider = UUID.randomUUID();

        MatchSession session = mm.createMatch(UUID.randomUUID(), p1, p2);

        // Before fighting (in LOADOUT_SELECTION), no damage allowed
        assertTrue("Damage denied before FIGHTING state", !cm.canInflictDamage(p1, p2, 5.0f));

        // Advance to FIGHTING
        session.getStateMachine().transitionTo(MatchState.READY_CHECK);
        session.getStateMachine().transitionTo(MatchState.ARENA_BUILDING);
        session.getStateMachine().transitionTo(MatchState.COUNTDOWN);
        session.getStateMachine().transitionTo(MatchState.FIGHTING);

        // Opponents can damage each other
        assertTrue("Duelists can damage each other during FIGHTING", cm.canInflictDamage(p1, p2, 7.0f));

        // Third-party outsider cannot damage duelist
        assertTrue("Outsider cannot damage duelist", !cm.canInflictDamage(outsider, p1, 10.0f));

        // Duelist cannot break blocks
        assertTrue("Duelist cannot break blocks in arena", !cm.canBreakBlock(p1));
        assertTrue("Duelist cannot place blocks in arena", !cm.canPlaceBlock(p1));
    }
}

