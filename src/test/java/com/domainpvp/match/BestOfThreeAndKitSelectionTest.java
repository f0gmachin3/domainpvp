package com.domainpvp.match;

import com.domainpvp.arena.ArenaInstance;
import com.domainpvp.arena.ArenaTemplate;
import com.domainpvp.combat.CombatManager;
import com.domainpvp.loadout.Loadout;
import com.domainpvp.loadout.LoadoutManager;
import com.domainpvp.loadout.LoadoutPreset;
import com.domainpvp.player.PlayerSnapshot;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class BestOfThreeAndKitSelectionTest {

    @Test
    public void testAllSixPresetsExistAndValidate() {
        LoadoutManager lm = LoadoutManager.getInstance();

        assertEquals(6, LoadoutPreset.values().length, "There must be exactly 6 official PvP Domain kits");

        for (LoadoutPreset preset : LoadoutPreset.values()) {
            Loadout loadout = lm.getPreset(preset.name());
            assertNotNull(loadout, "Preset must exist: " + preset.name());
            LoadoutManager.ValidationResult res = lm.validate(loadout);
            assertTrue(res.valid(), "Preset " + preset.name() + " must be valid: " + res.message());
        }

        // Test numerical aliases
        assertEquals("SWORD_PVP", lm.getPreset("1").getName());
        assertEquals("AXE_PVP", lm.getPreset("2").getName());
        assertEquals("UHC_PVP", lm.getPreset("3").getName());
        assertEquals("SMP_PVP", lm.getPreset("4").getName());
        assertEquals("MACE_PVP", lm.getPreset("5").getName());
        assertEquals("POT_PVP", lm.getPreset("6").getName());
    }

    @Test
    public void testBestOfThreeMatchProgression() {
        UUID p1 = UUID.randomUUID();
        UUID p2 = UUID.randomUUID();
        MatchSession match = new MatchSession(UUID.randomUUID(), UUID.randomUUID(), p1, p2);

        assertEquals(1, match.getCurrentRound());
        assertEquals(0, match.getPlayer1Score());
        assertEquals(0, match.getPlayer2Score());
        assertFalse(match.isMatchDecided());

        // Round 1 won by p1
        match.recordRoundWin(p1);
        assertEquals(1, match.getPlayer1Score());
        assertEquals(0, match.getPlayer2Score());
        assertFalse(match.isMatchDecided());

        match.nextRound();
        assertEquals(2, match.getCurrentRound());

        // Round 2 won by p2
        match.recordRoundWin(p2);
        assertEquals(1, match.getPlayer1Score());
        assertEquals(1, match.getPlayer2Score());
        assertFalse(match.isMatchDecided());

        match.nextRound();
        assertEquals(3, match.getCurrentRound());

        // Round 3 won by p1 -> 2 - 1 -> Match decided!
        match.recordRoundWin(p1);
        assertEquals(2, match.getPlayer1Score());
        assertEquals(1, match.getPlayer2Score());
        assertTrue(match.isMatchDecided());
        assertEquals(p1, match.getMatchWinner());
    }

    @Test
    public void testKitVotingResolution() {
        UUID p1 = UUID.randomUUID();
        UUID p2 = UUID.randomUUID();
        MatchSession match = new MatchSession(UUID.randomUUID(), UUID.randomUUID(), p1, p2);

        // Fallback default
        assertEquals("SWORD_PVP", match.resolveKit());

        // Single vote
        match.voteKit(p1, "AXE_PVP");
        assertEquals("AXE_PVP", match.resolveKit());

        // Unanimous vote
        match.voteKit(p2, "AXE_PVP");
        assertTrue(match.haveBothVotedKit());
        assertEquals("AXE_PVP", match.resolveKit());

        // Split vote: both voted differently -> resolveKit picks one of them
        match.voteKit(p2, "MACE_PVP");
        String picked = match.resolveKit();
        assertTrue(picked.equals("AXE_PVP") || picked.equals("MACE_PVP"));
    }

    @Test
    public void testArenaBoundaryCeilingAndRadialClamping() {
        ArenaTemplate colosseum = ArenaTemplate.createColosseumTemplate();
        ArenaInstance arena = new ArenaInstance(UUID.randomUUID(), colosseum, "minecraft:overworld", 0, 100, 0);

        // Point inside fighting arena
        CombatManager.ClampedPosition pInside = CombatManager.getInstance().clampToBoundary(arena, 5, 102, 5);
        assertFalse(pInside.outside());

        // Player tries to water-climb or tower over the ceiling
        CombatManager.ClampedPosition pHigh = CombatManager.getInstance().clampToBoundary(arena, 5, 125, 5);
        assertTrue(pHigh.outside());
        assertTrue(pHigh.y() < 125);

        // Player tries to breach the outer perimeter
        CombatManager.ClampedPosition pFar = CombatManager.getInstance().clampToBoundary(arena, 40, 102, 40);
        assertTrue(pFar.outside());
        double dist = Math.sqrt(pFar.x() * pFar.x() + pFar.z() * pFar.z());
        assertTrue(dist <= colosseum.getBoundaryRadius());
    }

    @Test
    public void testPlayerSnapshotItemStorage() {
        UUID uuid = UUID.randomUUID();
        PlayerSnapshot snapshot = new PlayerSnapshot(uuid, "TestPlayer");

        assertNotNull(snapshot.getSavedMain());
        assertNotNull(snapshot.getSavedArmor());
        assertNull(snapshot.getSavedOffhand());
        assertTrue(snapshot.getSavedMain().isEmpty());
    }
}
