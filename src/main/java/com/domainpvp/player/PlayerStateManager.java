package com.domainpvp.player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-authoritative manager responsible for capturing, protecting,
 * and restoring pre-arena player states.
 */
public class PlayerStateManager {
    private static final PlayerStateManager INSTANCE = new PlayerStateManager();

    // Map of Player UUID -> Snapshot taken before entering any domain
    private final Map<UUID, PlayerSnapshot> preArenaSnapshots = new ConcurrentHashMap<>();

    private PlayerStateManager() {}

    public static PlayerStateManager getInstance() {
        return INSTANCE;
    }

    /**
     * Stores a player's original snapshot prior to domain entry.
     * Guaranteed never to overwrite an existing snapshot during a rematch session!
     */
    public synchronized boolean recordSnapshot(PlayerSnapshot snapshot) {
        if (snapshot == null) return false;
        // Safeguard: Never overwrite original state during rematches
        if (preArenaSnapshots.containsKey(snapshot.getPlayerUuid())) {
            return false;
        }
        preArenaSnapshots.put(snapshot.getPlayerUuid(), snapshot);
        return true;
    }

    public PlayerSnapshot getSnapshot(UUID playerUuid) {
        return preArenaSnapshots.get(playerUuid);
    }

    public boolean hasSnapshot(UUID playerUuid) {
        return preArenaSnapshots.containsKey(playerUuid);
    }

    /**
     * Consumes and removes the snapshot once a player has safely returned to their original world state.
     */
    public synchronized PlayerSnapshot releaseSnapshot(UUID playerUuid) {
        return preArenaSnapshots.remove(playerUuid);
    }

    public synchronized void clear() {
        preArenaSnapshots.clear();
    }
}

