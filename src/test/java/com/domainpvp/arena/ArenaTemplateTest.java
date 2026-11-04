package com.domainpvp.arena;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class ArenaTemplateTest {

    @Test
    public void testColosseumTemplateGeneration() {
        ArenaTemplate colosseum = ArenaTemplate.createColosseumTemplate();
        assertNotNull(colosseum);
        assertEquals("domainpvp:colosseum", colosseum.getId());
        assertTrue(colosseum.getBlocks().size() > 500, "Colosseum should have substantial block count");
        assertEquals(2, colosseum.getSpawnPoints().size(), "Colosseum must define 2 opposing spawn points");
        assertTrue(colosseum.getBoundaryRadius() >= 18.0);
    }

    @Test
    public void testArenaBoundaryCalculation() {
        ArenaTemplate colosseum = ArenaTemplate.createColosseumTemplate();
        ArenaInstance instance = new ArenaInstance(
                UUID.randomUUID(),
                colosseum,
                "minecraft:overworld",
                5000, 150, 5000
        );

        // Center should be inside
        assertTrue(instance.isInsideBoundary(5000.5, 151, 5000.5));

        // Spawns should be inside
        ArenaSpawnPoint sp1 = instance.getSpawn1World();
        ArenaSpawnPoint sp2 = instance.getSpawn2World();
        assertTrue(instance.isInsideBoundary(sp1.x(), sp1.y(), sp1.z()));
        assertTrue(instance.isInsideBoundary(sp2.x(), sp2.y(), sp2.z()));

        // Far coordinates should be outside
        assertFalse(instance.isInsideBoundary(5100, 151, 5000));
        assertFalse(instance.isInsideBoundary(5000, 200, 5000));
    }
}

